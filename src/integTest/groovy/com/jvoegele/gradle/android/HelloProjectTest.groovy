package com.jvoegele.gradle.android

import org.gradle.api.PathValidation
import org.junit.Test

class HelloProjectTest extends AbstractIntegrationTest {
  @Test
  void projectGetsBuilt() {
    def p = project('hello')

    p.runTasks 'clean', 'build', buildScript: 'simple.gradle'

    p.file 'build/libs/hello-1.0.jar', PathValidation.FILE
    p.file 'build/libs/hello-1.0-unaligned.apk', PathValidation.FILE
  }
}
