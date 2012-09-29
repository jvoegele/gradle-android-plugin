package com.jvoegele.gradle.tasks.android

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class Pull extends AbstractAdbTask
{
    /** the absolute path of the source file on the target device */
    String src

    /** the destination file */
    @OutputFile
    File dest

    @TaskAction
    def copy()
    {
        logger.info "copying $src from device to $dest.absolutePath"
        invokeAdb {
            arg value: 'pull'
            arg value: src
            arg path: dest
        }
    }
}
