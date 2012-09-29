package com.jvoegele.gradle.tasks.android.exceptions;

class InstrumentationTestsFailedException extends AdbErrorException {

  def InstrumentationTestsFailedException() {
    super("There were test failures")
  }
  
}