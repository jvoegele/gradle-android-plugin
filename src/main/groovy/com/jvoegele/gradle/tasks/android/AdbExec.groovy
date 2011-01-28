package com.jvoegele.gradle.tasks.android

import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskAction

import com.jvoegele.gradle.tasks.android.exceptions.AdbErrorException;

class AdbExec extends DefaultTask {
  def exec = new Exec()
  def stdout = new ByteArrayOutputStream() // output is small, we can safely read it into memory
  def stderr = new ByteArrayOutputStream() // adb prints error messages both on stdout and stderr

  def AdbExec() {
    exec.executable project.ant['adb']
    if (project.ant['adb.device.arg']) {
      exec.args project.ant['adb.device.arg']
    }

    // both stdout and stderr should be logged AND inspected for error messages at the same time
    exec.standardOutput = new TeeOutputStream(System.out, stdout)
    exec.errorOutput = new TeeOutputStream(System.err, stderr)

    exec.ignoreExitValue = true
  }

  @TaskAction
  def exec() {
    project.logger.info("running '${exec.commandLine.join(" ")}'")

    exec.exec()

    checkForErrors stdout
    checkForErrors stderr

    // exit value was ignored to inspect stdout/stderr first and possibly throw exception with reasonable message
    // now it's sure that the exception wasn't thrown, but the exit value really should be checked
    exec.execResult.assertNormalExitValue()
  }

  def checkForErrors(stream) {
    def reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()))
    reader.eachLine {
      if (it.toLowerCase().contains("failure") || it.toLowerCase().contains("error")) {
        throw new AdbErrorException(it.trim())
      }
    }
  }

  def AdbExec args(Object... args) {
    exec.args(args)
    return this
  }

  def AdbExec args(Iterable<?> args) {
    exec.args(args)
    return this
  }

  def AdbExec setArgs(Iterable<?> args) {
    exec.setArgs(args)
    return this
  }

  def List<String> getArgs() {
    return exec.getArgs()
  }
}
