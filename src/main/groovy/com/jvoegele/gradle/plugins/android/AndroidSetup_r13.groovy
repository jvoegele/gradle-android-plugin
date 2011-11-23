package com.jvoegele.gradle.plugins.android

class AndroidSetup_r13 extends AbstractAndroidSetup {
  AndroidSetup_r13(project) {
	super(project)
  }

  void setup() {
	def ant = project.ant
	def androidConvention = project.convention.plugins.android

	def sdkDir = ant['sdk.dir']
	def toolsDir = new File(sdkDir, "tools")
    def platformToolsDir = new File(sdkDir, "platform-tools")

	ant.condition('property': "exe", value: ".exe", 'else': "") { os(family: "windows") }
	if (platformToolsDir.exists()) { // since SDK r8, adb is moved from tools to platform-tools
      ant.property(name: "adb", location: new File(platformToolsDir, "adb${ant['exe']}"))
    } else {
      ant.property(name: "adb", location: new File(toolsDir, "adb${ant['exe']}"))
    }
    ant.property(name: "zipalign", location: new File(toolsDir, "zipalign${ant['exe']}"))
    ant.property(name: 'adb.device.arg', value: '')

    def outDir = project.buildDir.absolutePath
    ant.property(name: "resource.package.file.name", value: "${project.name}.ap_")

    ant.taskdef(name: 'setup', classname: 'com.android.ant.SetupTask', classpathref: 'android.antlibs')

    // The following properties are put in place by the setup task:
    // android.jar, android.aidl, aapt, aidl, and dx
    ant.setup('import': false)

    ant.taskdef(name: "xpath", classname: "com.android.ant.XPathTask", classpathref: "android.antlibs")
    ant.taskdef(name: "aaptexec", classname: "com.android.ant.AaptExecLoopTask", classpathref: "android.antlibs")
    ant.taskdef(name: "apkbuilder", classname: "com.android.ant.ApkBuilderTask", classpathref: "android.antlibs")

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