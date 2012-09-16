/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jvoegele.gradle.tasks.android

class ApkBuilderTask_r8 extends AndroidAntTask {
  ApkBuilderTask_r8(project) {
    super(project)
  }

  /**
   * Execute the apkbuilder task.
   * @param args Map of keyword arguments.  Supported keywords are sign and
   *             verbose, both of which should be boolean values if provided.
   */
  void execute(Map args) {
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
      project.configurations.runtime.each { jarfile(path: it) }
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
