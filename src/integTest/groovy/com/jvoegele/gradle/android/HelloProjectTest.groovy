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

    p.archive('build/libs/hello-1.0.jar').assertContains 'com/jvoegele/gradle/android/hello/HelloActivity.class'

    p.archive('build/distributions/hello-1.0.apk').assertAligned()
    p.archive('build/libs/hello-1.0-unaligned.apk').assertNotAligned()
  }

  @Test
  void debugBuild() {
    def p = project('hello')

    p.runTasks 'clean', 'configureDebug', 'build', buildScript: 'debug-release.gradle'

    assertDebugBuild(p)
  }

  @Test
  void debugBuildOtherwise() {
    def p = project('hello')

    p.runTasks 'clean', 'build', buildScript: 'debug-release-otherwise.gradle'

    assertDebugBuild(p)
  }

  void assertDebugBuild(p) {
    p.fileExists 'build/libs/hello-1.0-debug.jar'
    p.fileExists 'build/libs/hello-1.0-debug-unaligned.apk'
    p.fileExists 'build/distributions/hello-1.0-debug.apk'
    p.fileDoesntExist 'build/libs/hello-1.0.jar'
    p.fileDoesntExist 'build/distributions/hello-1.0.apk'

    p.archive('build/libs/hello-1.0-debug.jar').assertContains 'com/jvoegele/gradle/android/hello/HelloActivity.class'

    p.archive('build/distributions/hello-1.0-debug.apk').assertAligned()
    p.archive('build/libs/hello-1.0-debug-unaligned.apk').assertNotAligned()

    p.archive('build/distributions/hello-1.0-debug.apk').assertSigned('CN=Android Debug, O=Android, C=US')
  }

  @Test
  void releaseBuild() {
    def p = project('hello')

    p.runTasks 'clean', 'configureRelease', 'build', buildScript: 'debug-release.gradle'

    assertReleaseBuild(p)
  }

  @Test
  void releaseBuildOtherwise() {
    def p = project('hello')

    p.runTasks 'clean', 'release', buildScript: 'debug-release-otherwise.gradle'

    assertReleaseBuild(p)
  }

  void assertReleaseBuild(p) {
    p.fileExists 'build/libs/hello-1.0.jar'
    p.fileExists 'build/libs/hello-1.0-unaligned.apk'
    p.fileExists 'build/distributions/hello-1.0.apk'

    p.fileDoesntExist 'build/libs/hello-1.0-debug.jar'
    p.fileDoesntExist 'build/distributions/hello-1.0-debug.apk'

    p.archive('build/libs/hello-1.0.jar').assertContains 'com/jvoegele/gradle/android/hello/HelloActivity.class'

    p.archive('build/distributions/hello-1.0.apk').assertAligned()
    p.archive('build/libs/hello-1.0-unaligned.apk').assertNotAligned()

    p.archive('build/distributions/hello-1.0.apk').assertSigned('CN=Gradle Android Plugin integration tests, O=Gradle Android Plugin, C=US')
  }
}
