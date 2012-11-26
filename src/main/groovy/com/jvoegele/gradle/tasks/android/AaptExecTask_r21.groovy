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

class AaptExecTask_r21 extends AndroidAntTask {
  AaptExecTask_r21(project) {
    super(project);
  }

  /* (non-Javadoc)
   * @see com.jvoegele.gradle.tasks.android.AndroidAntTask#execute(java.util.Map)
   */
  @Override
  void execute(Map args) {
	// <aapt executable="${aapt}"
	//         command="package"
	//         versioncode="${version.code}"
	//         versionname="${version.name}"
	//         debug="${build.is.packaging.debug}"
	//         manifest="${out.manifest.abs.file}"
	//         assets="${asset.absolute.dir}"
	//         androidjar="${project.target.android.jar}"
	//         apkfolder="${out.absolute.dir}"
	//         nocrunch="${build.packaging.nocrunch}"
	//         resourcefilename="${resource.package.file.name}"
	//         resourcefilter="${aapt.resource.filter}"
	//         libraryResFolderPathRefid="project.library.res.folder.path"
	//         libraryPackagesRefid="project.library.packages"
	//         libraryRFileRefid="project.library.bin.r.file.path"
	//         previousBuildType="${build.last.target}"
	//         buildType="${build.target}"
	//         ignoreAssets="${aapt.ignore.assets}">
	//     <res path="${out.res.absolute.dir}" />
	//     <res path="${resource.absolute.dir}" />
	//     <!-- <nocompress /> forces no compression on any files in assets or res/raw -->
	//     <!-- <nocompress extension="xml" /> forces no compression on specific file extensions in assets and res/raw -->
	// </aapt>

	println "androidConvention.resDirs = ${androidConvention.resDirs}";

    ant.aapt(
        executable: ant.aapt,
        command: args.get('command', 'package'),
        // versioncode: ant['version.code'],
        // versionname: ant['version.name'],
        debug: project.jar.classifier && project.jar.classifier == 'debug',
        manifest: androidConvention.androidManifest.path,
        assets: androidConvention.assetsDir,
        androidjar: ant['android.jar'],
        apkfolder: project.libsDir,
        nocrunch: true,
        resourcefilename: androidConvention.resourceFileName,
        // resourcefilter="${aapt.resource.filter}"
        // previousBuildType="${build.last.target}"
        // buildType="${build.target}"
        // ignoreAssets="${aapt.ignore.assets}">
        libraryResFolderPathRefid: "project.library.res.folder.path",
        libraryPackagesRefid: "project.library.packages",
        libraryRFileRefid: "project.library.bin.r.file.path",
    ) {
      androidConvention.resDirs.each { File file ->
        res(path: file.path)
      }
    }
  }
}

