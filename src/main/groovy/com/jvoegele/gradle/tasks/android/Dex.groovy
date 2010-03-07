package com.jvoegele.gradle.tasks.android;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Task for converting Java class files into dex files for Dalvik.
 *
 * @author Jason Voegele (jason@jvoegele.com)
 */
class Dex extends ConventionTask {
  boolean verbose
  File intermediateDexFile = new File(project.buildDir, "classes.dex")
  Closure dependencyFilter = { true }

  @TaskAction
  def process() {
    ant.apply(executable: ant.dx, failonerror: true, parallel: true) {
      arg(value: "--dex")
      arg(value: "--output=${intermediateDexFile}")
      if (verbose) arg(line: "--verbose")
      arg(path: project.sourceSets.main.classesDir)
      project.configurations.compile.files(dependencyFilter).each { dependency -> fileset(file: dependency) }
      fileset(dir: project.buildDir, includes: "*.jar")
    }
  }
/*
    <macrodef name="dex-helper">
       <element name="external-libs" optional="yes" />
       <element name="extra-parameters" optional="yes" />
       <sequential>
         <echo>Converting compiled files and external libraries into ${intermediate.dex.file}...
         </echo>
         <apply executable="${dx}" failonerror="true" parallel="true">
             <arg value="--dex" />
             <arg value="--output=${intermediate.dex.file}" />
             <extra-parameters />
             <arg line="${verbose.option}" />
             <!-- <arg path="${out.classes.absolute.dir}" /> -->
             <fileset dir="${external.libs.absolute.dir}" includes="*.jar" />
             <fileset dir="${out.dir}" includes="classes.min.jar" />
             <external-libs />
         </apply>
       </sequential>
    </macrodef>

 */
}
