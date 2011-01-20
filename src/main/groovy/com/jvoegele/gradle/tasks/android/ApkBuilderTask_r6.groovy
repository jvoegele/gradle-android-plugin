package com.jvoegele.gradle.tasks.android

class ApkBuilderTask_r6 extends AndroidAntTask {
  public ApkBuilderTask_r6(project) {
    super(project)
  }

  /**
   * Execute the apkbuilder task.
   * @param args Map of keyword arguments.  Supported keywords are sign and
   *             verbose, both of which should be boolean values if provided.
   */
  public void execute(Map args) {
    assert ant != null
    ant.apkbuilder(outfolder: project.buildDir,
      basename: project.name,
      signed: args.get('sign', false),
      'verbose': args.get('verbose', false)) {
        ant.file(path: androidConvention.intermediateDexFile.absolutePath)
        //sourcefolder(path: project.sourceSets.main.java)
        nativefolder(path: androidConvention.nativeLibsDir)
        //jarfolder(path: androidConvention.nativeLibsDir)
      }
  }
}
