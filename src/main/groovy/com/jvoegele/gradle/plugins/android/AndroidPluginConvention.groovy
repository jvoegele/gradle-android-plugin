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
  String apkBaseName
  File apkArchivePath
  
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
    apkBaseName = project.jar.baseName 
//        + (project.jar.appendix != null ? "-"+project.jar.appendix : "") 
//        + (project.version != null ? "-"+project.version : "") 
//        + (project.jar.classifier != null ? "-"+project.jar.classifier : "") 
//        + ".apk"
    apkArchivePath = new File (project.distsDir, apkBaseName + ".apk")
  }
}
