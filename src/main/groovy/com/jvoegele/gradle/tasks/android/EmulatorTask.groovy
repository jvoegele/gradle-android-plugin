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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction

class EmulatorTask extends DefaultTask {

  @Input public String avdName

  def EmulatorTask() {}

  @TaskAction
  def start() {
    if (avdName == null) {
      throw new IllegalStateException("""
          Please specify avdName in your build.gradle:

          androidEmulatorStart {
            avdName = "YourAvdName"
          }
        """)
    }

    project.logger.info("Starting emulator...")
    def command = project.ant['sdk.dir'] + "/tools/emulator -avd " +avdName
    def proc = command.execute()
  }
}
