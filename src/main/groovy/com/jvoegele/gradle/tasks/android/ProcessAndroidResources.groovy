package com.jvoegele.gradle.tasks.android;

import groovy.lang.MetaClass;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention;

class ProcessAndroidResources extends ConventionTask {
  boolean verbose

  AndroidPluginConvention androidConvention
  File genDir

  public ProcessAndroidResources () {
    super()
    
    androidConvention = project.convention.plugins.android
    genDir = androidConvention.genDir
    
    // Set input and output files and directories for this task
    inputs.file (androidConvention.androidManifest.absolutePath)
    inputs.dir (androidConvention.resDir.absolutePath)
    outputs.dir (genDir.absolutePath)
  }
  
  
  @TaskAction
  protected void process() {
    genDir.mkdirs()
	project.logger.info("Generating AIDL java files")
	project.ant.exec(executable: ant.aidl, failonerror: "true") {
		arg(value: '-p')
		arg(path: ant.references['android.aidl'])
		arg(value: '-I')
		arg(path: project.tasks.compileJava.source.absolutePath)
		arg(value: '-o')
		arg(path: genDir.absolutePath)
		fileset(dir: 'src'){
			include(name: '** /*.aidl')
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

    /* TODO: include command to process .aidl files, as done by this Ant task:
     <apply executable="${aidl}" failonerror="true">
     <arg value="-p${android.aidl}" />
     <arg value="-I${source.absolute.dir}" />
     <arg value="-o${gen.absolute.dir}" />
     <fileset dir="${source.absolute.dir}">
     <include name="** /*.aidl" />
     */
  }
}
