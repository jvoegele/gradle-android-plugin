package com.jvoegele.gradle.tasks.android;

import groovy.lang.MetaClass;
import groovy.util.XmlSlurper;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

class ProGuard extends ConventionTask {
  private static final String PRO_GUARD_RESOURCE = "proguard/ant/task.properties"

  String artifactGroup = "net.sf.proguard"
  String artifactName = "proguard"
  String artifactVersion = "4.4"

  boolean enabled = true
  boolean warn = false
  boolean note = false
  boolean obfuscate = false
  
  File outJar = new File(project.libsDir, "classes.min.jar")

  @TaskAction
  protected void process() {
    if (!enabled) return

    defineProGuardTask()
    ant.proguard('warn': warn, 'obfuscate': obfuscate,
                 'allowaccessmodification': true, 'overloadaggressively': true) {
      injar(file: project.sourceSets.main.classesDir)
      project.configurations.compile.files.each { dependency ->
        injar(file: dependency)
      }
      outjar(file: outJar)
      libraryjar(file: ant['android.jar'])
      optimizations(filter: "!code/simplification/arithmetic")
      keep(access: 'public', 'extends': 'android.app.Activity')
      keep(access: 'public', 'extends': 'android.app.Service')
      keep(access: 'public', 'extends': 'android.content.BroadcastReceiver')
      keep(access: 'public', 'extends': 'android.content.ContentProvider')
      keep(access: 'public', 'name': '**.R')
    }
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
