package com.jvoegele.gradle.tasks.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class Dex extends DefaultTask
{
    protected ant = project.ant

    @Input
    @Optional
    Boolean noLocals

    @OutputFile
    File dexFile

    boolean verbose

    @TaskAction
    def dex()
    {
        ant.dex(executable: ant.dx, output: getDexFile(), noLocals: getNoLocals(), verbose: getVerbose()) {
            path path: inputs.sourceFiles.asPath
        }
    }
}
