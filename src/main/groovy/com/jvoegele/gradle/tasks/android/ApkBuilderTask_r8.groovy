package com.jvoegele.gradle.tasks.android

class ApkBuilderTask_r8 extends AndroidAntTask {
  public ApkBuilderTask_r8(project) {
    super(project)
  }

  /**
   * Execute the apkbuilder task.
   * @param args Map of keyword arguments.  Supported keywords are sign and
   *             verbose, both of which should be boolean values if provided.
   */
  public void execute(Map args) {
    assert ant != null
    ant.apkbuilder(outfolder: project.libsDir,
                   resourcefile: ant['resource.package.file.name'],
                   apkfilepath: androidConvention.unsignedArchivePath,
                   debugsigning: args.get('sign', false),
                   abifilter: '',
                   hascode: ant['manifest.hasCode'],
                   verbose: args.get('verbose', false)) {
      dex(path: androidConvention.intermediateDexFile)
      // Takes resource files from the source folder - classes are processed by the dx command
      sourcefolder(path: project.sourceSets.main.output.classesDir)
	  if (project.sourceSets.main.output.resourcesDir.exists()) {
		  sourcefolder(path: project.sourceSets.main.output.resourcesDir)
	  }
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
