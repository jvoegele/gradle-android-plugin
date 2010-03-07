package com.jvoegele.gradle.plugins.android

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin 
import org.gradle.api.plugins.ProjectPluginsContainer;

import com.jvoegele.gradle.tasks.android.Dex 
import com.jvoegele.gradle.tasks.android.ProcessAndroidResources;

/**
 * Gradle plugin that extends the Java plugin for Android development.
 *
 * @author Jason Voegele (jason@jvoegele.com)
 */
class AndroidPlugin implements Plugin {
  private static final String ANDROID_CONFIGURATION_NAME = "android"
  private static final String PROCESS_ANDROID_RESOURCES_TASK_NAME = "processAndroidResources"
  private static final String DEX_TASK_NAME = "dex"
  private static final PROPERTIES_FILES = ['local', 'build', 'default']
  private static final ANDROID_JARS = ['anttasks', 'sdklib', 'androidprefs', 'apkbuilder', 'jarutils']

  private androidConvention

  @Override
  public void use(Project project, ProjectPluginsContainer plugins) {
    JavaPlugin javaPlugin = plugins.usePlugin(JavaPlugin.class, project);

    project.configurations.add(ANDROID_CONFIGURATION_NAME).setVisible(false).setTransitive(true);
    androidConvention = new AndroidPluginConvention(project)
    project.convention.plugins.android = androidConvention

    androidSetup(project)
    defineTasks(project)
    configureCompile(project)
  }

  private void androidSetup(Project project) {
    def ant = project.ant

    PROPERTIES_FILES.each { ant.property(file: "${it}.properties") }
    def sdkDir = project['sdk.dir']

    ant.path(id: 'android.antlibs') {
      ANDROID_JARS.each { pathelement(path: "${sdkDir}/tools/lib/${it}.jar") }
    }

    ant.taskdef(name: 'setup', classname: 'com.android.ant.SetupTask', classpathref: 'android.antlibs')

    // The following properties are put in place by the setup task:
    // android.jar, android.aidl, aapt, aidl, and dx
    ant.setup('import': false)
  }

  private void defineTasks(Project project) {
    ProcessAndroidResources task = project.tasks.add(PROCESS_ANDROID_RESOURCES_TASK_NAME, ProcessAndroidResources.class)
    task.description = "Generate R.java source file from Android resource XML files"
    project.tasks.compileJava.dependsOn(PROCESS_ANDROID_RESOURCES_TASK_NAME)

    Dex dexTask = project.tasks.add(DEX_TASK_NAME, Dex.class)
    dexTask.description = "Convert Java class files into dex format"
    project.tasks.jar.dependsOn(DEX_TASK_NAME)
  }

  private void configureCompile(Project project) {
    def mainSource = project.tasks.compileJava.source
    project.tasks.compileJava.source = [androidConvention.genDir, mainSource]
    project.sourceSets.main.compileClasspath += project.files(project.ant.references['android.target.classpath'].list())
    project.compileJava.options.bootClasspath = project.ant.references['android.target.classpath']
  }
}
