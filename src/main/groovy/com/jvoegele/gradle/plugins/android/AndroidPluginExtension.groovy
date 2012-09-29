package com.jvoegele.gradle.plugins.android

import org.gradle.api.Project

class AndroidPluginExtension {
    File resDir
    File genDir
    File assetsDir
    File nativeLibsDir
    File manifest
    File dexFile
    File instrumentedDexFile
    File obfuscatedDexFile
    File resourcePkg
    String instrumentationTestsRunner
    String targetDevice
    boolean library = false
    int versionCode = 0
    File coverageReportsDir
    File coverageMetadataFile
    File coverageHtmlReport
    File coverageXmlReport
    File lintReportsDir
    File lintHtmlReport
    File lintXmlReport

    File keyStore
    String keyStorePassword
    String keyAlias
    String keyPassword

    File proguardConfigFile
    File proguardDumpFile
    File proguardSeedsFile
    File proguardUsageFile
    File proguardMappingFile
    File proguardOutJar

    AndroidPluginExtension(Project project) {
        resDir = new File(project.projectDir, 'res')
        assetsDir = new File(project.projectDir, 'assets')
        nativeLibsDir = new File(project.projectDir, 'libs')
        manifest = new File(project.projectDir, 'AndroidManifest.xml')
    // FIXME (Matthias): I find this misleading, this is NOT conventional; the gen/ folder
    // typically sits at the project root, not inside the build/ folder, that's a Gradle thing.
    // In fact, for the EclipseEnhancement to work, I had to hack around this by removing this
    // entry and replacing it with $projectDir/gen, which is the actual convention.
        genDir = new File(project.buildDir, 'gen')
        dexFile = new File(project.libsDir, 'classes.dex')
        instrumentedDexFile = new File(project.libsDir, 'classes-instrumented.dex')
        obfuscatedDexFile = new File(project.libsDir, 'classes-obfuscated.dex')
        resourcePkg = new File(project.buildDir, 'resources.ap_')
        instrumentationTestsRunner = 'android.test.InstrumentationTestRunner'
        coverageReportsDir = new File(project.reporting.baseDir, 'emma')
        coverageHtmlReport = new File(coverageReportsDir, 'coverage.html')
        coverageXmlReport = new File(coverageReportsDir, 'coverage.xml')
        coverageMetadataFile = new File(project.buildDir, 'coverage.em')
        lintReportsDir = new File(project.reporting.baseDir, 'lint')
        lintHtmlReport = new File(lintReportsDir, 'html')
        lintXmlReport = new File(lintReportsDir, 'lint-results.xml')

        proguardConfigFile = new File(project.projectDir, 'config/proguard/proguard.cfg')
        def proguardDir = new File(project.buildDir, 'proguard')
        proguardDumpFile = new File(proguardDir, 'dump.txt')
        proguardSeedsFile = new File(proguardDir, 'seeds.txt')
        proguardUsageFile = new File(proguardDir, 'usage.txt')
        proguardMappingFile = new File(proguardDir, 'mapping.txt')
        proguardOutJar = new File(project.libsDir, 'obfuscated.jar')
    }
}
