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

import com.jvoegele.gradle.tasks.android.AndroidSdkToolsFactory;

class HelloProjectTest extends AbstractIntegrationTest {
  private int toolsRevision = -1

  @Test
  void build() {
    def p = project('hello')

    p.runTasks 'clean', 'build', buildScript: 'simple.gradle'

    p.fileExists 'build/libs/hello-1.0.jar'
    p.fileExists 'build/libs/hello-1.0-unsigned.apk'
    p.fileExists 'build/libs/hello-1.0-unaligned.apk'
    p.fileExists 'build/distributions/hello-1.0.apk'

    p.archive('build/libs/hello-1.0.jar').assertContains 'com/jvoegele/gradle/android/hello/HelloActivity.class'

    p.archive('build/distributions/hello-1.0.apk').assertAligned()
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
    p.fileExists 'build/libs/hello-1.0-debug-unsigned.apk'
    p.fileExists 'build/libs/hello-1.0-debug-unaligned.apk'
    p.fileExists 'build/distributions/hello-1.0-debug.apk'
    p.fileDoesntExist 'build/libs/hello-1.0.jar'
    p.fileDoesntExist 'build/distributions/hello-1.0.apk'

    p.archive('build/libs/hello-1.0-debug.jar').assertContains 'com/jvoegele/gradle/android/hello/HelloActivity.class'

    p.archive('build/distributions/hello-1.0-debug.apk').assertAligned()
    if (this.androidSdkToolsRevision >= 8) {
      p.archive('build/distributions/hello-1.0-debug.apk').assertDebuggable()
    }

    p.archive('build/distributions/hello-1.0-debug.apk').assertSigned('CN=Android Debug, O=Android, C=US')
  }

  @Test
  void releaseBuild() {
    def p = project('hello')

    p.runTasks 'clean', 'configureRelease', 'build', buildScript: 'debug-release.gradle'

    assertReleaseBuild(p)
  }

  @Test
  void releaseBuildDeprecated() {
    def p = project('hello')

    p.runTasks 'clean', 'configureReleaseDeprecated', 'build', buildScript: 'debug-release.gradle'

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
    p.fileExists 'build/libs/hello-1.0-unsigned.apk'
    p.fileExists 'build/libs/hello-1.0-unaligned.apk'
    p.fileExists 'build/distributions/hello-1.0.apk'

    p.fileDoesntExist 'build/libs/hello-1.0-debug.jar'
    p.fileDoesntExist 'build/distributions/hello-1.0-debug.apk'

    p.archive('build/libs/hello-1.0.jar').assertContains 'com/jvoegele/gradle/android/hello/HelloActivity.class'

    p.archive('build/distributions/hello-1.0.apk').assertAligned()
    if (this.androidSdkToolsRevision >= 8) {
      p.archive('build/distributions/hello-1.0.apk').assertNotDebuggable()
    }

    p.archive('build/distributions/hello-1.0.apk').assertSigned('CN=Gradle Android Plugin integration tests, O=Gradle Android Plugin, C=US')
  }
  
  int getAndroidSdkToolsRevision() {
    if (toolsRevision < 0) {
      def toolsDir = new File(System.getenv("ANDROID_HOME"), "tools")
      assert toolsDir.exists()
      def sourcePropertiesFile = new File(toolsDir, AndroidSdkToolsFactory.SOURCE_PROPERTIES_FILE)
      assert sourcePropertiesFile.exists()

      Properties props = new Properties()
      props.load(new FileInputStream(sourcePropertiesFile))

      toolsRevision = Integer.parseInt(props[AndroidSdkToolsFactory.PKG_REVISION_PROPERTY])
    }

    return toolsRevision
  }
}
