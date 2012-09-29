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
