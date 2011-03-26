package com.jvoegele.gradle.android

import org.junit.Test

class HelloProjectTest extends AbstractIntegrationTest {
  @Test
  void build() {
    def p = project('hello')

    p.runTasks 'clean', 'build', buildScript: 'simple.gradle'

    p.fileExists 'build/libs/hello-1.0.jar'
    p.fileExists 'build/libs/hello-1.0-unaligned.apk'
    p.fileExists 'build/distributions/hello-1.0.apk'
  }

  @Test
  void debugBuild() {
    def p = project('hello')

    p.runTasks 'clean', 'configureDebug', 'build', buildScript: 'debug-release.gradle'

    p.fileExists 'build/libs/hello-1.0-debug.jar'
    p.fileExists 'build/libs/hello-1.0-debug-unaligned.apk'
    p.fileExists 'build/distributions/hello-1.0-debug.apk'
    p.fileDoesntExist 'build/libs/hello-1.0.jar'
    p.fileDoesntExist 'build/libs/hello-1.0-unproguarded.jar'
    p.fileDoesntExist 'build/distributions/hello-1.0.apk'

    new SignVerifier(archive: p.file('build/distributions/hello-1.0-debug.apk')).verify(
            'CN=Android Debug, O=Android, C=US')
  }

  @Test
  void releaseBuild() {
    def p = project('hello')

    p.runTasks 'clean', 'configureRelease', 'build', buildScript: 'debug-release.gradle'

    p.fileExists 'build/libs/hello-1.0.jar'
    p.fileExists 'build/libs/hello-1.0-unproguarded.jar'
    p.fileExists 'build/libs/hello-1.0-unaligned.apk'
    p.fileExists 'build/distributions/hello-1.0.apk'

    p.fileDoesntExist 'build/libs/hello-1.0-debug.jar'
    p.fileDoesntExist 'build/distributions/hello-1.0-debug.apk'

    new SignVerifier(archive: p.file('build/distributions/hello-1.0.apk')).verify(
            'CN=Gradle Android Plugin integration testsa, O=Gradle Android Plugin, C=US')
  }
}
