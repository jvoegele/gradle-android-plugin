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

  AndroidPluginConvention(Project project) {
    this.project = project
    resDir = new File(project.projectDir, 'res')
    genDir = new File(project.buildDir, 'gen')
    assetsDir = new File(project.buildDir, 'assets')
    nativeLibsDir = new File(project.projectDir, 'libs')
    androidManifest = new File(project.projectDir, 'AndroidManifest.xml')
    intermediateDexFile = new File(project.buildDir, "classes.dex")
  }
}
