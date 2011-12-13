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
