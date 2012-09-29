package com.jvoegele.gradle.tasks.android

import java.io.File;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class Lint extends DefaultTask {
    /** The lint executable */
    File lint

    @OutputFile
    @Optional
    File xmlReport

    @OutputDirectory
    @Optional
    File htmlReport

    @TaskAction
    def lint() {
        ant.exec(executable: getLint(), failonerror: true) {
            if (xmlReport) {
                arg value: '--xml'
                arg value: xmlReport
            }
            if (htmlReport) {
                arg value: '--html'
                arg value: htmlReport
            }
            arg value: project.projectDir
        }
    }
}
