package com.jvoegele.gradle.tasks.android

class AaptExecTask_r8 extends AndroidAntTask {

  public AaptExecTask_r8(project) {
    super(project);
  }

  /* (non-Javadoc)
   * @see com.jvoegele.gradle.tasks.android.AndroidAntTask#execute(java.util.Map)
   */
  @Override
  public void execute(Map args) {
    ant.aaptexec(executable: ant.aapt,
                 command: args.get('command', 'package'),
                 manifest: androidConvention.androidManifest.path,
                 assets: androidConvention.assetsDir,
                 androidjar: ant['android.jar'],
                 apkfolder: project.libsDir,
                 resourcefilename: androidConvention.resourceFileName) {
      res(path: androidConvention.resDir.path)
    }
  }

}
