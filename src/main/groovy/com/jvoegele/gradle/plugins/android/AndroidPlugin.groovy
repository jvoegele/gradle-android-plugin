package com.jvoegele.gradle.plugins.android

import com.jvoegele.gradle.enhancements.EclipseEnhancement
import com.jvoegele.gradle.enhancements.JavadocEnhancement
import com.jvoegele.gradle.enhancements.ScalaEnhancement
import com.jvoegele.gradle.tasks.android.AbstractAaptPackageTask
import com.jvoegele.gradle.tasks.android.AbstractAdbTask
import com.jvoegele.gradle.tasks.android.Aidl
import com.jvoegele.gradle.tasks.android.Dex
import com.jvoegele.gradle.tasks.android.EmulatorTask
import com.jvoegele.gradle.tasks.android.GenerateResources
import com.jvoegele.gradle.tasks.android.Install
import com.jvoegele.gradle.tasks.android.Lint
import com.jvoegele.gradle.tasks.android.Package
import com.jvoegele.gradle.tasks.android.PackageResources
import com.jvoegele.gradle.tasks.android.ProGuard
import com.jvoegele.gradle.tasks.android.Pull
import com.jvoegele.gradle.tasks.android.Test
import com.jvoegele.gradle.tasks.android.Uninstall
import com.jvoegele.gradle.tasks.android.coverage.Instrument
import com.jvoegele.gradle.tasks.android.coverage.Report
import com.jvoegele.gradle.tasks.android.instrumentation.InstrumentationTestsTask

import org.apache.tools.ant.Target

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.compile.Compile

/**
 * Gradle plugin that extends the Java plugin for Android development.
 *
 * @author Marcus Better (marcus@better.se)
 * @author Jason Voegele (jason@jvoegele.com)
 */
class AndroidPlugin implements Plugin<Project> {
    public static final ANDROID_INSTRUMENTATION_TESTS_TASK_NAME = "androidInstrumentationTests"
    public static final ANDROID_START_EMULATOR_TASK_NAME = "androidEmulatorStart"

    private static final ANDROID_GROUP = "Android"

    private static final PROPERTIES_FILES = ['local', 'build', 'project']
    final log = Logging.getLogger(AndroidPlugin)

    def appPackage
    def manifest

    private Project project
    def installTask

    @Override
    public void apply(Project project)
    {
        project.plugins.apply 'java'
        this.project = project

        project.extensions.android = new AndroidPluginExtension(project)
        androidSetup()
        
        def androidConfiguration = project.configurations.add('android').setVisible(false)
        def emmaRuntimeConfiguration = project.configurations.add('emmaRuntime')
        project.dependencies {
            android project.files(project.ant.references['android.target.classpath'].list())
            emmaRuntime project.files(new File(project.ant['emma.dir'], 'emma_device.jar'))
        }

        defineTasks()
        project.tasks.withType(Package).all {
            conventionMapping.baseName = { project.archivesBaseName }
            conventionMapping.version = { project.version == Project.DEFAULT_VERSION ? null : project.version }
            conventionMapping.destinationDir = { project.distsDir }
            conventionMapping.resourcePkg = { project.android.resourcePkg }
        }
        project.tasks.withType(AbstractAaptPackageTask).all { AbstractAaptPackageTask task ->
            conventionMapping.manifest = { project.android.manifest }
            conventionMapping.resourceDir = { project.android.resDir }
        }
        project.tasks.withType(GenerateResources).all { GenerateResources task ->
            conventionMapping.genDir = { project.android.genDir }
            conventionMapping.nonConstantId = { project.android.library }
        }
        project.tasks.withType(PackageResources).all { PackageResources task ->
            conventionMapping.assetsDir = {
                def assetsDir = project.file(project.android.assetsDir)
                if (assetsDir.exists()) {
                    assetsDir
                }
            }
            conventionMapping.resourcePkg = { project.android.resourcePkg }
            conventionMapping.versionName = { project.version }
            conventionMapping.versionCode = { project.android.versionCode }
        }
        project.tasks.withType(Aidl).all { Aidl task ->
            conventionMapping.genDir = { project.android.genDir }
        }
        project.tasks.withType(AbstractAdbTask).all { AbstractAdbTask task ->
            conventionMapping.targetDevice = { project.android.targetDevice }
            conventionMapping.adb = { new File(project.ant.adb) }
        }
        project.tasks.withType(Lint).all { Lint task ->
            conventionMapping.lint = { new File(project.ant.'android.tools.dir', "lint$project.ant.exe") }
        }
        project.tasks.withType(Compile).all {
            dependsOn project.configurations.android
            options.bootClasspath = project.configurations.android.asPath
        }
        project.tasks.findAll { it.group == ANDROID_GROUP }.logging*.captureStandardOutput LogLevel.INFO
        project.afterEvaluate {
            project.sourceSets.main.java.srcDirs += project.android.genDir
        }
        configureEnhancements()
    }

    private void androidSetup() {
        def ant = project.ant

        PROPERTIES_FILES.each { ant.property(file: "${it}.properties") }

        // Determine the sdkDir value.
        def sdkDir
        try {
            sdkDir = ant['sdk.dir']
        } catch (MissingPropertyException e) {
            // ignore
        }
        if (!sdkDir) {
            sdkDir = System.getenv("ANDROID_HOME")
        }
        if (!sdkDir) {
            throw new MissingPropertyException("Unable to find location of Android SDK. Please read documentation.")
        }
        ant.'sdk.dir' = sdkDir

        ['test', 'clean', 'install', 'uninstall', 'instrument'].each {
            // avoid name clash with targets in build.xml
            ant.project.addTarget(it, new Target())
        }
        ant.importBuild new File(sdkDir, 'tools/ant/build.xml')
        ant.project.executeTarget '-setup'

        // TODO: there can be several instrumentations defined
        ant.xpath(input: project.android.manifest, expression: "/manifest/instrumentation/@android:targetPackage", output: "tested.manifest.package")
        ant.xpath(input: project.android.manifest, expression: "/manifest/application/@android:hasCode",
            output: "manifest.hasCode", 'default': "true")

        ant.xpath(input: project.android.manifest, expression: "/manifest/instrumentation/@android:name", output: "android.instrumentation")
        try {
            manifest = new XmlSlurper().parse(project.android.manifest)
            appPackage = manifest.@package.text()
        } catch (Exception e) {
            throw new RuntimeException("failed to parse $project.android.manifest", e)
        }
        if (ant['android.instrumentation']) {
            project.android.instrumentationTestsRunner = ant['android.instrumentation']
        }
    }

    def defineTasks() {
        def genResourcesTask = project.task('genResources', group: ANDROID_GROUP, type: GenerateResources)
        project.tasks.compileJava.dependsOn genResourcesTask
        project.afterEvaluate { Project p ->
            genResourcesTask.libResourceDirs = project.files()
            genResourcesTask.libPackageNames = ''
        }

        def aidlTask = project.task('aidl', group: ANDROID_GROUP, type: Aidl)
        project.tasks.compileJava.dependsOn aidlTask
        project.afterEvaluate { Project p ->
            aidlTask.srcDirs = project.files(project.sourceSets.main.java.srcDirs)
            project.files()
            genResourcesTask.libPackageNames = ''
        }

        def main = project.sourceSets.main
        def classes = main.output.classesDir
        def classesInstrumented = new File(project.buildDir, "instrumented/$main.name")
        def instrument = project.task(main.getTaskName('instrument', null),
            group: ANDROID_GROUP, type: Instrument,
            description: 'instrument class files for Emma code coverage',
            dependsOn: main.output)
        instrument.inputs.source { classes }
        def conv = instrument.conventionMapping
        conv.destDir = { classesInstrumented }
        conv.metadataFile = { project.android.coverageMetadataFile }

        project.ant.taskdef(resource: 'proguard/ant/task.properties', classpath: new File(project.ant.'android.tools.dir', 'proguard/lib/proguard.jar'))
        def proguardTask = project.task('proguard', type: ProGuard, group: ANDROID_GROUP,
            description: 'process classes and libraries with ProGuard',
            dependsOn: main.output) {
            conventionMapping.configFile = { project.android.proguardConfigFile }
            conventionMapping.dumpFile = { project.android.proguardDumpFile }
            conventionMapping.seedsFile = { project.android.proguardSeedsFile }
            conventionMapping.usageFile = { project.android.proguardUsageFile }
            conventionMapping.mappingFile = { project.android.proguardMappingFile }
            conventionMapping.outJar = { project.android.proguardOutJar }
            inputs.source main.runtimeClasspath.filter { it.exists() }
            libs = project.files(project.configurations.android)
        }

        def dexTask = project.task(main.getTaskName('dex', null), type: Dex, group: ANDROID_GROUP,
                description: 'convert classes to .dex format',
                dependsOn: { main.output.asFileTree }) {
            inputs.source project.configurations.runtime, classes
            conventionMapping.dexFile = { project.android.dexFile }
        }
        def dexInstrumented = project.task(main.getTaskName('dexInstrumented', null), type: Dex, group: ANDROID_GROUP,
                description: 'convert instrumented classes to .dex format',
                dependsOn: [instrument, project.configurations.emmaRuntime, { main.runtimeClasspath }]) {
            inputs.source project.configurations.runtime, project.configurations.emmaRuntime, classesInstrumented
            noLocals = true
            conventionMapping.dexFile = { project.android.instrumentedDexFile }
        }
        def dexObfuscated = project.task(main.getTaskName('dexObfuscated', null), type: Dex, group: ANDROID_GROUP,
            description: 'convert obfuscated classes to .dex format',
            dependsOn: proguardTask) {
            inputs.source proguardTask.outJar
            conventionMapping.dexFile = { project.android.obfuscatedDexFile }
        }

        def packageResourcesTask = project.task('packageResources', type: PackageResources, group: ANDROID_GROUP,
            description: 'put the resources into the package file')

        def packageDebugTask = project.task('packageDebug', type: Package, group: ANDROID_GROUP,
            description: 'build the debug application archive',
            dependsOn: [dexTask, packageResourcesTask]) {
            debug = true
            appendix = 'debug'
            jarFiles = project.files()
            conventionMapping.dexFile = { dexTask.dexFile }
        }
        project.tasks.assemble.dependsOn packageDebugTask
        def packageDebugObfuscatedTask = project.task('packageDebugObfuscated', type: Package, group: ANDROID_GROUP,
            description: 'build the debug application archive',
            dependsOn: [dexObfuscated, packageResourcesTask]) {
            debug = true
            appendix = 'debug-proguard'
            jarFiles = project.files()
            conventionMapping.dexFile = { dexObfuscated.dexFile }
        }

        def packageInstrumentedTask = project.task('packageInstrumented', type: Package, group: ANDROID_GROUP,
            description: 'build the instrumented application archive',
            dependsOn: [dexInstrumented, packageResourcesTask]) {
            debug = true
            appendix = 'instrumented'
            jarFiles = project.configurations.emmaRuntime
            conventionMapping.dexFile = { dexInstrumented.dexFile }
        }

        def androidLibs = project.configurations.add('androidLibs')
        project.configurations.compile.extendsFrom androidLibs
        project.afterEvaluate { Project p ->
            p.tasks.withType(AbstractAaptPackageTask).all { AbstractAaptPackageTask task ->
                task.libResourceDirs = project.files()
                p.configurations.androidLibs.dependencies.withType(ProjectDependency).all { ProjectDependency dep ->
                    task.libResourceDirs += project.files(dep.dependencyProject.android.resDir)
                }
            }
        }

        def packageReleaseTask = project.task('packageRelease', type: Package, group: ANDROID_GROUP,
            description: 'build the release application archive',
            dependsOn: [dexObfuscated, packageResourcesTask]) {
            jarFiles = project.files()
            conventionMapping.dexFile = { dexObfuscated.dexFile }
        }
        def mapping = packageReleaseTask.conventionMapping
        def projOrSysProperty = { projectProp, systemProp ->
            project.hasProperty(projectProp) ? project[projectProp] : System.getProperty(systemProp)
        }
        mapping.keyStore = {
            def keyStore = project.android.keyStore
            if (!keyStore) {
                def keyStorePath = projOrSysProperty('keyStore', 'key.store')
                if (keyStorePath) {
                    keyStore = new File(keyStorePath)
                }
            }
            keyStore
        }
        mapping.keyStorePassword = {
            project.android.keyStorePassword ?: projOrSysProperty('keyStorePassword', 'key.store.password')
        }
        mapping.keyAlias = {
            project.android.keyAlias ?: projOrSysProperty('keyAlias', 'key.alias')
        }
        mapping.keyPassword = {
            project.android.keyPassword ?: projOrSysProperty('keyPassword', 'key.password')
        }

        installTask = project.task('install', group: ANDROID_GROUP, type: Install,
            description: "Installs the debug package onto a running emulator or device",
            dependsOn: packageDebugTask) {
            conventionMapping.app = { packageDebugTask.archivePath }
        }
        project.task('installObfuscated', group: ANDROID_GROUP, type: Install,
            description: "Installs the obfuscated debug package onto a running emulator or device",
            dependsOn: packageDebugObfuscatedTask) {
            conventionMapping.app = { packageDebugObfuscatedTask.archivePath }
        }
        def installInstrumentedTask = project.task('installInstrumented', group: ANDROID_GROUP, type: Install,
            description: "Installs the instrumented package onto a running emulator or device",
            dependsOn: packageInstrumentedTask) {
            conventionMapping.app = { packageInstrumentedTask.archivePath }
        }
        def uninstallTask = project.task('uninstall', group: ANDROID_GROUP, type: Uninstall,
            description: "Uninstalls the application from a running emulator or device") {
            appPackage = this.appPackage
        }

        def junitreportConfig = project.configurations.add('junitreport')
        project.dependencies {
            junitreport group: 'org.apache.ant', name: 'ant-junit', version: '1.8.2'
        }
        def deviceTestTask = project.task('deviceTest', group: ANDROID_GROUP, type: Test, dependsOn: installTask) {
            appPackage = this.appPackage
            testRunnerClass = manifest.instrumentation.@name.text()
            dataDir = "/data/data/${manifest.instrumentation.@targetPackage.text()}"
        }
        def deviceTestCoverageTask = project.task('deviceTestCoverage', group: ANDROID_GROUP, type: Test, dependsOn: installTask) {
            appPackage = this.appPackage
            testRunnerClass = manifest.instrumentation.@name.text()
            dataDir = "/data/data/${manifest.instrumentation.@targetPackage.text()}"
            emmaDumpFile = "$dataDir/coverage.ec"
            params = [coverage: true, coverageFile: emmaDumpFile]
        }
        def copyTestReportTask = project.task('copyDeviceTestReport', group: ANDROID_GROUP, type: Pull,
            dependsOn: junitreportConfig) {
            outputs.upToDateWhen { false }
            outputs.dir project.testReportDir
            src = deviceTestTask.dataDir + "/files/junit-report.xml"
            dest = new File(project.testResultsDir, 'junit-report.xml')
        } << {
            ant.xslt(basedir: dest.parent, destdir: project.testReportDir, includes: dest.name) {
                style {
                    javaresource name: 'org/apache/tools/ant/taskdefs/optional/junit/xsl/junit-frames.xsl',
                        classpath: junitreportConfig.asPath
                }
                param name: 'output.dir', expression: project.testReportDir.absolutePath
            }
        }
        def copyEmmaDumpFile = project.task('copyEmmaDumpFile', group: ANDROID_GROUP, type: Pull,
            dependsOn: deviceTestCoverageTask) {
            outputs.upToDateWhen { false }
            outputs.dir project.testReportDir
            src = deviceTestCoverageTask.emmaDumpFile
            dest = new File(project.testResultsDir, 'coverage.ec')
        }
        def emmaReport = project.task('emmaReport', group: ANDROID_GROUP, type: Report,
            dependsOn: copyEmmaDumpFile) {
            inputs.file { copyEmmaDumpFile.dest }
            conventionMapping.xmlReport = { project.android.coverageXmlReport }
            conventionMapping.htmlReport = { project.android.coverageHtmlReport }
        }
        def lintTask = project.task('lint', group: ANDROID_GROUP, type: Lint,
            dependsOn: project.tasks.compileJava) {
            conventionMapping.xmlReport = { project.android.lintXmlReport }
            outputs.upToDateWhen { false }
        }

        def testConfig = project.configurations.add('androidTest')
        main.compileClasspath += testConfig
        testConfig.dependencies.withType(ProjectDependency).all { ProjectDependency dep ->
            def testedProject = dep.dependencyProject
            def tasks = testedProject.tasks
            uninstallTask.dependsOn tasks.uninstall
            emmaReport.srcDirs = testedProject.files(testedProject.sourceSets.main.java.srcDirs)
            tasks.withType(Instrument).all { t ->
                emmaReport.inputs.file { t.metadataFile }
            }
            deviceTestTask.dependsOn tasks.install
            deviceTestCoverageTask.dependsOn tasks.installInstrumented
        }

        defineAndroidEmulatorStartTask()
        defineAndroidInstrumentationTestsTask()
    }

  private void defineAndroidEmulatorStartTask() {
    def androidEmulatorStartTask = project.task(ANDROID_START_EMULATOR_TASK_NAME,
        description: "Starts the android emulator", type:EmulatorTask)
    androidEmulatorStartTask.group = ANDROID_GROUP
  }
  
  private void defineAndroidInstrumentationTestsTask() {
    def description = """Runs instrumentation tests on a running emulator or device.
      Use the 'runners' closure to configure your test runners:
          
         androidInstrumentationTests {
           runners {
             run testpackage: "com.my.package", with: "com.my.TestRunner"
             run annotation: "com.my.Annotation", with: "com.my.OtherRunner"
           } 
         }
          
      You can also use 'run with: "..."' to run all tests using the given runner, but
      note that this only works as long as you do not bind any other more specific runners.
    """

    def androidInstrumentationTestsTask = project.task(
        ANDROID_INSTRUMENTATION_TESTS_TASK_NAME,
        group: ANDROID_GROUP,
        description: description,
        type: InstrumentationTestsTask, testPackage: appPackage)
    androidInstrumentationTestsTask.dependsOn(installTask)
  }

  /**
   * Configure enhancements to other Gradle plugins so that they work better in
   * concert with the Android plugin.
   */
  private void configureEnhancements() {
    new JavadocEnhancement(project).apply()
    new EclipseEnhancement(project).apply()
    new ScalaEnhancement(project).apply()
  }
}
