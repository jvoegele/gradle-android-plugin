package com.jvoegele.gradle.tasks.android

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention 
import com.jvoegele.gradle.tasks.android.AndroidSdkToolsFactory 
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

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
  
  public AndroidPackageTask() {
    // Initialize internal data
    androidConvention = project.convention.plugins.android
    sdkTools = new AndroidSdkToolsFactory(project)
    ant = project.ant
    
    // Create necessary directories for this task
    androidConvention.apkArchivePath.getParentFile().mkdirs()
  }
  
  @TaskAction
  protected void process() {
    
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
    File temp = new File (project.libsDir, androidConvention.apkBaseName + "-unaligned.apk")
    ant.copy(file: androidConvention.apkArchivePath.toString(), toFile: temp.toString())
    // Do the alignment
    zipAlign(temp, androidConvention.apkArchivePath)
    // Remove temp file
    temp.delete()
    
    logger.info("Final Package: " + androidConvention.apkArchivePath)
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
    ant.signjar(jar: project.jar.archivePath,
        signedjar: project.jar.archivePath,
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
      //arg(path: project.sourceSets.main.classesDir)
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