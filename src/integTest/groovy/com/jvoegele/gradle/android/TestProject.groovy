package com.jvoegele.gradle.android

import org.gradle.GradleLauncher
import org.gradle.api.PathValidation
import org.gradle.api.Project

class TestProject {
  /*@Delegate*/ Project project

  def runTasks(Map<String, Object> args, String... tasks) {
    runTasks(args, tasks as List<String>)
  }

  def runTasks(Map<String, Object> args, List<String> tasks) {
    def startParameter = project.gradle.startParameter.newBuild()
    startParameter.projectDir = project.projectDir
    if (args.buildScript) {
      startParameter.buildFile = new File(project.projectDir, args.buildScript)
    }
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
