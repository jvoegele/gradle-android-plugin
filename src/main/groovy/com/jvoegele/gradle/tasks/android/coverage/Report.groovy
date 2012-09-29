package com.jvoegele.gradle.tasks.android.coverage

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Generate an Emma coverage report.
 *
 * @author Marcus Better
 */
class Report extends DefaultTask
{
    @InputFiles
    FileCollection srcDirs

    @OutputFile
    @Optional
    File textReport

    @OutputFile
    @Optional
    File htmlReport

    @OutputFile
    @Optional
    File xmlReport

    @TaskAction
    report()
    {
        project.ant.emma {
            report(sourcepath: getSrcDirs().asPath) {
                inputs.files.addToAntBuilder(delegate, 'infileset', FileCollection.AntType.FileSet)
                if (textReport) {
                    txt outfile: textReport
                }
                if (htmlReport) {
                    html outfile: htmlReport
                }
                if (xmlReport) {
                    xml outfile: xmlReport
                }
            }
        }
    }
}
