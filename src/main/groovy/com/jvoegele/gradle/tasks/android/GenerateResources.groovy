package com.jvoegele.gradle.tasks.android

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenerateResources extends AbstractAaptPackageTask
{
    @Input
    Boolean nonConstantId

    /** the output directory */
    @OutputDirectory
    File genDir

    @TaskAction
    def generate()
    {
        callAapt(rfolder: getGenDir(), nonConstantId: getNonConstantId())
    }
}
