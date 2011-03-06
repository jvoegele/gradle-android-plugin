package com.jvoegele.gradle.plugins.android;

import org.gradle.api.Project 

class AndroidPluginConvention {
  Project project
  File resDir
  File genDir
  File assetsDir
  File nativeLibsDir
  File androidManifest
  File intermediateDexFile
  String instrumentationTestsRunner
  private String apkBaseName
  private File apkArchivePath
  
  AndroidPluginConvention(Project project) {
    this.project = project
	
	// Input paths
    resDir = new File(project.projectDir, 'res')
    assetsDir = new File(project.projectDir, 'assets')
    nativeLibsDir = new File(project.projectDir, 'libs')
    androidManifest = new File(project.projectDir, 'AndroidManifest.xml')
	
	// Output paths
    genDir = new File(project.buildDir, 'gen')
    intermediateDexFile = new File(project.libsDir, "classes.dex")
    
    // instrumentation conventions
    instrumentationTestsRunner = "android.test.InstrumentationTestRunner"
  }
  
  /**
   * This value has to be calculated dynamically
   * @return
   */
  public String getApkBaseName() {
    apkBaseName = project.jar.baseName 
    if (project.jar.appendix != null && project.jar.appendix.length() > 0) {
      apkBaseName += "-"+project.jar.appendix
    }
    if (project.version != null && project.version.length() > 0) {
      apkBaseName += "-"+project.version
    }
    if (project.jar.classifier != null && project.jar.classifier.length() > 0) {
      apkBaseName += "-"+project.jar.classifier
    }
    return apkBaseName 
  }
  
  /**
   * This value has to be calculated dynamically
   * @return
   */
  public File getApkArchivePath() {
    apkArchivePath = new File (project.distsDir, getApkBaseName() + ".apk")
    return apkArchivePath
  }
}
