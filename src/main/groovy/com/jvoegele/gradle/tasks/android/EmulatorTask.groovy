package com.jvoegele.gradle.tasks.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction
import org.gradle.api.internal.ConventionTask

class EmulatorTask extends DefaultTask {
	
	@Input public String avdName
	private sdkDir

	def EmulatorTask() {
		sdkDir = System.getenv("ANDROID_HOME")
	}

	@TaskAction
	def start() {
		project.logger.info("Starting emulator...")
		def command = sdkDir + "/tools/emulator -avd " +avdName
		def proc = command.execute()
	}
}
