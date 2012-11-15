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

package com.jvoegele.gradle.plugins.android

class AndroidSetupFactory {
  private static final String SOURCE_PROPERTIES_FILE = 'source.properties'
  private static final String PKG_REVISION_PROPERTY = 'Pkg.Revision'

  private project
  private int toolsRevision = -1

  AndroidSetupFactory(project) {
    this.project = project
  }

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

  AndroidSetup getAndroidSetup() {
    if (this.androidSdkToolsRevision < 14) {
      return new AndroidSetup_r13(project)
    } else if (this.androidSdkToolsRevision < 17) {
      return new AndroidSetup_r14(project)
    } else if (this.androidSdkToolsRevision < 18) {
      return new AndroidSetup_r17(project)
    } else if (this.androidSdkToolsRevision < 21) {
      return new AndroidSetup_r18(project)
    } else {
      return new AndroidSetup_r21(project)
    }
  }
}
