package com.jvoegele.gradle.enhancements

import org.gradle.api.Project
import org.gradle.api.plugins.scala.ScalaPlugin

class ScalaEnhancement extends GradlePluginEnhancement {
  ScalaEnhancement(Project project) {
    super(project)
  }

  def apply() {
    project.gradle.projectsEvaluated {
      if (project.plugins.hasPlugin(ScalaPlugin)) {
        project.logger.info("Compiling Scala project, enabling Proguard (dexing fails without it)")
        project.proguard.enabled = true
      }
    }
  }
}
