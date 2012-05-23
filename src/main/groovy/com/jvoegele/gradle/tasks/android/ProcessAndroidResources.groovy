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

package com.jvoegele.gradle.tasks.android;

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

class ProcessAndroidResources extends DefaultTask {
  boolean verbose

  AndroidPluginConvention androidConvention
  File genDir

  // Define which are the AIDL files (assuming they are created in the main sourceSet,
  // and that we have only one main src directory).
  String aidlDir = project.sourceSets.main.java.srcDirs.iterator().next().getAbsolutePath()
  FileTree aidlFiles = project.fileTree(dir: aidlDir, include: "**/*.aidl")

  ProcessAndroidResources () {
    super()

    androidConvention = project.convention.plugins.android
    genDir = androidConvention.genDir

    // Set input and output files and directories for this task
    inputs.file (androidConvention.androidManifest.absolutePath)
    inputs.files (aidlFiles)
    inputs.dir (androidConvention.resDir.absolutePath)
    outputs.dir (genDir.absolutePath)
  }


  @TaskAction
  protected void process() {
    genDir.mkdirs()

    // Check if there is at least one AIDL file
    if (!aidlFiles.isEmpty()) {
      project.logger.info("Generating AIDL java files...")
      project.ant.apply(executable: ant.aidl, failonerror: "true") {
        arg(value: "-I${aidlDir}")
        arg(value: "-o${genDir.absolutePath}")
        fileset(dir: aidlDir) {
          include(name: '**/*.aidl')
        }

        // Note by Fabio: android.aidl is empty and the -p option wants a "preprocess" file in input, which
        // I don't know what is or where came from, so I omitted.
//        arg(value: '-p')
//        arg(path: ant.references['android.aidl'])
      }
    }

    project.logger.info("Generating R.java / Manifest.java from the resources...")
    project.ant.exec(executable: ant.aapt, failonerror: "true") {
      arg(value: "package")
      if (verbose) arg(line: "-v")
      arg(value: "-m")
      arg(value: "-J")
      arg(path: genDir.absolutePath)
      arg(value: "-M")
      arg(path: androidConvention.androidManifest.absolutePath)
      androidConvention.resDirs.each { File file ->
        arg(value: "-S")
        arg(path: file.absolutePath)
      }
      arg(value: "-I")
      arg(path: ant['android.jar'])
    }
  }
}
