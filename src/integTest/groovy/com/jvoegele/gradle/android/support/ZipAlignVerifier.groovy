package com.jvoegele.gradle.android.support

import org.gradle.os.OperatingSystem

import static org.junit.Assert.assertTrue

class ZipAlignVerifier {
  def project

  def verifyAligned(archive) {
    def androidTools = new File(System.getenv("ANDROID_HOME"), "tools")
    def zipalign = new File(androidTools, "zipalign${OperatingSystem.current().isWindows() ? '.exe' : ''}")

    def result = project.exec {
      executable zipalign.canonicalPath
      args '-c', 4, archive
      ignoreExitValue = true
    }

    assertTrue("The archive ${archive} must be zipaligned", result.exitValue == 0)
  }
}
