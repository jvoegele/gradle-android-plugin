package com.jvoegele.gradle.tasks.android

import org.gradle.api.tasks.TaskAction

class Install extends AbstractAdbTask
{
    /** the application package */
    File app

    @TaskAction
    def install()
    {
        logger.info "installing ${getApp().name} onto target device"
        invokeAdb {
            arg value: 'install'
            arg value: '-r'
            arg path: getApp()
        }
    }
}
