package com.jvoegele.gradle.tasks.android

class AaptExecTask_r14 extends AndroidAntTask {

  public AaptExecTask_r14(project) {
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
                 resourcefilename: androidConvention.resourceFileName,
                 projectLibrariesResName: 'project.libraries.res',
                 projectLibrariesPackageName: 'project.libraries.package') {
      res(path: androidConvention.resDir.path)
    }
  }

}
