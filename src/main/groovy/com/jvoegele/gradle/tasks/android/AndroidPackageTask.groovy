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

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author think01
 *
 */
class AndroidPackageTask extends DefaultTask {
  String keyStore
  String keyAlias
  String keyStorePassword
  String keyAliasPassword

  boolean verbose
  List<String> dexParams

  AndroidPluginConvention androidConvention
  AndroidSdkToolsFactory sdkTools

  private boolean keyStoreConfigurationDeprecationShown = false

  def ant

  AndroidPackageTask() {
    // Initialize internal data
    androidConvention = project.convention.plugins.android
    sdkTools = new AndroidSdkToolsFactory(project)
    ant = project.ant

    // Set static inputs and outputs for this task
    inputs.dir (androidConvention.resDir.absolutePath)
    inputs.dir (androidConvention.assetsDir.absolutePath)
    inputs.dir (androidConvention.nativeLibsDir.absolutePath)
    inputs.file (androidConvention.androidManifest.absolutePath)
    inputs.files (project.fileTree (dir: project.sourceSets.main.output.classesDir, exclude: "**/*.class"))
    dexParams = ['dex', "output=${androidConvention.intermediateDexFile}"]
  }

  // Inputs and outputs files and directories (to be determined dynamically)
  @InputFile
  File getJarArchivePath() {
    return project.jar.archivePath
  }

  @OutputFile
  File getUnsignedArchivePath() {
    return androidConvention.unsignedArchivePath
  }

  private void logKeyStoreConfigurationDeprecation() {
    if (!keyStoreConfigurationDeprecationShown) {
      logger.warn('Configuring signing on androidPackage task is deprecated. You should now configure it on androidSignAndAlign task.')
      keyStoreConfigurationDeprecationShown = true
    }
  }

  void setKeyStore(String keyStore) {
    this.keyStore = keyStore
    logKeyStoreConfigurationDeprecation()
  }

  void setKeyAlias(String keyAlias) {
    this.keyAlias = keyAlias
    logKeyStoreConfigurationDeprecation()
  }

  void setKeyStorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword
    logKeyStoreConfigurationDeprecation()
  }

  void setKeyAliasPassword(String keyAliasPassword) {
    this.keyAliasPassword = keyAliasPassword
    logKeyStoreConfigurationDeprecation()
  }

  @TaskAction
  protected void process() {
    // Create necessary directories for this task
    getUnsignedArchivePath().getParentFile().mkdirs()

    createPackage()
  }

  /**
   * Creates a classes.dex file containing all classes required at runtime, i.e.
   * all class files from the application itself, plus all its dependencies, and
   * bundles it into the final APK.
   *
   */
  private void createPackage() {
    logger.info("Converting compiled files and external libraries into ${androidConvention.intermediateDexFile}...")
    ant.apply(executable: ant.dx, failonerror: true, parallel: true, logError: true) {
      dexParams.each { arg(value: "--$it") }

      if (verbose) arg(line: "--verbose")

      // add classes from application JAR
      fileset(file: getJarArchivePath())

      // Add classes from application dependencies block, unless ProGuard is
      // enabled, in which case dependencies have already been packaged into
      // the application JAR.
      if (!project.proguard.enabled) {
        project.configurations.runtime.each { fileset file: it }
      }
    }

    logger.info("Packaging resources")
    sdkTools.aaptexec.execute(command: 'package')
    sdkTools.apkbuilder.execute('sign': false, 'verbose': verbose)
  }
}
