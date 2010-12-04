package com.jvoegele.gradle.enhancements

import org.gradle.api.Project 

class JavadocEnhancement {
  private Project project
  private ant

  public JavadocEnhancement(Project project) {
    this.project = project
    this.ant = project.ant
  }

  public void apply() {
    project.tasks.javadoc.classpath += project.files(ant['android.jar'])
  }
}
