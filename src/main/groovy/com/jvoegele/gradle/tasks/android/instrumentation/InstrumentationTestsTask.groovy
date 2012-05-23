/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jvoegele.gradle.tasks.android.instrumentation

import com.jvoegele.gradle.plugins.android.AndroidPlugin;
import com.jvoegele.gradle.tasks.android.AdbExec;
import com.jvoegele.gradle.tasks.android.exceptions.InstrumentationTestsFailedException;
import com.jvoegele.gradle.tasks.android.exceptions.AdbErrorException;

/**
 * Runs instrumentation tests on a connected device or emulator.
 *
 * @author Matthias Kaeppler
 * @author Ladislav Thon
 */
class InstrumentationTestsTask extends AdbExec {
  def testPackage
  def testRunnersConfig
  def testReportsSourcePath
  def testReportsTargetPath
  def defaultAdbArgs

  def InstrumentationTestsTask() {
    logger.info("Running instrumentation tests...")

    this.testPackage = ant['manifest.package']
    this.defaultAdbArgs = super.getArgs()
    this.testRunnersConfig = new TestRunnersConfig(project, testPackage)

    def testedPackage = ant['tested.manifest.package']
    if (testedPackage) { // this is only set for instrumentation projects
      this.testReportsSourcePath = "/data/data/$testedPackage/files"
      this.testReportsTargetPath = project.file('build/test-results').toString()
    }

    onlyIf {
      boolean isTestingOtherPackage = testedPackage || false
      if (!isTestingOtherPackage) {
        logger.warn("!! Skipping $AndroidPlugin.ANDROID_INSTRUMENTATION_TESTS_TASK_NAME task "
          + "for project $project.name since no target package was specified in the manifest")
      }
      isTestingOtherPackage
    }
  }

  /**
   * Used to configure test runners using a closure, e.g.:
   * <code>
   * androidInstrumentationTests {
   *   runners {
   *     run testpackage: "com.my.package", with: "com.my.TestRunner"
   *   }
   * }
   * </code>
   * @param config
   * @return
   */
  def runners(Closure config) {
    config.delegate = testRunnersConfig
    config()
  }

  @Override
  def exec() {
    if (testRunnersConfig.performDefaultRun()) {
      def runConfig = testRunnersConfig.defaultConfig
      if (!runConfig) {
        // this will happen if there was no runners block provided at all
        runConfig = testRunnersConfig.createRunConfig()
      }
      performTestRun(runConfig)
    } else {
      // execute package specific runners
      testRunnersConfig.packageRunners.each {
        def packageName = it.key
        def runConfig   = it.value
        performTestRun(runConfig, ["-e", "package", "$packageName"])
      }
      // execute annotation specific runners
      testRunnersConfig.annotationRunners.each {
        def annotation = it.key
        def runConfig = it.value
        performTestRun(runConfig, ["-e", "annotation", "$annotation"])
      }
    }
  }

  def performTestRun(def runConfig, def filter = null) {
    // run the tests
    setArgs(defaultAdbArgs)
    args "shell", "am", "instrument"
    args runConfig.options // it's a List!
    if (filter) {
      args filter          // this too
    }
    args "$testPackage/$runConfig.runner"

    InstrumentationTestsFailedException testFailure = null
    try {
      super.exec()
    } catch (InstrumentationTestsFailedException itfex) {
      testFailure = itfex
    } finally {
      // publish test results, if any (requires a runner that supports this)
      def reportFile = "${testReportsSourcePath}/${runConfig.name}.xml"
      try {
        publishTestReport(runConfig, reportFile)
      } catch (Exception e) {
        logger.warn "!! Failed to publish test reports"
      }
    }

    logger.info "Test run complete."

    if (testFailure) {
      throw testFailure
    }
  }

  private void publishTestReport(def runConfig, def reportFile) {
    logger.info("Publishing test results from $reportFile to $testReportsTargetPath")

    def pullTestReport = new AdbExec()
    pullTestReport.args "pull", "$reportFile", "$testReportsTargetPath"
    pullTestReport.exec()
  }

  @Override
  def checkForErrors(stream) {
    def reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()))
    if (stream == stdout) {
      detectBuildFailures(reader)
    } else { // stderr
      detectAdbErrors(reader);
    }
  }

  private void detectBuildFailures(def reader) {
    def success = false
    // ADB currently fails with errors on stdout, so we literally have to check
    // for 'OK' to decide whether test failed or not
    reader.eachLine {
      if (it.matches("^OK \\(([0-9]+ test[s]?){1}\\)")) {
        success = true
        return
      }
    }

    if (!success) {
      throw new InstrumentationTestsFailedException();
    }
  }

  // at the moment, ADB errors are actually reported via stdout instead of stderr.
  // this method may become useful though once Google fixes that.
  private void detectAdbErrors(def reader) {
    if (reader.getText().trim()) {
      // at the moment, we treat any non-empty text on stderr as an error
      throw new AdbErrorException("There was an error while trying to run the instrumentation tests")
    }
  }
}
