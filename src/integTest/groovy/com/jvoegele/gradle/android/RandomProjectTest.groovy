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

package com.jvoegele.gradle.android

import org.junit.Test

class RandomProjectTest extends AbstractIntegrationTest {
  @Test
  void build() {
    def p = project('random')

    p.runTasks 'clean', 'build'

    p.fileExists 'build/libs/random-1.0.jar'
    p.fileExists 'build/libs/random-1.0-unsigned.apk'
    p.fileExists 'build/libs/random-1.0-unaligned.apk'
    p.fileExists 'build/distributions/random-1.0.apk'

    p.archive('build/libs/random-1.0.jar').assertContains 'com/jvoegele/gradle/android/random/RandomActivity.class'

    p.archive('build/distributions/random-1.0.apk').assertSigned('CN=Android Debug, O=Android, C=US')
  }
}
