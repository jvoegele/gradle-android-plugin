package com.jvoegele.gradle.plugins.android

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin 
import org.gradle.api.plugins.ProjectPluginsContainer;

import com.jvoegele.gradle.tasks.android.Dex 
import com.jvoegele.gradle.tasks.android.ProGuard
import com.jvoegele.gradle.tasks.android.ProcessAndroidResources

/**
 * Gradle plugin that extends the Java plugin for Android development.
 *
 * @author Jason Voegele (jason@jvoegele.com)
 */

/*
android-plugin tasks
--------------------
generateSources
    Generates the R.java source file used to access resources
    dependants: compileJava

proguard
    Processes classes and JARs with ProGuard
    dependsOn: classes

androidPackageDebug
    Creates the Android application apk package, signed with debug key
    dependsOn: classes
    dependants: assemble

androidPackageRelease
    Creates the Android application apk package, which must be signed before it is published
    dependsOn: classes

androidInstall
    Installs the debug package onto a running emulator or device
    dependsOn: androidDebugPackage

androidUninstall
    Uninstalls the application from a running emulator or device

Should proguard task be exposed?  I think so, since that will allow it to be configured or disabled.
*/
class AndroidPlugin implements Plugin {
  private static final String GENERATE_SOURCES_TASK_NAME = "generateSources"
  private static final String PROGUARD_TASK_NAME = "proguard"
  private static final String ANDROID_PACKAGE_DEBUG_TASK_NAME = "androidPackageDebug"
  private static final String ANDROID_PACKAGE_RELEASE_TASK_NAME = "androidPackageRelease"
  private static final String ANDROID_INSTALL_TASK_NAME = "androidInstall"
  private static final String ANDROID_UNINSTALL_TASK_NAME = "androidUninstall"

  private static final PROPERTIES_FILES = ['local', 'build', 'default']
  private static final ANDROID_JARS = ['anttasks', 'sdklib', 'androidprefs', 'apkbuilder', 'jarutils']

  private androidConvention
  private sdkDir
  private toolsDir

  boolean verbose = false

  @Override
  public void use(Project project, ProjectPluginsContainer plugins) {
    JavaPlugin javaPlugin = plugins.usePlugin(JavaPlugin.class, project);

    //project.configurations.add(ANDROID_CONFIGURATION_NAME).setVisible(false).setTransitive(true);
    androidConvention = new AndroidPluginConvention(project)
    project.convention.plugins.android = androidConvention

    androidSetup(project)
    defineTasks(project)
    configureCompile(project)
  }

  private void androidSetup(Project project) {
    def ant = project.ant

    PROPERTIES_FILES.each { ant.property(file: "${it}.properties") }
    sdkDir = project['sdk.dir']
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

    ant.taskdef(name: "aaptexec", classname: "com.android.ant.AaptExecLoopTask", classpathref: "android.antlibs")
    ant.taskdef(name: "apkbuilder", classname: "com.android.ant.ApkBuilderTask", classpathref: "android.antlibs")
  }

  private void defineTasks(Project project) {
    ProcessAndroidResources processAndroidResourcesTask = project.tasks.add(GENERATE_SOURCES_TASK_NAME, ProcessAndroidResources.class)
    processAndroidResourcesTask.description = "Generate R.java source file from Android resource XML files"
    project.tasks.compileJava.dependsOn(processAndroidResourcesTask)

    ProGuard proguardTask = project.tasks.add(PROGUARD_TASK_NAME, ProGuard.class)
    proguardTask.description = "Process classes and JARs with ProGuard"
    proguardTask.dependsOn('classes')

    // Since there is no configuration for this task, just define it inline.
    project.task(ANDROID_PACKAGE_DEBUG_TASK_NAME) << {

      ant.apply(executable: ant.dx, failonerror: true, parallel: true) {
        arg(value: "--dex")
        arg(value: "--output=${androidConvention.intermediateDexFile}")
        if (verbose) arg(line: "--verbose")
        arg(path: project.sourceSets.main.classesDir)
        fileset(dir: project.buildDir, includes: "*.jar")
      }

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
                     signed: true,
                     'verbose': true /*FIXME*/) {
        ant.file(path: androidConvention.intermediateDexFile.absolutePath)
        //sourcefolder(path: project.sourceSets.main.java)
        nativefolder(path: androidConvention.nativeLibsDir)
        //jarfolder(path: androidConvention.nativeLibsDir)
      }

      ant.exec(executable: ant.zipalign, failonerror: true) {
        // FIXME: use some global verbosity setting and add this if set: arg(line="${v.option}" />
        arg(value: '-f')
        arg(value: 4)
        arg(path: new File(project.buildDir, "${project.name}-debug-unaligned.apk"))
        arg(path: new File(project.buildDir, "${project.name}-debug.apk"))
      }
    }
    project.tasks[ANDROID_PACKAGE_DEBUG_TASK_NAME].dependsOn(proguardTask)

    project.task(ANDROID_INSTALL_TASK_NAME) << {
      ant.exec(executable: ant['adb'], failonerror: true) {
        arg(line: ant['adb.device.arg'])
        arg(value: 'install')
        arg(value: '-r')
        arg(path: ant['out.debug.package'])
      }
    }
    project.tasks[ANDROID_INSTALL_TASK_NAME].dependsOn(ANDROID_PACKAGE_DEBUG_TASK_NAME)
    project.tasks.assemble.dependsOn(ANDROID_PACKAGE_DEBUG_TASK_NAME)
  }

  private void configureCompile(Project project) {
    def mainSource = project.tasks.compileJava.source
    project.tasks.compileJava.source = [androidConvention.genDir, mainSource]
    project.sourceSets.main.compileClasspath += project.files(project.ant.references['android.target.classpath'].list())
    project.compileJava.options.bootClasspath = project.ant.references['android.target.classpath']
  }
}
