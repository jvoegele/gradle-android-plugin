package com.jvoegele.gradle.tasks.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction


class EmulatorTask extends DefaultTask {
	
	@Input public String avdName

	def EmulatorTask() {}

	@TaskAction
	def start() {
		if (avdName == null) 
		{
			throw new IllegalStateException("""\
		Please specify avdName in your build.gradle:

			androidEmulatorStart {
				avdName = "YourAvdName"
			}	
			""")
		}
		project.logger.info("Starting emulator...")
		
		def command = project.ant['sdk.dir'] + "/tools/emulator -avd " +avdName
		def proc = command.execute()
				
		project.logger.info "Waiting for the emulator package manager..."

		//Check if package manager is found
		//@see http://stackoverflow.com/questions/6006496/testing-android-applications-on-a-clean-emulator
		
		StringBuilder output
		boolean pm_found = false		
		int counter	= 0
		
		while (!pm_found && counter < 5) 
		{						
			command = project.ant['sdk.dir'] + "/platform-tools/adb wait-for-device shell pm path android"
			proc = command.execute()
			output = new StringBuilder()
			
			proc.consumeProcessOutput(output, output)
			proc.waitForOrKill(10000)
			
			project.logger.debug "Emulator-Answer: " + output			

			if (output.toString().startsWith("package:")) {
				pm_found = true
			}
			counter++
		}
		if (pm_found) {
			project.logger.info "Emulator started."
		}
		else {
			project.logger.info "Emulator not started."
		}
	}
}