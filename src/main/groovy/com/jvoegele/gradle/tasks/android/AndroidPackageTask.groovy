package com.jvoegele.gradle.tasks.android

import groovy.lang.Closure;

import java.io.File

import org.gradle.api.Task;
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskInputs
import org.gradle.api.tasks.TaskOutputs

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention

/**
 * 
 * @author think01
 *
 */
class AndroidPackageTask extends ConventionTask {
  
  // Public configuration properties
  public String keyStore
  public String keyAlias
  public String keyStorePassword
  public String keyAliasPassword
  public boolean verbose

  // Internal fields
  AndroidPluginConvention androidConvention
  AndroidSdkToolsFactory sdkTools
  def ant
  
  public File getTempFile() {
    return new File (project.libsDir, androidConvention.getApkBaseName() + "-unaligned.apk")
  }
  
//  public TaskOutputs getOutputs() {
//    TaskOutputs to = super.getOutputs()
//    to.file (androidConvention.getApkArchivePath())
//    return to
//  }

  public AndroidPackageTask() {
    // Initialize internal data
    androidConvention = project.convention.plugins.android
    sdkTools = new AndroidSdkToolsFactory(project)
    ant = project.ant
    
  }

    
  @Override
  public Task configure(Closure closure) {

    // Do the base configuration
    Task configuredTask = super.configure(closure)
    
    // Declare inputs and outputs    
    inputs.file (project.jar.archivePath)
    outputs.file (androidConvention.getApkArchivePath())
    
    return configuredTask
  }


  @TaskAction
  protected void process() {
    
    // Create necessary directories for this task
    androidConvention.getApkArchivePath().getParentFile().mkdirs()
    
    if (keyStore != null && keyAlias != null) {
      // Dont' sign - it'll sign with the provided key
      createPackage(false)
      // Sign with provided key
      sign ()
    } else {
      // Sign with debug key
      createPackage(true)
    }
    
    // Create temporary file for the zipaligning
    File temp = getTempFile()
    ant.copy(file: androidConvention.getApkArchivePath().toString(), toFile: temp.toString())
    // Do the alignment
    zipAlign(temp, androidConvention.getApkArchivePath())
    // Touch temp file (to correctly manage tasks' inputs and outputs, as the output is the temp file itself)
    ant.touch (file: temp.getAbsolutePath());
    
    logger.info("Final Package: " + androidConvention.getApkArchivePath())
  }
  
  private void sign() {
    if (keyStorePassword == null || keyAliasPassword == null) {
      def console = System.console()
      keyStorePassword = new String(console.readPassword(
          "Please enter keystore password (store:${keyStore}): "))
      keyAliasPassword = new String(console.readPassword(
          "Please enter password for alias '${keyAlias}': "))
    }
    
    logger.info("Signing final apk...")
    ant.signjar(jar: androidConvention.getApkArchivePath().absolutePath,
        signedjar: androidConvention.getApkArchivePath().absolutePath,
        keystore: keyStore,
        storepass: keyStorePassword,
        alias: keyAlias,
        keypass: keyAliasPassword,
        verbose: verbose)
  }
  
  private void createPackage(boolean sign) {
    logger.info("Converting compiled files and external libraries into ${androidConvention.intermediateDexFile}...")
    ant.apply(executable: ant.dx, failonerror: true, parallel: true) {
      arg(value: "--dex")
      arg(value: "--output=${androidConvention.intermediateDexFile}")
      if (verbose) arg(line: "--verbose")
      fileset(file: project.jar.archivePath)
    }
    
    logger.info("Packaging resources")
    sdkTools.aaptexec.execute(command: 'package')
    sdkTools.apkbuilder.execute('sign': sign, 'verbose': verbose)
  }
  
  private void zipAlign(inPackage, outPackage) {
    logger.info("Running zip align on final apk...")
    ant.exec(executable: ant.zipalign, failonerror: true) {
      if (verbose) arg(line: '-v')
      arg(value: '-f')
      arg(value: 4)
      arg(path: inPackage)
      arg(path: outPackage)
    }
  }
}