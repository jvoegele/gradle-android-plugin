package com.jvoegele.gradle.android.support

import org.gradle.GradleLauncher
import org.gradle.api.Project
import org.gradle.os.OperatingSystem

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

// Note that on Windows, we don't test proper cleaning of the project. If we did,
// we would get spurious test failures because somehow, there are file handles
// leaking and the 'clean' task isn't able to finish successfully. See runTasks
// and fileDoesntExist methods.

// We still use GradleLauncher here to launch a Gradle build, even if it is
// deprecated, because Tooling API doesn't yet support specifying custom
// buildscript file (which we heavily use). It will be supported in the future.

class TestProject {
  /*@Delegate*/ Project project

  def runTasks(String... tasks) {
    runTasks([:], tasks as List<String>)
  }

  def runTasks(Map<String, Object> args, String... tasks) {
    runTasks(args, tasks as List<String>)
  }

  def runTasks(Map<String, Object> args, List<String> tasks) {
    if (OperatingSystem.current().isWindows()) {
      tasks.remove 'clean'
    }

    def startParameter = project.gradle.startParameter.newBuild()
    startParameter.projectDir = project.projectDir
    if (args.buildScript) {
      startParameter.buildFile = new File(project.projectDir, args.buildScript)
    }
    startParameter.taskNames = tasks
    def launcher = GradleLauncher.newInstance(startParameter)
    launcher.addListener(new MyBuildListener())
    def result = launcher.run()
    result.rethrowFailure()
  }

  def archive(path) {
    new TestArchive(project: project, archive: file(path))
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
    if (OperatingSystem.current().isWindows()) {
      return
    }

    assertFalse("File ${path} must not exist", file(path).exists())
  }
}
