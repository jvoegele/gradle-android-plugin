package com.jvoegele.gradle.tasks.android;

import groovy.lang.MetaClass;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

class ProcessAndroidResources extends ConventionTask {
  boolean verbose

  @TaskAction
  protected void process() {
    def androidConvention = project.convention.plugins.android
    def genDir = androidConvention.genDir
    genDir.mkdirs()
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
