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

package com.jvoegele.gradle.plugins.android;

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class AndroidPluginConvention {
  Project project
  File resDir
  File genDir
  File assetsDir
  File nativeLibsDir
  File androidManifest
  File intermediateDexFile
  String resourceFileName
  String instrumentationTestsRunner
  FileCollection resDirs

  AndroidPluginConvention(Project project) {
    this.project = project

    // Input paths
    resDir = new File(project.projectDir, 'res')
    assetsDir = new File(project.projectDir, 'assets')
    nativeLibsDir = new File(project.projectDir, 'libs')
    androidManifest = new File(project.projectDir, 'AndroidManifest.xml')
    resDirs = project.files(resDir)

    // Output paths
    // FIXME (Matthias): I find this misleading, this is NOT conventional; the gen/ folder
    // typically sits at the project root, not inside the build/ folder, that's a Gradle thing.
    // In fact, for the EclipseEnhancement to work, I had to hack around this by removing this
    // entry and replacing it with $projectDir/gen, which is the actual convention.
    genDir = new File(project.buildDir, 'gen')

    intermediateDexFile = new File(project.libsDir, "classes.dex")

    resourceFileName = project.name + ".ap_"

    // instrumentation conventions
    instrumentationTestsRunner = "android.test.InstrumentationTestRunner"
  }

  /**
   * This value has to be calculated dynamically
   * @return
   */
  String getApkBaseName() {
    def nameParts = [project.jar.baseName]

    if (project.jar.appendix) {
      nameParts << project.jar.appendix
    }

    if (project.version) {
      nameParts << project.version
    }

    if (project.jar.classifier) {
      nameParts << project.jar.classifier
    }

    return nameParts.join('-')
  }

  /**
   * This value has to be calculated dynamically
   * @return
   */
  File getApkArchivePath() {
    return new File (project.distsDir, "${apkBaseName}.apk")
  }

  File getUnsignedArchivePath() {
    return new File(project.libsDir, "${apkBaseName}-unsigned.apk")
  }
}
