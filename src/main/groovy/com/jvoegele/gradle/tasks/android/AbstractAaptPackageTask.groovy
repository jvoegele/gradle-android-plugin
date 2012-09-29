package com.jvoegele.gradle.tasks.android

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional

class AbstractAaptPackageTask extends DefaultTask
{
    static final LIB_RESOURCES_REF = this.class.name + 'lib.resources'
    static final LIB_PACKAGES_REF = this.class.name + 'lib.packages'

    /** the manifest file (usually AndroidManifest.xml) */
    @InputFile
    File manifest

    /** the resources directory */
    @InputDirectory
    File resourceDir

    /** the resource directories of library sub-projects */
    @InputFiles
    @Optional
    FileCollection libResourceDirs

    /** list package names of library sub-projects, separated with ';' */
    @Input
    @Optional
    String libPackageNames

    boolean verbose

    protected ant = project.ant

    protected callAapt(Map params, Closure c = null)
    {
        ant.references[LIB_RESOURCES_REF] = ant.path {
            libResourceDirs.each {
                pathelement location: it
            }
        }
        if (libPackageNames) {
            ant.references[LIB_PACKAGES_REF] = libPackageNames
        }
        def commonParams = [executable: ant.properties.aapt, command: 'package', verbose: getVerbose(),
            manifest: getManifest(), androidjar: ant['android.jar'],
            projectLibrariesResName: LIB_RESOURCES_REF, projectLibrariesPackageName: LIB_PACKAGES_REF]
        ant.aapt(commonParams + params) {
            res path: getResourceDir()
            if (c) {
                c.delegate = delegate
                c.call()
            }
        }
    }
}
