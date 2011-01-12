package com.jvoegele.gradle.enhancements

import org.gradle.api.Project;

class GradlePluginEnhancement {
  protected Project project
  protected ant

  public GradlePluginEnhancement(Project project) {
    this.project = project
    this.ant = project.ant
  }
}
