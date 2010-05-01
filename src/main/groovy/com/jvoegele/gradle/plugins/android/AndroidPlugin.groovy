package com.jvoegele.gradle.plugins.android

import org.gradle.api.logging.LogLevel;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin

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
  private static final ANDROID_PACKAGE_DEBUG_TASK_NAME = "androidPackageDebug"
  private static final ANDROID_PACKAGE_RELEASE_TASK_NAME = "androidPackageRelease"
  private static final ANDROID_INSTALL_TASK_NAME = "androidInstall"
  private static final ANDROID_UNINSTALL_TASK_NAME = "androidUninstall"

  private static final PROPERTIES_FILES = ['local', 'build', 'default']
  private static final ANDROID_JARS = ['anttasks', 'sdklib', 'androidprefs', 'apkbuilder', 'jarutils']

  private androidConvention
  private sdkDir
  private toolsDir

  private Project project
  private logger

  private androidProcessResourcesTask, proguardTask, androidPackageDebugTask, androidPackageReleaseTask,
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
    configureCompile()
  }

  private void androidSetup() {
    def ant = project.ant

    PROPERTIES_FILES.each { ant.property(file: "${it}.properties") }
    sdkDir = ant['sdk.dir']
    toolsDir = new File(sdkDir, "tools")

    ant.path(id: 'android.antlibs') {
      ANDROID_JARS.each { pathelement(path: "${sdkDir}/tools/lib/${it}.jar") }
    }

    ant.condition('property': "exe", value: ".exe", 'else': "") { os(family: "windows") }
    ant.property(name: "adb", location: new File(toolsDir, "adb${ant['exe']}"))
    ant.property(name: "zipalign", location: new File(toolsDir, "zipalign${ant['exe']}"))
    ant.property(name: 'adb.device.arg', value: '')

    def outDir = project.buildDir.absolutePath
    ant.property(name: "out.debug.unaligned.package", location: "${outDir}/${project.name}-debug-unaligned.apk")
    ant.property(name: "out.debug.package", location: "${outDir}/${project.name}-debug.apk")
    ant.property(name: "out.unsigned.package", location: "${outDir}/${project.name}-unsigned.apk")
    ant.property(name: "out.unaligned.package", location: "${outDir}/${project.name}-unaligned.apk")
    ant.property(name: "out.release.package", location: "${outDir}/${project.name}-release.apk")

    ant.taskdef(name: 'setup', classname: 'com.android.ant.SetupTask', classpathref: 'android.antlibs')

    // The following properties are put in place by the setup task:
    // android.jar, android.aidl, aapt, aidl, and dx
    ant.setup('import': false)

    ant.taskdef(name: "xpath", classname: "com.android.ant.XPathTask", classpathref: "android.antlibs")
    ant.taskdef(name: "aaptexec", classname: "com.android.ant.AaptExecLoopTask", classpathref: "android.antlibs")
    ant.taskdef(name: "apkbuilder", classname: "com.android.ant.ApkBuilderTask", classpathref: "android.antlibs")

    ant.xpath(input: androidConvention.androidManifest, expression: "/manifest/@package", output: "manifest.package")
  }

  private void defineTasks() {
    defineAndroidProcessResourcesTask()
    defineProguardTask()
    defineAndroidPackageDebugTask()
    defineAndroidPackageReleaseTask()
    defineAndroidInstallTask()
    defineAndroidUninstallTask()
    defineTaskDependencies()
    configureTaskLogging()
  }

  private void defineAndroidProcessResourcesTask() {
    androidProcessResourcesTask = project.tasks.add(ANDROID_PROCESS_RESOURCES_TASK_NAME, ProcessAndroidResources.class)
    androidProcessResourcesTask.description = "Generate R.java source file from Android resource XML files"
  }

  private void defineProguardTask() {
    proguardTask = project.tasks.add(PROGUARD_TASK_NAME, ProGuard.class)
    proguardTask.description = "Process classes and JARs with ProGuard"
  }

  private void defineAndroidPackageDebugTask() {
    androidPackageDebugTask = project.task(ANDROID_PACKAGE_DEBUG_TASK_NAME) << {
      androidPackage(ant, true)
      zipAlign(ant, ant['out.debug.unaligned.package'], ant['out.debug.package'])
      logger.info("Debug Package: " + ant['out.debug.package'])
    }
    androidPackageDebugTask.description =
        "Creates the Android application apk package, signed with debug key"
  }

  private void defineAndroidPackageReleaseTask() {
    androidPackageReleaseTask = project.task(ANDROID_PACKAGE_RELEASE_TASK_NAME) << {
      androidPackage(ant, false)

      String keyStore, keyAlias
      try {
        keyStore = ant['key.store']
        keyAlias = ant['key.alias']
      } catch (Exception ignoreBecauseWeCheckForNullLaterAnywayAfterAll) {}

      if (!keyStore || !keyAlias) {
        logger.warn("No key.store and key.alias properties found in build.properties.")
        logger.warn("Please sign ${ant['out.unsigned.package']} manually")
        logger.warn("and run zipalign from the Android SDK tools.")
      }
      else {
        def console = System.console()
        String keyStorePassword = new String(console.readPassword(
            "Please enter keystore password (store:${keyStore}): "))
        String keyAliasPassword = new String(console.readPassword(
            "Please enter password for alias '${keyAlias}': "))

        logger.info("Signing final apk...")
        ant.signjar(jar: ant['out.unsigned.package'],
                    signedjar: ant['out.unaligned.package'],
                    keystore: keyStore,
                    storepass: keyStorePassword,
                    alias: keyAlias,
                    keypass: keyAliasPassword,
                    verbose: true)

        zipAlign(ant, ant['out.unaligned.package'], ant['out.release.package'])
        logger.info("Release Package: " + ant['out.release.package'])
      }
    }
    androidPackageReleaseTask.description =
        "Creates the Android application apk package, which must be signed before it is published"
  }

  private void defineAndroidInstallTask() {
    androidInstallTask = project.task(ANDROID_INSTALL_TASK_NAME) << {
      logger.info("Installing ${ant['out.debug.package']} onto default emulator or device...")
      ant.exec(executable: ant['adb'], failonerror: true) {
        arg(line: ant['adb.device.arg'])
        arg(value: 'install')
        arg(value: '-r')
        arg(path: ant['out.debug.package'])
      }
    }
    androidInstallTask.description =
        "Installs the debug package onto a running emulator or device"
  }

  private void defineAndroidUninstallTask() {
    androidUninstallTask = project.task(ANDROID_UNINSTALL_TASK_NAME) << {
      String manifestPackage = null
      try {
        manifestPackage = ant['manifest.package']
      } catch (Exception ignoreBecauseWeCheckForNullLaterAnywayAfterAll) {}
      if (!manifestPackage) {
        logger.error("Unable to uninstall, manifest.package property is not defined.")
      }
      else {
        logger.info("Uninstalling ${ant['manifest.package']} from the default emulator or device...")
        ant.exec(executable: ant['adb'], failonerror: true) {
          arg(line: ant['adb.device.arg'])
          arg(value: "uninstall")
          arg(value: ant['manifest.package'])
        }
      }
    }
    androidUninstallTask.description =
        "Uninstalls the application from a running emulator or device"
  }

  private void defineTaskDependencies() {
    project.tasks.compileJava.dependsOn(androidProcessResourcesTask)
    proguardTask.dependsOn('classes')
    androidPackageDebugTask.dependsOn(proguardTask)
    androidPackageReleaseTask.dependsOn(proguardTask)
    project.tasks.assemble.dependsOn(ANDROID_PACKAGE_DEBUG_TASK_NAME)
    androidInstallTask.dependsOn(ANDROID_PACKAGE_DEBUG_TASK_NAME)
  }

  private void configureTaskLogging() {
    androidProcessResourcesTask.captureStandardOutput(LogLevel.INFO)
    proguardTask.captureStandardOutput(LogLevel.INFO)
    androidPackageDebugTask.captureStandardOutput(LogLevel.INFO)
    androidPackageReleaseTask.captureStandardOutput(LogLevel.INFO)
    androidInstallTask.captureStandardOutput(LogLevel.INFO)
    androidUninstallTask.captureStandardOutput(LogLevel.INFO)
  }

  private void configureCompile() {
    def mainSource = project.tasks.compileJava.source
    project.tasks.compileJava.source = [androidConvention.genDir, mainSource]
    project.sourceSets.main.compileClasspath +=
        project.files(project.ant.references['android.target.classpath'].list())
    project.compileJava.options.bootClasspath = project.ant.references['android.target.classpath']
  }

  private void androidPackage(ant, boolean sign) {
    logger.info("Converting compiled files and external libraries into ${androidConvention.intermediateDexFile}...")
    ant.apply(executable: ant.dx, failonerror: true, parallel: true) {
      arg(value: "--dex")
      arg(value: "--output=${androidConvention.intermediateDexFile}")
      if (verbose) arg(line: "--verbose")
      //arg(path: project.sourceSets.main.classesDir)
      fileset(file: proguardTask.outJar)
    }

    logger.info("Packaging resources")
    ant.aaptexec(executable: ant.aapt,
                 command: 'package',
                 manifest: androidConvention.androidManifest.path,
                 resources: androidConvention.resDir.path,
                 assets: androidConvention.assetsDir,
                 androidjar: ant['android.jar'],
                 outfolder: project.buildDir,
                 basename: project.name)

    ant.apkbuilder(outfolder: project.buildDir,
        basename: project.name,
        signed: sign,
        'verbose': verbose) {
          ant.file(path: androidConvention.intermediateDexFile.absolutePath)
          //sourcefolder(path: project.sourceSets.main.java)
          nativefolder(path: androidConvention.nativeLibsDir)
          //jarfolder(path: androidConvention.nativeLibsDir)
        }
  }

  private void zipAlign(ant, inPackage, outPackage) {
    logger.info("Running zip align on final apk...")
    ant.exec(executable: ant.zipalign, failonerror: true) {
      if (verbose) arg(line: '-v')
      arg(value: '-f')
      arg(value: 4)
      arg(path: inPackage)
      arg(path: outPackage)
    }
  }
}
