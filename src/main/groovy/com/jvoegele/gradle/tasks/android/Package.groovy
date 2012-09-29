package com.jvoegele.gradle.tasks.android

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class Package extends DefaultTask
{
    @InputFile
    File resourcePkg

    /** the compressed classes (dex) file */
    @InputFile
    File dexFile

    // whether to build in debug mode
    @Input
    boolean debug = false

    @InputFiles
    @Optional
    FileCollection sourceFolders

    @InputFiles
    @Optional
    FileCollection jarFolders

    @InputFiles
    @Optional
    FileCollection jarFiles

    @InputFiles
    @Optional
    FileCollection nativeFolders

    boolean verbose

    /** the directory where the archive is generated into */
    @Input
    File destinationDir

    /** the base name of the archive */
    @Input
    String baseName

    /** the appendix part of the archive name */
    @Input
    @Optional
    String appendix

    /** the version part of the archive name */
    @Input
    @Optional
    String version

    /** the extension part of the archive name */
    @Input
    String extension = 'apk'

    /** the classifier part of the archive name */
    @Input
    String classifier = ''

    @InputFile
    @Optional
    File keyStore

    @Input
    @Optional
    String keyAlias

    String keyStorePassword

    String keyPassword

    protected String customName

    protected ant = project.ant

    /**
     * Returns the archive name. If the name has not been explicitly set, the pattern for the name is:
     * [baseName]-[appendix]-[version]-[classifier].[extension]
     *
     * @return the archive name
     */
    String getArchiveName()
    {
        if (customName) {
            return customName
        }
        String name = (getBaseName() ?: '') + maybe(getBaseName(), getAppendix())
        name += maybe(name, getVersion())
        name += maybe(name, getClassifier())
        name += getExtension() ? '.' + getExtension() : ''
    }

    /**
     * Sets the archive name.
     *
     * @param name the archive name
     */
    void setArchiveName(String name)
    {
        customName = name
    }
    
    /**
     * The path where the archive is constructed. The path is simply the destinationDir plus the archiveName.
     *
     * @return a File object with the path to the archive
     */
    @OutputFile
    File getArchivePath()
    {
        new File(getDestinationDir(), archiveName)
    }

    @TaskAction
    def doPackage()
    {
        ant.apkbuilder(outfolder: getResourcePkg().parentFile.absolutePath, resourcefile: getResourcePkg().name,
            apkfilepath: archivePath, debugpackaging: getDebug(), debugsigning: getDebug(),
            verbose: getVerbose(), hascode: ant['manifest.hasCode']) {
            dex path: getDexFile()
            if (getSourceFolders()) {
                sourcefolder path: getSourceFolders().asPath
            }
            if (getJarFolders()) {
                jarfolder path: getJarFolders().asPath
            }
            if (getJarFiles()) {
                jarfile path: getJarFiles().asPath
            }
            if (getNativeFolders()) {
                nativefolder path: getNativeFolders().asPath
            }
        }

        def unalignedArchive = archivePath
        if (!getDebug()) {
            if (getKeyStore() && getKeyAlias()) {
                if (!getKeyStorePassword()) {
                    keyStorePassword = new String(System.console().readPassword("Please enter keystore password for ${getKeyStore().path}"))
                }
                if (!getKeyPassword()) {
                    keyPassword = new String(System.console().readPassword("Please enter password for key ${getKeyAlias()}"))
                }
                unalignedArchive = new File(temporaryDir, archiveName + '-unaligned')
                ant.signjar(jar: archivePath, signedjar: unalignedArchive, keystore: getKeyStore(),
                    storepass: getKeyStorePassword(), alias: getKeyAlias(), keypass: getKeyPassword(), verbose: getVerbose())
            } else {
                logger.info "No release signing key was provided. Please sign ${unalignedArchive.absolutePath} manually and run zipalign on it."
            }
        }
        def alignedArchive = new File(temporaryDir, archiveName)
        zipAlign unalignedArchive, alignedArchive
        ant.move file: alignedArchive, tofile: archivePath
    }

    protected zipAlign(inPackage, outPackage)
    {
        ant.zipalign(executable: ant.properties.zipalign, input: inPackage, output: outPackage,
            verbose: getVerbose())
    }

    protected String maybe(String prefix, String value)
    {
        value ? (prefix ? "-$value" : value) : ''
    }
}
