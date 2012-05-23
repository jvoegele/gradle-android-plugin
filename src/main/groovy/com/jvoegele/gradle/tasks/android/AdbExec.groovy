/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      exec.args project.ant['adb.device.arg'].split(" ")
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
