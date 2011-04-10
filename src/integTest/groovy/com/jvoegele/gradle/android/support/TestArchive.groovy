package com.jvoegele.gradle.android.support

import java.util.jar.JarFile

import static org.junit.Assert.assertNotNull

class TestArchive {
  def project
  def archive

  def assertContains(path) {
    def jar = new JarFile(archive)
    assertNotNull "The archive ${archive} must contain ${path}", jar.getEntry(path)
  }

  def assertAligned() {
    new ZipAlignVerifier(project: project).verifyAligned archive
  }

  def assertNotAligned() {
    new ZipAlignVerifier(project: project).verifyNotAligned archive
  }

  def assertSigned(distinguishedName) {
    new SignVerifier(archive: archive).verify(distinguishedName)
  }
}
