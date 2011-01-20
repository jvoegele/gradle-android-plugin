package com.jvoegele.gradle.plugins.android

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin

import com.jvoegele.gradle.enhancements.JavadocEnhancement
import com.jvoegele.gradle.tasks.android.AdbExec
import com.jvoegele.gradle.tasks.android.AndroidPackageTask
import com.jvoegele.gradle.tasks.android.ProGuard
import com.jvoegele.gradle.tasks.android.ProcessAndroidResources

/**
 * Gradle plugin that extends the Java plugin for Android development.
 *
 * @author Jason Voegele (jason@jvoegele.com)
 */
class AndroidPlugin implements Plugin<Project> {
  private static final ANDROID_PROCESS_RESOURCES_TASK_NAME = "androidProcessResources"
  private static final PROGUARD_TASK_NAME = "proguard"
  private static final ANDROID_PACKAGE_TASK_NAME = "androidPackage"
  private static final ANDROID_INSTALL_TASK_NAME = "androidInstall"
  private static final ANDROID_UNINSTALL_TASK_NAME = "androidUninstall"

  private static final PROPERTIES_FILES = ['local', 'build', 'default']
  private static final ANDROID_JARS = ['anttasks', 'sdklib', 'androidprefs', 'apkbuilder', 'jarutils']

  private AndroidPluginConvention androidConvention
  private sdkDir
  private toolsDir
  private platformToolsDir // used since SDK r8, so check if it exists before using!

  private Project project
  private logger

  private androidProcessResourcesTask, proguardTask, androidPackageTask, 
  androidInstallTask, androidUninstallTask

  boolean verbose = false

  @Override
  public void apply(Project project) {
    project.plugins.apply(JavaPlugin.class)

    this.project = project
    this.logger = project.logger

    androidConvention = new AndroidPluginConvention(project)
    project.convention.plugins.android = androidConvention

    androidSetup()
    defineTasks()
    configureEnhancements()
    configureCompile()
  }

  private void androidSetup() {
    def ant = project.ant

    PROPERTIES_FILES.each { ant.property(file: "${it}.properties") }

    // Determine the sdkDir value.
    // First, let's try the sdk.dir property in local.properties file.
    try {
      sdkDir = ant['sdk.dir']
    } catch (MissingPropertyException e) {
      sdkDir = null
    }
    if (sdkDir == null || sdkDir.length() == 0) {
      // No local.properties and/or no sdk.dir property: let's try ANDROID_HOME
      sdkDir = System.getenv("ANDROID_HOME")
      // Propagate it to the Gradle's Ant environment
      if (sdkDir != null) {
        ant.setProperty("sdk.dir", sdkDir)
      }
    }

    // Check for sdkDir correctly valued, and in case throw an error
    if (sdkDir == null || sdkDir.length() == 0) {
      throw new MissingPropertyException("Unable to find location of Android SDK. Please read documentation.")
    }

    toolsDir = new File(sdkDir, "tools")
    platformToolsDir = new File(sdkDir, "platform-tools")

    ant.path(id: 'android.antlibs') {
      ANDROID_JARS.each { pathelement(path: "${sdkDir}/tools/lib/${it}.jar") }
    }

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
    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/application/@android:hasCode",
              output: "manifest.hasCode", 'default': "true")
  }

  private void defineTasks() {
    defineAndroidProcessResourcesTask()
    defineProguardTask()
    defineAndroidPackageTask()
    defineAndroidInstallTask()
    defineAndroidUninstallTask()
    defineTaskDependencies()
    configureTaskLogging()
  }

  private void defineAndroidProcessResourcesTask() {
    androidProcessResourcesTask = project.task(ANDROID_PROCESS_RESOURCES_TASK_NAME,
        description: "Generate R.java source file from Android resource XML files", type: ProcessAndroidResources)
  }

  private void defineProguardTask() {
    proguardTask = project.task(PROGUARD_TASK_NAME,
        description: "Process classes and JARs with ProGuard", type: ProGuard)
  }

  private void defineAndroidPackageTask() {
    androidPackageTask = project.task(ANDROID_PACKAGE_TASK_NAME,
        description: "Creates the Android application apk package, optionally signed, zipaligned", type: AndroidPackageTask)
  }

  private void defineAndroidInstallTask() {
    androidInstallTask = project.task(ANDROID_INSTALL_TASK_NAME,
        description: "Installs the debug package onto a running emulator or device", type: AdbExec).doFirst {

      logger.info("Installing ${androidConvention.getApkArchivePath()} onto default emulator or device...")

      args 'install', '-r', androidConvention.apkArchivePath
    }
  }

  private void defineAndroidUninstallTask() {
    androidUninstallTask = project.task(ANDROID_UNINSTALL_TASK_NAME,
        description: "Uninstalls the application from a running emulator or device", type: AdbExec).doFirst {

      def manifestPackage = null
      try {
        manifestPackage = ant['manifest.package']
      } catch (Exception e) {
        throw new GradleException("Application package is not defined in AndroidManifest.xml, unable to uninstall.", e)
      }

      logger.info("Uninstalling ${manifestPackage} from the default emulator or device...")

      // Should uninstall fail only because the package wasn't on the device? It does now...
      args 'uninstall', manifestPackage
    }
  }

  private void defineTaskDependencies() {
    project.tasks.compileJava.dependsOn(androidProcessResourcesTask)
    proguardTask.dependsOn(project.tasks.jar)
    androidPackageTask.dependsOn(proguardTask)
    project.tasks.assemble.dependsOn(androidPackageTask)
    androidInstallTask.dependsOn(project.tasks.assemble)
  }

  private void configureTaskLogging() {
    androidProcessResourcesTask.logging.captureStandardOutput(LogLevel.INFO)
    proguardTask.logging.captureStandardOutput(LogLevel.INFO)
    androidPackageTask.logging.captureStandardOutput(LogLevel.INFO)
    androidInstallTask.logging.captureStandardOutput(LogLevel.INFO)
    androidUninstallTask.logging.captureStandardOutput(LogLevel.INFO)
  }

  /**
   * Configure enhancements to other Gradle plugins so that they work better in
   * concert with the Android plugin.
   */
  private void configureEnhancements() {
    new JavadocEnhancement(project).apply()
  }

  private void configureCompile() {
    def mainSource = project.tasks.compileJava.source
    project.tasks.compileJava.source = [androidConvention.genDir, mainSource]
    project.sourceSets.main.compileClasspath +=
    project.files(project.ant.references['android.target.classpath'].list())
    project.compileJava.options.bootClasspath = project.ant.references['android.target.classpath']
  }

}
