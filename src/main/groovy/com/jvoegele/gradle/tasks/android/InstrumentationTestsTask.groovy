package com.jvoegele.gradle.tasks.android

import com.jvoegele.gradle.tasks.android.exceptions.InstrumentationTestsFailedException;

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
    reader.eachLine {
      if (it.matches("FAILURES!!!")) {
        throw new InstrumentationTestsFailedException();
      }
    }
  }
}
