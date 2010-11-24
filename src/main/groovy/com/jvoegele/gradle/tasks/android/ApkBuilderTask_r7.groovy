package com.jvoegele.gradle.tasks.android

import java.util.Map;

class ApkBuilderTask_r7 extends AndroidAntTask {
  public ApkBuilderTask_r7(project) {
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
                   resourcefile: ant['resource.package.file.name'],
                   apkfilepath: ant.properties["out.debug.unaligned.package"],
                   signed: args.get('sign', false),
                   abifilter: '',
                   hascode: ant['manifest.hasCode'],
                   verbose: args.get('verbose', false)) {
      dex(path: androidConvention.intermediateDexFile)
      nativefolder(path: androidConvention.nativeLibsDir)
    }
/*
            <apkbuilder>
                <dex path="${intermediate.dex.file}"/>
                <sourcefolder path="${source.absolute.dir}" />
                <sourcefolder refid="android.libraries.src" />
                <jarfolder path="${external.libs.absolute.dir}" />
                <jarfolder refid="android.libraries.libs" />
                <nativefolder path="${native.libs.absolute.dir}" />
                <nativefolder refid="android.libraries.libs" />
                <extra-jars/>

 */
  }
}
