package com.jvoegele.gradle.plugins.android;

import org.gradle.api.Project 

class AndroidPluginConvention {
  Project project
  File resDir
  File genDir
  String androidManifest = "AndroidManifest.xml"

  AndroidPluginConvention(Project project) {
    this.project = project
    resDir = new File(project.rootDir, 'res')
    genDir = new File(project.buildDir, 'gen')
  }
}
