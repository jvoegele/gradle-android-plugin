package com.jvoegele.gradle.plugins.android

abstract class AbstractAndroidSetup implements AndroidSetup {
  protected project
	
  AbstractAndroidSetup(project) {
    this.project = project
  }
}