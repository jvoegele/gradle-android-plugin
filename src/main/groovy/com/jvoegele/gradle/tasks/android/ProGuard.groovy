package com.jvoegele.gradle.tasks.android;

import java.io.File;

import groovy.lang.MetaClass;
import groovy.util.XmlSlurper;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention;

/**
 * Uses the ProGuard tool to create a minimal JAR containing only those classes
 * and resources actually used by the application code.
 */
class ProGuard extends ConventionTask {
  private static final String PRO_GUARD_RESOURCE = "proguard/ant/task.properties"

  String artifactGroup = "net.sf.proguard"
  String artifactName = "proguard"
  String artifactVersion = "4.4"

  boolean warn = false
  boolean note = false
  boolean obfuscate = false
  
  public ProGuard () {
    // By default, this task is disabled - it has to be explicitly enabled by user in build.gradle
    enabled = false
  }
  
  public File getTempFile() {
    AndroidPluginConvention androidConvention = project.convention.plugins.android
    return new File (project.libsDir, androidConvention.getApkBaseName() + "-proguard-temp.jar")
  }

  public File getProguardConfig() {
    return new File(project.rootDir, "proguard.cfg")
  }

  @TaskAction
  protected void process() {

    defineProGuardTask()
    String tempFilePath = getTempFile().getAbsolutePath()

    Map proguardOptions = [
      'warn': warn,
      'obfuscate': obfuscate
    ]
    if (proguardConfig.exists()) {
      proguardOptions['configuration'] = proguardConfig
    }

    ant.proguard(proguardOptions) {
      injar(path: project.jar.archivePath)

      // Add each dependency into the ProGuard-processed JAR
      project.configurations.compile.files.each { dependency ->
        injar(file: dependency)
      }
      outjar(file: tempFilePath)
      libraryjar(file: ant['android.jar'])
      keep(access: 'public', 'name': '**.R')
      keep('name': '**.R$*')
    }
                 
    // Update the output file of this task
    ant.move(file: tempFilePath, toFile: project.jar.archivePath, overwrite: true)
  }

  private boolean proGuardTaskDefined = false
  private void defineProGuardTask() {
    if (!proGuardTaskDefined) {
      project.configurations {
        proguard
      }
      project.dependencies {
        proguard group: artifactGroup, name: artifactName, version: artifactVersion
      }
      ant.taskdef(resource: PRO_GUARD_RESOURCE, classpath: project.configurations.proguard.asPath)
      proGuardTaskDefined = true
    }
  }
}
