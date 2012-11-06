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


import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention
import groovy.io.FileType

import java.util.regex.Matcher

class ProcessAndroidResources extends DefaultTask {
  boolean verbose

  AndroidPluginConvention androidConvention
  File genDir

  ProcessAndroidResources () {
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

    generateAIDLFiles()

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

    generateBuildConfigFile()
  }

  private void generateAIDLFiles( ) {
    project.logger.info( "Generating AIDL Java files..." )

    project.sourceSets.main.java.srcDirs.each() {
      def srcDir = it
      if ( srcDir.exists() ) {
        def aidlFileTree = project.fileTree( dir: srcDir, include: '**/*.aidl' )

        if ( !aidlFileTree.isEmpty() ) {
          aidlFileTree.getFiles().each() {
            def aidlFile = new File( it.toString() )

            project.ant.exec( executable: ant.aidl, failonerror: "true" ) {
              arg( value: "-I${srcDir.getAbsolutePath()}" )
              arg( value: "-o${genDir.absolutePath}" )
              arg( value: aidlFile.getAbsolutePath() )
            }
          }
        }
      }
    }
  }

  private void generateBuildConfigFile( ) {
    project.logger.info( "Generating BuildConfig.java..." )
    def packageDir = getPackageDir()

    // Replace all path separators to a dot and strip out the first dot.
    def packageDeclaration = packageDir.replaceAll( Matcher.quoteReplacement(File.separator), '.' )
    packageDeclaration = packageDeclaration.substring( 1, packageDeclaration.length() )

    def isDebug = project.version.endsWith("-SNAPSHOT")

    def BuildConfigFile = new File( genDir.absolutePath, packageDir + '/BuildConfig.java' )
    BuildConfigFile.write( "package ${packageDeclaration};\n\npublic final class BuildConfig {\n\tpublic final static boolean DEBUG = ${isDebug};\n}" )
  }

  private String getPackageDir( ) {
    def packageDir

    def genDir = new File( genDir.absolutePath )
    genDir.eachFileRecurse( FileType.FILES ) {
      if ( ( ~/R.java/ ).matcher( it.name ).find() ) {
        // Returns something like /com/example.
        packageDir = it.parent.replace( genDir.absolutePath, '' )
      }
    }

    packageDir
  }

}
