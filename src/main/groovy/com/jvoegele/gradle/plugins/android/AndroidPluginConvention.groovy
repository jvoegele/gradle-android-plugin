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

  AndroidPluginConvention(Project project) {
    this.project = project
	
	// Input paths
    resDir = new File(project.projectDir, 'res')
    assetsDir = new File(project.projectDir, 'assets')
    nativeLibsDir = new File(project.projectDir, 'libs')
    androidManifest = new File(project.projectDir, 'AndroidManifest.xml')
	
	// Output paths
    // FIXME (Matthias): I find this misleading, this is NOT conventional; the gen/ folder
    // typically sits at the project root, not inside the build/ folder, that's a Gradle thing.
    // In fact, for the EclipseEnhancement to work, I had to hack around this by removing this
    // entry and replacing it with $projectDir/gen, which is the actual convention.
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
    def nameParts = [project.jar.baseName]
    if (project.jar.appendix) {
      nameParts << project.jar.appendix
    }
    if (project.version) {
      nameParts << project.version
    }
    if (project.jar.classifier) {
      nameParts << project.jar.classifier
    }
    return nameParts.join('-')
  }
  
  /**
   * This value has to be calculated dynamically
   * @return
   */
  public File getApkArchivePath() {
    return new File (project.distsDir, "${apkBaseName}.apk")
  }

  public File getUnsignedArchivePath() {
    return new File(project.libsDir, "${apkBaseName}-unsigned.apk")
  }

  public File getUnalignedArchivePath() {
    return new File(project.libsDir, "${apkBaseName}-unaligned.apk")
  }
}
