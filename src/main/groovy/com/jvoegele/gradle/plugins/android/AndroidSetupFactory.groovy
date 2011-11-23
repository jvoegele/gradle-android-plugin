package com.jvoegele.gradle.plugins.android

class AndroidSetupFactory {
  private static final String SOURCE_PROPERTIES_FILE = 'source.properties'
  private static final String PKG_REVISION_PROPERTY = 'Pkg.Revision'

  private project
  private int toolsRevision = -1

  AndroidSetupFactory(project) {
	this.project = project
  }

  int getAndroidSdkToolsRevision() {
    if (toolsRevision < 0) {
      def ant = project.ant
      def toolsDir = new File(ant['sdk.dir'], 'tools')
      assert toolsDir.exists()
      def sourcePropertiesFile = new File(toolsDir, SOURCE_PROPERTIES_FILE)
      assert sourcePropertiesFile.exists()
      ant.property(file: sourcePropertiesFile)
      toolsRevision = Integer.parseInt(ant[PKG_REVISION_PROPERTY])
    }

    return toolsRevision
  }

  AndroidSetup getAndroidSetup() {
	if (this.androidSdkToolsRevision < 14) {
	  return new AndroidSetup_r13(project)
	} else {
	  return new AndroidSetup_r14(project)
	}
  } 
}