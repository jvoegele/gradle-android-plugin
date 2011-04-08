package com.jvoegele.gradle.android

import org.gradle.GradleLauncher
import org.gradle.api.Project

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

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

  def exec(closure) {
    project.exec(closure)
  }

  // asserts

  def fileExists(path) {
    assertTrue("File ${path} must exist", file(path).isFile())
  }

  def fileDoesntExist(path) {
    assertFalse("File ${path} must not exist", file(path).exists())
  }
}
