package com.jvoegele.gradle.enhancements

import org.gradle.api.Project 

class JavadocEnhancement extends GradlePluginEnhancement {
  public JavadocEnhancement(Project project) {
    super(project)
  }

  public void apply() {
    project.tasks.javadoc.classpath += project.files(ant['android.jar'])
  }
}
