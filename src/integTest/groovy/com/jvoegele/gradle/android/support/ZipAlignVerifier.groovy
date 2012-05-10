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

import org.gradle.internal.os.OperatingSystem
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
