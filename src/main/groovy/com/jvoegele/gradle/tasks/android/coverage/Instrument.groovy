package com.jvoegele.gradle.tasks.android.coverage

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Instruments classes with Emma.
 *
 * @author Marcus Better
 */
class Instrument extends DefaultTask
{
    @OutputDirectory
    File destDir

    @OutputFile
    File metadataFile

    @TaskAction
    instrument()
    {
        project.ant.emma {
            instr(outdir: getDestDir(), merge: false, metadatafile: getMetadataFile(),
                instrpath: inputs.files.asPath)
        }
    }
}
