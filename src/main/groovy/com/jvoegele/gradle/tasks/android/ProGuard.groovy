package com.jvoegele.gradle.tasks.android;

import groovy.lang.MetaClass;

import org.gradle.api.internal.ConventionTask;

class ProGuard extends ConventionTask {
  private static final String PRO_GUARD_RESOURCE = "proguard/ant/task.properties"

  String artifactCoordinates = "net.sf.proguard:proguard:4.4"

  String rootPackage

  private String keep
  public String getKeep() {
    if (keep == null) {
      if (rootPackage == null) return null
      return "class ${rootPackage}.*"
    }
    return keep
  }

  public void setKeep(String value) {
    keep = value
  }

  private boolean proGuardTaskDefined = false
  private void defineProGuardTask() {
    if (!proGuardTaskDefined) {
      project.configurations {
        proGuard
      }
      project.dependencies {
        proGuard this.artifactCoordinates
      }
      ant.taskdef(resource: PRO_GUARD_RESOURCE, classpath: configurations.proguard.asPath)
      proGuardTaskDefined = true
    }
  }
}
