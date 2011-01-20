package com.jvoegele.gradle.tasks.android

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention 

abstract class AndroidAntTask {

  protected final project
  protected final ant
  protected final AndroidPluginConvention androidConvention

  protected AndroidAntTask(project) {
    this.project = project
    this.ant = project.ant
    this.androidConvention = project.convention.plugins.android
  }

  public abstract void execute(Map args);
}
