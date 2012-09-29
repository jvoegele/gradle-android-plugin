package com.jvoegele.gradle.tasks.android

import org.gradle.api.tasks.TaskAction

class Test extends AbstractAdbTask
{
    /** the application package name */
    String appPackage

    /** the test runner class used to run the tests */
    String testRunnerClass = 'android.test.InstrumentationTestRunner'

    Map params = [:]

    @TaskAction
    def copy()
    {
        invokeAdb {
            arg value: 'shell'
            arg value: 'am'
            arg value: 'instrument'
            arg value: '-w'
            getParams().each { k, v ->
                arg value: '-e'
                arg value: k
                arg value: v
            }
            arg value: "$appPackage/$testRunnerClass"
        }
    }
}
