package com.jvoegele.gradle.tasks.android

import java.util.Map;

class AdbExecTask extends AndroidAntTask {

  public AdbExecTask(project) {
    super(project)
  }

  /** closure should only contain calls to args; remember that the device arg has already been set! */
  public void execute(Map args = [:], Closure closure = null) {
    def stdout = new ByteArrayOutputStream() // output is small, we can safely read it into memory
    project.exec {
      executable project.ant['adb']
      if (project.ant['adb.device.arg']) {
        args project.ant['adb.device.arg']
      }

      // the closure must run in context of this project.exec, so it must have the same delegate
      closure.delegate = delegate
      closure()

      standardOutput = stdout
    }

    def reader = new InputStreamReader(new ByteArrayInputStream(stdout.toByteArray()))
    reader.eachLine {
      if (it.toLowerCase().contains("failure") || it.toLowerCase().contains("error")) {
        throw new AdbErrorException(it.trim());
      }
    }
  }
}
