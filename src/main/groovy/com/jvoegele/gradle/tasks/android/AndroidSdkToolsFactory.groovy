package com.jvoegele.gradle.tasks.android

/**
 * Factory for creating Ant tasks and other tools packaged with the Android SDK.
 */
class AndroidSdkToolsFactory {
  private static final String SOURCE_PROPERTIES_FILE = 'source.properties'
  private static final String PKG_REVISION_PROPERTY = 'Pkg.Revision'

  private final project
  private final logger
  private int toolsRevision = -1

  /**
   * Create a new AndroidSdkToolsFactory for the given project.
   */
  public AndroidSdkToolsFactory(project) {
    this.project = project
    this.logger = project.logger
  }

  /**
   * Returns the value of the Pkg.Revision property from the source.properties
   * file in the Android SDK's tools directory.
   */
  public int getAndroidSdkToolsRevision() {
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

  /**
   * Returns an <code>AndroidAntTask</code> that invokes the appropriate
   * apkbuilder for the active Android SDK tools revision.
   */
  public AndroidAntTask getApkbuilder() {
    if (this.androidSdkToolsRevision < 7) {
      return new ApkBuilderTask_r6(project)
    }
    else {
      return new ApkBuilderTask_r7(project)
    }
  }

  public AndroidAntTask getAaptexec() {
    return new AaptExecTask(project)
  }
}
