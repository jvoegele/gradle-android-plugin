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
    
    def defaultTestRunner = getProject().convention.plugins.android.testRunner
    def packageRunners    = [:]
    def annotationRunners = [:]
    
    def run(args = [(RUNNER): defaultTestRunner]) {
      def testRunner  = args[(RUNNER)]
      def packageName = args[(PACKAGE)]
      def annotation  = args[(ANNOTATION)] 
     
      if (packageName) {
        packageRunners[expandPackageName(packageName)] = testRunner
      } else if (annotation) {
        annotationRunners[annotation] = testRunner
      } else {
        defaultTestRunner = testRunner
      }
    }
    
    boolean performDefaultRun() {
      packageRunners.isEmpty() && annotationRunners.isEmpty()
    }
    
    private String expandPackageName(String packageName) {
      if (!packageName.startsWith(getTestPackage())) {
        return "${getTestPackage()}.${packageName}"
      }
      return packageName;
    }
  }
  
  def testPackage
  def testRunnerConfig
  
  def InstrumentationTestsTask() {
    logger.info("Running instrumentation tests...")
    
    this.testPackage = ant['manifest.package']
    this.testRunnerConfig = new TestRunnerConfig()
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
    String cmd = "am instrument -w "

    if (testRunnerConfig.performDefaultRun()) {
      setArgs(["shell", cmd + "$testPackage/$testRunnerConfig.defaultTestRunner"])
      super.exec()
    } else {
      // execute package specific runners
      testRunnerConfig.packageRunners.each {
        setArgs(["shell", cmd + "-e package ${it.key}", "$testPackage/${it.value}"])
        super.exec()
      }
      // execute annotation specific runners
      testRunnerConfig.annotationRunners.each {
        setArgs(["shell", cmd + "-e annotation ${it.key}", "$testPackage/${it.value}"])
        super.exec()
      }
    }
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
