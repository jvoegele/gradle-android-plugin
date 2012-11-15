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

package com.jvoegele.gradle.tasks.android

/**
 * Factory for creating Ant tasks and other tools packaged with the Android SDK.
 */
class AndroidSdkToolsFactory {
  private static final String SOURCE_PROPERTIES_FILE = 'source.properties'
  private static final String PKG_REVISION_PROPERTY = 'Pkg.Revision'

  private final project
  private final logger
  private int toolsRevision = -1

  /**
   * Create a new AndroidSdkToolsFactory for the given project.
   */
  AndroidSdkToolsFactory(project) {
    this.project = project
    this.logger = project.logger
  }

  /**
   * Returns the value of the Pkg.Revision property from the source.properties
   * file in the Android SDK's tools directory.
   */
  int getAndroidSdkToolsRevision() {
    if (toolsRevision < 0) {
      def ant = project.ant
      def toolsDir = new File(ant['sdk.dir'], 'tools')
      assert toolsDir.exists()
      def sourcePropertiesFile = new File(toolsDir, SOURCE_PROPERTIES_FILE)
      assert sourcePropertiesFile.exists()
      ant.property(file: sourcePropertiesFile)
      String toolsFullRevision = ant[PKG_REVISION_PROPERTY];
      if (toolsFullRevision.contains('.')) {
        toolsRevision = Integer.parseInt(toolsFullRevision.substring(0, toolsFullRevision.indexOf('.')))
      } else {
        toolsRevision = Integer.parseInt(toolsFullRevision)
      }
    }

    return toolsRevision
  }

  /**
   * Returns an <code>AndroidAntTask</code> that invokes the appropriate
   * apkbuilder for the active Android SDK tools revision.
   */
  AndroidAntTask getApkbuilder() {
    if (this.androidSdkToolsRevision < 7) {
      return new ApkBuilderTask_r6(project)
    } else if (this.androidSdkToolsRevision < 8) {
      return new ApkBuilderTask_r7(project)
    } else if (this.androidSdkToolsRevision < 14) {
      return new ApkBuilderTask_r8(project)
    } else {
      return new ApkBuilderTask_r14(project)
    }
  }

  AndroidAntTask getAaptexec() {
    if (this.androidSdkToolsRevision < 8) {
      return new AaptExecTask_r7(project)
    } else if (this.androidSdkToolsRevision < 14) {
      return new AaptExecTask_r8(project)
    } else if (this.androidSdkToolsRevision < 18) {
      return new AaptExecTask_r14(project)
    } else if (this.androidSdkToolsRevision < 21) {
      return new AaptExecTask_r18(project)
    } else {
      return new AaptExecTask_r21(project)
    }
  }
}
