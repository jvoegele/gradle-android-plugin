package com.jvoegele.gradle.tasks.android

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class PackageResources extends AbstractAaptPackageTask
{
    @InputDirectory
    @Optional
    File assetsDir

    @Input
    String resourceFilter = ''

    @Input
    Boolean noCrunch = false

    @Input
    Integer versionCode

    @Input
    String versionName

    @Input
    Boolean debug = false

    @OutputFile
    File resourcePkg

    @TaskAction
    def packageResources()
    {
        callAapt(versionCode: getVersionCode(), versionName: getVersionName(), debug: getDebug(),
            nocrunch: getNoCrunch(), resourcefilter: getResourceFilter(), assets: getAssetsDir(),
            apkFolder: getResourcePkg().parentFile.absolutePath, resourcefilename: getResourcePkg().name)
    }
}
