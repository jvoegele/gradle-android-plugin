package com.jvoegele.gradle.tasks.android

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class Uninstall extends AbstractAdbTask
{
    /** the application package name */
    @Input
    String appPackage

    @TaskAction
    def uninstall()
    {
        logger.info "uninstalling ${getAppPackage()}"
        invokeAdb {
            arg value: 'uninstall'
            arg value: getAppPackage()
        }
    }
}
