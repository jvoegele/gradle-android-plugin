package com.jvoegele.gradle.tasks.android;

import groovy.lang.MetaClass

import org.gradle.api.file.FileTree
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention

class ProcessAndroidResources extends ConventionTask {
  boolean verbose

  AndroidPluginConvention androidConvention
  File genDir
  
  // Define which are the AIDL files (assuming they are created in the main sourceSet, 
  // and that we have only one main src directory).
  String aidlDir = project.sourceSets.main.java.srcDirs.iterator().next().getAbsolutePath() 
  FileTree aidlFiles = project.fileTree {
    from aidlDir
    include "**/*.aidl"
  }

  public ProcessAndroidResources () {
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
      arg(value: "-S")
      arg(path: androidConvention.resDir.absolutePath)
      arg(value: "-I")
      arg(path: ant['android.jar'])
    }
  }
}
