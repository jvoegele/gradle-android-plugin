package com.jvoegele.gradle.android

import org.gradle.GradleLauncher
import org.gradle.api.Project
import org.junit.Assert

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

  def file(path) {
    project.file(path)
  }

  // asserts

  def fileExists(path) {
    Assert.assertTrue("File ${path} must exist", file(path).isFile())
  }

  def fileDoesntExist(path) {
    Assert.assertFalse("File ${path} must not exist", file(path).exists())
  }
}
