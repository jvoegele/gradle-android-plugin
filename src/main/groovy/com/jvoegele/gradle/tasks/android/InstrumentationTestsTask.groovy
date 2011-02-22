package com.jvoegele.gradle.tasks.android

import com.jvoegele.gradle.tasks.android.exceptions.InstrumentationTestsFailedException;
import com.jvoegele.gradle.tasks.android.exceptions.AdbErrorException;

class InstrumentationTestsTask extends AdbExec {

  /**
   * Configures the test runners used to run the instrumentation tests.
   *  
   * @author Matthias Kaeppler
   */
  private class TestRunnerConfig {
    
    static final String PACKAGE    = "testpackage"
    static final String ANNOTATION = "annotation"
    static final String RUNNER     = "with"
    static final String NAME       = "name"
    static final String OPTIONS    = "options"
    
    def defaultConfig
    def packageRunners    = [:]
    def annotationRunners = [:]
    
    def run(args = [:]) {
      def testRunner  = args[(RUNNER)] ?: getProject().convention.plugins.android.testRunner
      def packageName = args[(PACKAGE)]
      def annotation  = args[(ANNOTATION)]
      def name        = args[(NAME)] ?: "instrumentation-tests-$numRunners"
      def options     = args[(OPTIONS)] ?: []

      if (options instanceof String || options instanceof GString) {
        options = [options]
      }
      
      // always wait for tests to finish
      options << "-w"
      // enable support for Zutubi's JUnit report test runner
      options << "-e reportFilePath ${name}.xml"
           
      if (packageName) {
        packageRunners[expandPackageName(packageName)] = buildRunner(testRunner, name, options)
      } else if (annotation) {
        annotationRunners[annotation] = buildRunner(testRunner, name, options)
      } else {
        defaultConfig = buildRunner(testRunner, name, options)
      }
    }
    
    boolean performDefaultRun() {
      packageRunners.isEmpty() && annotationRunners.isEmpty()
    }
    
    int getNumRunners() {
      packageRunners.size() + annotationRunners.size()
    }
    
    def buildRunner(def testRunner, def name, def options) {
      def wrapper = new Expando()
      wrapper.runner = testRunner
      wrapper.name = name
      wrapper.options = options
      wrapper
    }
    
    String expandPackageName(String packageName) {
      if (!packageName.startsWith(getTestPackage())) {
        return "${getTestPackage()}.${packageName}"
      }
      return packageName;
    }
  }
  
  def testPackage
  def testRunnerConfig
  def testReportsSourcePath
  def testReportsTargetPath
  
  def InstrumentationTestsTask() {
    logger.info("Running instrumentation tests...")
    
    this.testPackage = ant['manifest.package']
    this.testedPackage = ant['tested.manifest.package']
    this.testRunnerConfig = new TestRunnerConfig()
    
    if (testedPackage) { // this is only set for instrumentation projects
      this.testReportsSourcePath = "/data/data/$testedPackage/files"
      this.testReportsTargetPath = project.file('build/test-results').toString()
    }
    
    onlyIf {
      boolean isTestingOtherPackage = testedPackage != null
      if (!isTestingOtherPackage) { 
        logger.warn "!! Skipping androidInstrument task since no target package was specified"
      }
      isTestingOtherPackage
    }
  } 
  
  /**
   * Used to configure test runners using a closure, e.g.:
   * <code>
   * androidInstrument {
   *   runners {
   *     run testpackage: "com.my.package", with: "com.my.TestRunner"  
   *   }
   * }
   * </code>
   * @param config
   * @return
   */
  def runners(Closure config) {
    config.delegate = testRunnerConfig
    config()
  }
  
  @Override
  def exec() {

    if (testRunnerConfig.performDefaultRun()) {
      performTestRun(testRunnerConfig.defaultConfig)
    } else {
      // execute package specific runners
      testRunnerConfig.packageRunners.each {
        def packageName = it.key
        def runConfig   = it.value
        performTestRun(runConfig, "-e package $packageName")
      }
      // execute annotation specific runners
      testRunnerConfig.annotationRunners.each {
        def annotation = it.key
        def runConfig = it.value
        performTestRun(runConfig, "-e annotation $annotation")
      }
    }
  }
  
  def performTestRun(def runConfig, def filter = null) {
    if (filter) {
      runConfig.options << filter
    }
    
    // run the tests
    setArgs(["shell", "am instrument " + runConfig.options.join(" ") + " $testPackage/$runConfig.runner"])
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

    def pullTask = project.task('publishTestReport', type: AdbExec, overwrite: true)
    pullTask.args "pull", "$reportFile", "$testReportsTargetPath"
    pullTask.exec()
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
      if (it.matches("^OK \\(([0-9]+ tests){1}\\)")) {
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
