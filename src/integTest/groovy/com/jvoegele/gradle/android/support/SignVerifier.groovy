package com.jvoegele.gradle.android.support

import java.security.cert.X509Certificate
import java.util.jar.JarFile
import javax.security.auth.x500.X500Principal

import static org.junit.Assert.fail

class SignVerifier {
  def archive

  def verify(distinguishedNameStr) {
    def jar = new JarFile(archive)
    def distinguishedName = new X500Principal(distinguishedNameStr)

    def entry = jar.getEntry('classes.dex')

    // the entry must be fully read before getting certificates
    readStreamAndDiscard(jar.getInputStream(entry))

    def ok = false
    entry.certificates.each { cert ->
      if (!(cert instanceof X509Certificate)) {
        return
      }

      if (cert.subjectX500Principal == distinguishedName) {
        ok = true
      }
    }

    if (!ok) {
      fail "The jar ${archive} is not signed by ${distinguishedNameStr}"
    }
  }

  def readStreamAndDiscard(stream) {
    while (stream.read() != -1) {
      // empty on purpose
    }
  }
}
