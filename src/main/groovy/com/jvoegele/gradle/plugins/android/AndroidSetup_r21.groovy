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

package com.jvoegele.gradle.plugins.android

class AndroidSetup_r21 extends AbstractAndroidSetup {
  AndroidSetup_r21(project) {
    super(project)
  }

  void setup() {
    def ant = project.ant
    def androidConvention = project.convention.plugins.android

    def sdkDir = ant['sdk.dir']
    def toolsDir = new File(sdkDir, "tools")
    def platformToolsDir = new File(sdkDir, "platform-tools")

    project.logger.info("sdkDir = ${sdkDir}");
    project.logger.info("toolsDir = ${toolsDir}");
    project.logger.info("platformToolsDir = ${platformToolsDir}");

    ant.condition('property': "exe", value: ".exe", 'else': "") { os(family: "windows") }
    ant.condition('property': "bat", value: ".bat", 'else': "") { os(family: "windows") }

    if (platformToolsDir.exists()) { // since SDK r8, adb is moved from tools to platform-tools
      ant.property(name: "adb", location: new File(platformToolsDir, "adb${ant['exe']}"))
    } else {
      ant.property(name: "adb", location: new File(toolsDir, "adb${ant['exe']}"))
    }

    ant.property(name: "zipalign", location: new File(toolsDir, "zipalign${ant['exe']}"))
    ant.property(name: 'adb.device.arg', value: '')

    def outDir = project.buildDir.absolutePath
    ant.property(name: "resource.package.file.name", value: "${project.name}.ap_")

    // Required since SDK r17
    ant.property(name: "out.absolute.dir", value:'.')

    ant.taskdef(resource: 'anttasks.properties', classpathref: 'android.antlibs')

    // The following properties are put in place by the setup task:
    // android.jar, android.aidl, aapt, aidl, and dx
    ant.gettype(
        projectTypeOut: "android.project.type",
    );

    ant.gettarget(
      androidJarFileOut: "android.jar",
      androidAidlFileOut: "android.aidl",
      bootClassPathOut: "android.target.classpath",
      targetApiOut: "project.target.apilevel",
      minSdkVersionOut: "project.minSdkVersion",
      //renderScriptExeOut: "renderscript",
      // renderScriptIncludeDirOut: "android.rs",
      //bootclasspathrefOut: "android.target.classpath",
      //projectLibrariesRootOut: "project.libraries",
      //projectLibrariesJarsOut: "project.libraries.jars",
      //projectLibrariesResOut: "project.libraries.res",
      //projectLibrariesPackageOut: "project.libraries.package",
      //projectLibrariesLibsOut: "project.libraries.libs",
      // targetApiOut: "target.api",
    );

    ant.dependency(
      libraryFolderPathOut: "project.library.folder.path",
      libraryPackagesOut: "project.library.packages",
      libraryManifestFilePathOut: "project.library.manifest.file.path",
      libraryResFolderPathOut: "project.library.res.folder.path",
      libraryBinAidlFolderPathOut: "project.library.bin.aidl.folder.path",
      libraryRFilePathOut: "project.library.bin.r.file.path",
      libraryNativeFolderPathOut: "project.library.native.folder.path",
      jarLibraryPathOut: "project.all.jars.path",

      targetApi: ant['project.target.apilevel'],
      verbose: true
    );

    ant.property(name: "aapt", location: new File(platformToolsDir, "aapt${ant['exe']}"))
    ant.property(name: "aidl", location: new File(platformToolsDir, "aidl${ant['exe']}"))
    ant.property(name: "dx", location: new File(platformToolsDir, "dx${ant['bat']}"))
    ant.property(name: "renderscript", location: new File(platformToolsDir, "llvm-rs-cc${ant['bat']}"))

    ant.property(name: "android.tools.dir", location:"${ant['sdk.dir']}/tools")
    ant.property(name: "android.platform.tools.dir", location:"${ant['sdk.dir']}/platform-tools")

    ant.path(id: "android.renderscript.include.path") {
      pathelement(location:"${ant['android.platform.tools.dir']}/renderscript/include");
      pathelement(location:"${ant['android.platform.tools.dir']}/renderscript/clang-include");
	};

    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/@package", output: "manifest.package")
    // TODO: there can be several instrumentations defined
    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/instrumentation/@android:targetPackage", output: "tested.manifest.package")
    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/application/@android:hasCode", output: "manifest.hasCode", 'default': "true")

    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/instrumentation/@android:name", output: "android.instrumentation")

    if (ant['android.instrumentation']) {
      androidConvention.instrumentationTestsRunner = ant['android.instrumentation']
    }
  }
}

