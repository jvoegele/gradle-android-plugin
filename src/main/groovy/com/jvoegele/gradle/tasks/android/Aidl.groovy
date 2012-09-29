package com.jvoegele.gradle.tasks.android

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class Aidl extends DefaultTask
{
    /** the output directory */
    @OutputDirectory
    File genDir

    /** the resource directories of library sub-projects */
    @InputFiles
    FileCollection srcDirs

    protected ant = project.ant

    @TaskAction
    def aidl()
    {
        ant.aidl(executable: ant.properties.aidl, framework: ant['android.aidl'], genFolder: getGenDir()) {
            source path: getSrcDirs().asPath
        }
    }
}
