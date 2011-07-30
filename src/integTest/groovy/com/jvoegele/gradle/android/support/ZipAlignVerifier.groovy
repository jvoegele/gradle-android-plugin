package com.jvoegele.gradle.android.support

import org.gradle.util.OperatingSystem

import static org.junit.Assert.assertEquals

class ZipAlignVerifier {
  def project

  def verifyAligned(archive) {
    doVerify archive, true
  }

  def verifyNotAligned(archive) {
    doVerify archive, false
  }

  def doVerify(archive, expectedResult) {
    def androidTools = new File(System.getenv("ANDROID_HOME"), "tools")
    def zipalign = new File(androidTools, "zipalign${OperatingSystem.current().isWindows() ? '.exe' : ''}")

    def result = project.exec {
      executable zipalign.canonicalPath
      args '-c', 4, archive
      ignoreExitValue = true
    }

    assertEquals("The archive ${archive} ${expectedResult ? 'must' : 'must NOT'} be zipaligned",
            expectedResult, result.exitValue == 0)
  }
}
