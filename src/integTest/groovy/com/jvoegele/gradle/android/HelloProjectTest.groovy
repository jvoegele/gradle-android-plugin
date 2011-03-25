package com.jvoegele.gradle.android

import org.gradle.api.PathValidation
import org.junit.Test

class HelloProjectTest extends AbstractIntegrationTest {
  @Test
  void build() {
    def p = project('hello')

    p.runTasks 'clean', 'build', buildScript: 'simple.gradle'

    p.file 'build/libs/hello-1.0.jar', PathValidation.FILE
    p.file 'build/libs/hello-1.0-unaligned.apk', PathValidation.FILE
    p.file 'build/distributions/hello-1.0.apk', PathValidation.NONE
  }

  @Test
  void assemble() {
    def p = project('hello')

    p.runTasks 'clean', 'assemble', buildScript: 'simple.gradle'

    p.file 'build/libs/hello-1.0.jar', PathValidation.FILE
    p.file 'build/libs/hello-1.0-unaligned.apk', PathValidation.FILE
    p.file 'build/distributions/hello-1.0.apk', PathValidation.FILE
  }

  @Test
  void debugAssemble() {
    def p = project('hello')

    p.runTasks 'clean', 'configureDebug', 'assemble', buildScript: 'debug-release.gradle'

    p.file 'build/libs/hello-1.0.jar', PathValidation.NONE
    p.file 'build/libs/hello-1.0-debug.jar', PathValidation.FILE
    p.file 'build/libs/hello-1.0-unproguarded.jar', PathValidation.NONE
    p.file 'build/libs/hello-1.0-debug-unaligned.apk', PathValidation.FILE
    p.file 'build/distributions/hello-1.0.apk', PathValidation.NONE
    p.file 'build/distributions/hello-1.0-debug.apk', PathValidation.FILE

    // TODO check that hello-1.0-debug.apk is signed by key with CN=Android Debug, O=Android, C=US
  }

  @Test
  void releaseAssemble() {
    def p = project('hello')

    p.runTasks 'clean', 'configureRelease', 'assemble', buildScript: 'debug-release.gradle'

    p.file 'build/libs/hello-1.0.jar', PathValidation.FILE
    p.file 'build/libs/hello-1.0-debug.jar', PathValidation.NONE
    p.file 'build/libs/hello-1.0-unproguarded.jar', PathValidation.FILE
    p.file 'build/libs/hello-1.0-unaligned.apk', PathValidation.FILE
    p.file 'build/distributions/hello-1.0.apk', PathValidation.FILE
    p.file 'build/distributions/hello-1.0-debug.apk', PathValidation.NONE

    // TODO check that hello-1.0.apk is signed by key with CN=Gradle Android Plugin integration tests, O=Gradle Android Plugin, C=US
  }
}
