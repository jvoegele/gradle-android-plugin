package com.jvoegele.gradle.tasks.android;

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Uses the ProGuard tool to create a minimal JAR containing only those classes
 * and resources actually used by the application code.
 */
class ProGuard extends DefaultTask {
    @InputFile
    File configFile

    @InputFiles
    @Optional
    FileCollection libs

    @OutputFile
    File outJar

    /** the file where the internal structure of the class files is written */
    @OutputFile
    @Optional
    File dumpFile

    /** the list of classes and members matched by the <code>keep</code> commands */
    @OutputFile
    @Optional
    File seedsFile

    /** the list of unused code in the input files */
    @OutputFile
    @Optional
    File usageFile

    /** the mapping between original and obfuscated names */
    @OutputFile
    @Optional
    File mappingFile

    @TaskAction
    def process() {
        logger.info getLibs().asPath
        ant.proguard(configuration: getConfigFile(),
            dump: getDumpFile() ?: false, printseeds: getSeedsFile() ?: false,
            printusage: getUsageFile() ?: false, printmapping: getMappingFile() ?: false) {
            injar path: inputs.sourceFiles.asPath
            if (getLibs()) {
                libraryjar path: getLibs().asPath
            }
            outjar path: getOutJar()
        }
    }
}
