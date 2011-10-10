package com.jvoegele.gradle.plugins.android

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin

import com.jvoegele.gradle.enhancements.JavadocEnhancement
import com.jvoegele.gradle.tasks.android.AdbExec
import com.jvoegele.gradle.tasks.android.AndroidPackageTask
import com.jvoegele.gradle.tasks.android.EmulatorTask
import com.jvoegele.gradle.enhancements.EclipseEnhancement
import com.jvoegele.gradle.tasks.android.ProGuard
import com.jvoegele.gradle.tasks.android.ProcessAndroidResources
import com.jvoegele.gradle.tasks.android.instrumentation.InstrumentationTestsTask
import com.jvoegele.gradle.enhancements.ScalaEnhancement
import com.jvoegele.gradle.tasks.android.AndroidSignAndAlignTask

/**
 * Gradle plugin that extends the Java plugin for Android development.
 *
 * @author Jason Voegele (jason@jvoegele.com)
 */
class AndroidPlugin implements Plugin<Project> {

  public static final ANDROID_PROCESS_RESOURCES_TASK_NAME = "androidProcessResources"
  public static final PROGUARD_TASK_NAME = "proguard"
  public static final ANDROID_PACKAGE_TASK_NAME = "androidPackage"
  public static final ANDROID_SIGN_AND_ALIGN_TASK_NAME = "androidSignAndAlign"
  public static final ANDROID_INSTALL_TASK_NAME = "androidInstall"
  public static final ANDROID_UNINSTALL_TASK_NAME = "androidUninstall"
  public static final ANDROID_INSTRUMENTATION_TESTS_TASK_NAME = "androidInstrumentationTests"
  public static final ANDROID_START_EMULATOR_TASK_NAME = "androidEmulatorStart"
  
  private static final ANDROID_GROUP = "Android";
  
  private static final PROPERTIES_FILES = ['local', 'build', 'default']
  private static final ANDROID_JARS = ['anttasks', 'sdklib', 'androidprefs', 'apkbuilder', 'jarutils']

  private AndroidPluginConvention androidConvention
  private sdkDir
  private toolsDir
  private platformToolsDir // used since SDK r8, so check if it exists before using!

  private Project project
  private logger

  private androidProcessResourcesTask, proguardTask, androidPackageTask, androidSignAndAlignTask,
  androidInstallTask, androidUninstallTask, androidInstrumentationTestsTask, androidEmulatorStartTask

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
    // TODO: there can be several instrumentations defined
    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/instrumentation/@android:targetPackage", output: "tested.manifest.package")
    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/application/@android:hasCode",
              output: "manifest.hasCode", 'default': "true")

    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/instrumentation/@android:name", output: "android.instrumentation")
    if (ant['android.instrumentation']) {
      androidConvention.instrumentationTestsRunner = ant['android.instrumentation']
    }
  }

  private void defineTasks() {
    defineAndroidProcessResourcesTask()
    defineProguardTask()
    defineAndroidPackageTask()
    defineAndroidSignAndAlignTask()
    defineAndroidInstallTask()
    defineAndroidUninstallTask()
    defineAndroidEmulatorStartTask()
    defineAndroidInstrumentationTestsTask()
    defineTaskDependencies()
    configureTaskLogging()
  }

  private void defineAndroidProcessResourcesTask() {
    androidProcessResourcesTask = project.task(ANDROID_PROCESS_RESOURCES_TASK_NAME,
        group: ANDROID_GROUP,
        description: "Generate R.java source file from Android resource XML files",
        type: ProcessAndroidResources)
  }

  private void defineProguardTask() {
    proguardTask = project.task(PROGUARD_TASK_NAME,
        group: ANDROID_GROUP,
        description: "Process classes and JARs with ProGuard",
        type: ProGuard)
  }

  private void defineAndroidPackageTask() {
    androidPackageTask = project.task(ANDROID_PACKAGE_TASK_NAME,
        group: ANDROID_GROUP,
        description: "Creates the Android application apk package",
        type: AndroidPackageTask)
  }

  private void defineAndroidSignAndAlignTask() {
    androidSignAndAlignTask = project.task(ANDROID_SIGN_AND_ALIGN_TASK_NAME,
        group: ANDROID_GROUP,
        description: "Signs and zipaligns the application apk package",
        type: AndroidSignAndAlignTask)

    ['keyStore', 'keyAlias', 'keyStorePassword', 'keyAliasPassword'].each { String propertyName ->
      androidSignAndAlignTask.conventionMapping[propertyName] = { androidPackageTask[propertyName] }
    }
  }

  private void defineAndroidInstallTask() {
    androidInstallTask = project.task(ANDROID_INSTALL_TASK_NAME,
        group: ANDROID_GROUP,
        description: "Installs the debug package onto a running emulator or device",
        type: AdbExec) {

      doFirst {
        logger.info("Installing ${androidConvention.getApkArchivePath()} onto default emulator or device...")
        args 'install', '-r', androidConvention.apkArchivePath
      }
    }
  }

  private void defineAndroidUninstallTask() {
    androidUninstallTask = project.task(ANDROID_UNINSTALL_TASK_NAME,
        group: ANDROID_GROUP,
        description: "Uninstalls the application from a running emulator or device",
        type: AdbExec) {

      def manifestPackage = null
      try {
        manifestPackage = ant['manifest.package']
      } catch (Exception e) {
        throw new GradleException("Application package is not defined in AndroidManifest.xml, unable to uninstall.", e)
      }

      // Should uninstall fail only because the package wasn't on the device? It does now...
      args 'uninstall', manifestPackage

      doFirst {
        logger.info("Uninstalling ${manifestPackage} from the default emulator or device...")
      }
    }
  }
  
  private void defineAndroidEmulatorStartTask() {
    androidEmulatorStartTask = project.task(ANDROID_START_EMULATOR_TASK_NAME,
        description: "Starts the android emulator", type:EmulatorTask)
    androidEmulatorStartTask.group = ANDROID_GROUP
  }
  
  private void defineAndroidInstrumentationTestsTask() {
    def description = """Runs instrumentation tests on a running emulator or device.
      Use the 'runners' closure to configure your test runners:
          
         androidInstrumentationTests {
           runners {
             run testpackage: "com.my.package", with: "com.my.TestRunner"
             run annotation: "com.my.Annotation", with: "com.my.OtherRunner"
           } 
         }
          
      You can also use 'run with: "..."' to run all tests using the given runner, but
      note that this only works as long as you do not bind any other more specific runners.
    """

    androidInstrumentationTestsTask = project.task(
        ANDROID_INSTRUMENTATION_TESTS_TASK_NAME,
        group: ANDROID_GROUP,
        description: description,
        type: InstrumentationTestsTask)
  }

  private void defineTaskDependencies() {
    project.tasks.compileJava.dependsOn(androidProcessResourcesTask)
    proguardTask.dependsOn(project.tasks.jar)
    androidPackageTask.dependsOn(proguardTask)
    androidSignAndAlignTask.dependsOn(androidPackageTask)
    project.tasks.assemble.dependsOn(androidSignAndAlignTask)
    androidInstallTask.dependsOn(project.tasks.assemble)
    androidInstrumentationTestsTask.dependsOn(androidInstallTask)
  }

  private void configureTaskLogging() {
    androidProcessResourcesTask.logging.captureStandardOutput(LogLevel.INFO)
    proguardTask.logging.captureStandardOutput(LogLevel.INFO)
    androidPackageTask.logging.captureStandardOutput(LogLevel.INFO)
    androidSignAndAlignTask.logging.captureStandardOutput(LogLevel.INFO)
    androidInstallTask.logging.captureStandardOutput(LogLevel.INFO)
    androidUninstallTask.logging.captureStandardOutput(LogLevel.INFO)
  }

  /**
   * Configure enhancements to other Gradle plugins so that they work better in
   * concert with the Android plugin.
   */
  private void configureEnhancements() {
    new JavadocEnhancement(project).apply()
    new EclipseEnhancement(project).apply()
    new ScalaEnhancement(project).apply()
  }

  private void configureCompile() {
    def mainSource = project.tasks.compileJava.source
    project.tasks.compileJava.source = [androidConvention.genDir, mainSource]
    project.sourceSets.main.compileClasspath +=
    project.files(project.ant.references['android.target.classpath'].list())
    project.compileJava.options.bootClasspath = project.ant.references['android.target.classpath']
  }

}
