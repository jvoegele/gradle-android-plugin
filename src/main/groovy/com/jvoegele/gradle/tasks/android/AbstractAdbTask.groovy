package com.jvoegele.gradle.tasks.android

import org.gradle.api.DefaultTask

class AbstractAdbTask extends DefaultTask
{
    /** whether to run against the only connected USB device */
    boolean useUsbDevice

    /** whether to run against the only connected emulator */
    boolean useEmulator

    /** the target device serial number */
    String targetDevice

    /** the adb executable */
    File adb

    protected ant = project.ant

    protected invokeAdb(Closure c)
    {
        ant.exec(executable: getAdb(), failonerror: true) {
            if (getTargetDevice()) {
                arg line: '-s' + getTargetDevice()
            } else if (getUseUsbDevice()) {
                arg value: '-d'
            } else if (getUseEmulator()) {
                arg value: '-e'
            }
            c.delegate = delegate
            c.call()
        }
    }
}
