package com.jvoegele.gradle.plugins.android

class AndroidSetupFactory {
  private project

  AndroidSetupFactory(project) {
	this.project = project
  }

  AndroidSetup getAndroidSetup() {
	return new AndroidSetup_r14(project)
  } 
}