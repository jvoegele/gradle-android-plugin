package com.jvoegele.gradle.tasks.android

import com.jvoegele.gradle.tasks.android.exceptions.InstrumentationTestsFailedException;
import com.jvoegele.gradle.tasks.android.exceptions.AdbErrorException;

class InstrumentationTestsTask extends AdbExec {

  def InstrumentationTestsTask() {
    logger.info("Running instrumentation tests...")
    
    //TODO: make the test runner configurable or parse it from the manifest
    def testRunner  = 'android.test.InstrumentationTestRunner'
    def testPackage = ant['manifest.package']
    
    args "shell", "am instrument", "-w", "$testPackage/$testRunner"
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
    // for 'OK' to 
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
