package com.jvoegele.gradle.tasks.android

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention

/**
 * 
 * @author think01
 *
 */
class AndroidPackageTask extends ConventionTask {
  
  // Public configuration properties
  @Input public String keyStore
  @Input public String keyAlias
  @Input public String keyStorePassword
  @Input public String keyAliasPassword
  public boolean verbose

  // Inputs and outputs files and directories (to be determined dynamically)
  @InputFile
  public File getJarArchivePath() {
    return project.jar.archivePath
  }
  @OutputFile
  public File getApkArchivePath() {
    return androidConvention.getApkArchivePath()
  }
  
  // Internal fields
  AndroidPluginConvention androidConvention
  AndroidSdkToolsFactory sdkTools
  def ant

  public File getTempFile() {
    return new File (project.libsDir, androidConvention.getApkBaseName() + "-unaligned.apk")
  }
  
  public AndroidPackageTask() {
    // Initialize internal data
    androidConvention = project.convention.plugins.android
    sdkTools = new AndroidSdkToolsFactory(project)
    ant = project.ant

    // Set static inputs and outputs for this task
    inputs.dir (androidConvention.resDir.absolutePath)
    inputs.dir (androidConvention.assetsDir.absolutePath)
    inputs.dir (androidConvention.nativeLibsDir.absolutePath)
    inputs.file (androidConvention.androidManifest.absolutePath)
    inputs.files (project.fileTree (dir: project.sourceSets.main.classesDir, exclude: "**/*.class"))
  }
    
  @TaskAction
  protected void process() {
    
    // Create necessary directories for this task
    getApkArchivePath().getParentFile().mkdirs()
    
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
    ant.copy(file: getApkArchivePath().toString(), toFile: temp.toString())
    // Do the alignment
    zipAlign(temp, getApkArchivePath())
    
    logger.info("Final Package: " + getApkArchivePath())
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
    ant.signjar(jar: getApkArchivePath().absolutePath,
        signedjar: getApkArchivePath().absolutePath,
        keystore: keyStore,
        storepass: keyStorePassword,
        alias: keyAlias,
        keypass: keyAliasPassword,
        verbose: verbose)
  }
  
  /**
   * Creates a classes.dex file containing all classes required at runtime, i.e.
   * all class files from the application itself, plus all its dependencies, and
   * bundles it into the final APK.
   *
   * @param sign whether the APK should be signed with the release key or not
   */
  private void createPackage(boolean sign) {
    logger.info("Converting compiled files and external libraries into ${androidConvention.intermediateDexFile}...")
    ant.apply(executable: ant.dx, failonerror: true, parallel: true, logError: true) {
      arg(value: "--dex")
      arg(value: "--output=${androidConvention.intermediateDexFile}")
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
