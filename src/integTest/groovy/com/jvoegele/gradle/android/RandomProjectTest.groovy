package com.jvoegele.gradle.android

import org.junit.Test

class RandomProjectTest extends AbstractIntegrationTest {
  @Test
  void build() {
    def p = project('random')

    p.runTasks 'clean', 'build'

    p.fileExists 'build/libs/random-1.0.jar'
    p.fileExists 'build/distributions/random-1.0-unsigned.apk'
    p.fileExists 'build/distributions/random-1.0-unaligned.apk'
    p.fileExists 'build/distributions/random-1.0.apk'

    p.archive('build/libs/random-1.0.jar').assertContains 'com/jvoegele/gradle/android/random/RandomActivity.class'
  }
}
