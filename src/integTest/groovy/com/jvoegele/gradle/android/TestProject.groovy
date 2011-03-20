package com.jvoegele.gradle.android

import org.gradle.GradleLauncher
import org.gradle.api.PathValidation
import org.gradle.api.Project

class TestProject {
  /*@Delegate*/ Project project

  def runTasks(String... tasks) {
    runTasks(tasks.flatten())
  }

  def runTasks(List<String> tasks) {
    def startParameter = project.gradle.startParameter.newBuild()
    startParameter.projectDir = project.projectDir
    startParameter.taskNames = tasks
    def launcher = GradleLauncher.newInstance(startParameter)
    def result = launcher.run()
    result.rethrowFailure()
  }

  // delegation (sadly, Groovy's @Delegate doesn't seem to work)

  def file(Object path) {
    return project.file(path)
  }

  def file(Object path, PathValidation validation) {
    return project.file(path, validation)
  }
}
