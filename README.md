This is the Android plugin for the Gradle build system.  This plugin
enables the creation of Android applications using Gradle, with all of
the power and flexibility you've come to expect from Gradle.

Currently, Gradle 1.0-rc-3 is required.

For mailing lists see the project page on Google Code:
https://code.google.com/p/gradle-android-plugin/

For issue tracking see the GitHub issues page: 
https://github.com/jvoegele/gradle-android-plugin/issues

FEATURES
========

Features of the Android plugin include:

* Compile, package, and install Android applications.  (Including
  handling of Android resource files.)
* Sign application packages using the default debug key, or with a
  release key for publication to Android Market.
* Incorporation of ProGuard to ensure that applications have minimal
  memory footprint.
* Easily create Android applications in Scala (and possibly Groovy or Clojure).

The Android plugin fully integrates into the Gradle build lifecycle by
extending the Java plugin.  Furthermore, the incorporation of ProGuard
into the build not only ensures that Android application packages are
small and tight, it also trivially enables the use of Scala for Android
application development simply by incorporating the existing Scala
plugin into the build.  ProGuard will include only those classes
from the Scala library that are actually used by your Android
application, resulting in an application package that is as small as
possible.


TASKS AND LIFECYCLE
===================

The Android plugin adds the following tasks and dependencies to the
build:

    :androidProcessResources
       Generate R.java source file from Android resource XML files (:compileJava task depends on this task)

    :proguard
       Process classes and JARs with ProGuard (by default, this task is disabled, more on this below)
       -> :jar

    :androidPackage
       Creates the Android application apk package
       -> :proguard

    :androidSignAndAlign
       Signs (with a debug or provided key; more on this below) and zipaligns the application apk package
       -> :androidPackage
       (:assemble lifecycle task depends on this task)

    :androidInstall
       Installs the built package onto a running emulator or device
       -> :assemble

    :androidUninstall
       Uninstalls the application from a running emulator or device

    :androidEmulatorStart
       Starts the android emulator

    :androidInstrumentationTests
       Runs an instrumentation test suite on a running emulator or device
       -> :androidInstall

USAGE
=====

To use the Android plugin for Gradle you must first create the
application skeleton using the android command-line tool.  For example:

    $ android create project --target 1 --path ./MyAndroidApp \
      --activity MyAndroidActivity --package my.android.package

This will create and Android application skeleton that you can
immediately build using Ant.  To build with Gradle instead, you must (1)
create a build.gradle file that includes the Android plugin, and (2)
either move the source code to the directory expected by Gradle, or tell
Gradle to use the src directory of your project directly.

1) Create a build.gradle file in the root directory of the project, and
include the Android plugin as follows:

    buildscript {
      repositories {
        mavenCentral()
      }

      dependencies {
        classpath 'org.gradle.api.plugins:gradle-android-plugin:1.2.1'
      }
    }
    apply plugin: 'android'
    repositories {
      mavenCentral()
    }

2) The android create project command created the source code directly in the
src directory of the project.  The Android plugin tries to conform to the
conventions established by Android's Ant-based build, but in this case
it is better to conform to Gradle's "source sets" convention, since it allows
you to have separate test source code, or to use multiple languages.
Therefore, we recommend that the source should be moved to src/main/java
instead.  Once you've done this you can, of course, utilize Gradle's source
sets to their full extent by placing resources in src/main/resources, Scala
source files in src/main/scala etc.  However, if you prefer to keep your
source code directly in the src directory (for example, if you need to retain
compatibility with Ant) then you can do so by configuring the Java source set
in your build.gradle file.  Just add the following to build.gradle:

    sourceSets {
      main {
        java {
          srcDir 'src/java'
        }
      }
    }

If your Android project was initially created by Eclipse rather than
the android create project command, then you will have some additional
setup work to do.  The Android plugin for Gradle must be told the
location of the Android SDK.  When you create a project with the
android create project command, the location is filled in for you by
the application generator, but the Eclipse project generator does not
provide this information to the project.  Therefore, you must fill it
in yourself.  To do this, create (or edit) the local.properties file in
the root of the project and add the sdk.dir property, referring to the
location of your Android SDK installation:

    sdk.dir = /path/to/android/sdk

Note that this file should not be checked in to your version control
system as it will likely differ across various development
environments.  Alternatively, you can use the ANDROID_HOME environment
variable instead.

Once you've performed these steps you can build your Android application
by invoking the tasks described above.

A complete minimal but real-world example is as follows.

build.gradle
------------

    buildscript {
      repositories {
        mavenCentral()
        // To use a development snapshot version of the plugin, add the
        // Sonatype Snapshots repository.
        maven {
          url "https://oss.sonatype.org/content/repositories/snapshots"
        }
      }

      dependencies {
        classpath 'org.gradle.api.plugins:gradle-android-plugin:1.2.1'
      }
    }

    apply plugin: 'android'

    repositories {
        mavenCentral()
    }

    // Sets the package version
    version = "1.0.0"

    // Signing configuration, valid for all builds (1)
    androidSignAndAlign {
      keyStore = "path/to/my/keystore"
      keyAlias = "my-key-alias"
      keyStorePassword = "mystorepass"
      keyAliasPassword = "myaliaspass"
    }

    // Configure the filtering of resources with properties from the Gradle's project scope (2)
    processResources {
      expand (project.properties)
    }

    // Configure a dedicated debug build (3)
    task configureDebug << {
      jar.classifier = "debug"
    }

    // Configure a dedicated release build (4)
    task configureRelease << {
      proguard.enabled = true
    }

============


This build script configures the build to sign with a provided keystore. This
configuration applies for every build (1).
It also sets Gradle to expand properties in every resource file (2).

In this way you can get a full build with the command:

    gradle assemble

It processes all the resources, expanding them with properties from the project's scope,
compiles classes, packs them into the dex file, builds the apk, signs it with
the provided keystore (but does not process it with proguard) and zipaligns
the package, which is named <project>-x.y.z.apk and placed in <project-root>/build/distributions.
You can see the proguard task is skipped from Gradle's output during the build.

You can create several build configurations and choose which one to execute from the
gradle command line.
The task configureDebug (3) defines the Gradle classifier for the package name.
Executing this build with the command:

    gradle configureDebug assemble

creates a package with same steps than the default build, but the package name
is project-x.y.z-debug.apk.

The task configureRelease (4) defines a release configuration task, which
activate the proguard step. Again, you get the package named <project>-x.y.z.apk
in the same output directory.

To disable signing and get a signed apk with the debug key, you can remove the
androidPackage configuration (1): if keyStore or keyAlias are null, the signing is
skipped and the debug key is used. Of course, you can put the signing configuration
in a dedicated configuration task and invoke that task in order to get a signed
package (or not to call it for the debug signed package).

Also note that if you don't supply password (that is, keyStorePassword or keyAliasPassword
are null), Gradle asks for them on the command line (not very good for CI servers...).


To install the generated apk onto a running emulator or a device connected with USB (and
configured in debug mode), run:

    gradle androidInstall

This installs the default built package; as with previous examples, if you want to install the
debug (or release) package, you have to issue:

    gradle configureDebug androidInstall

or

    gradle configureRelease androidInstall


The androidUninstall task unistalls the application from a running emulator or a device.
There is no need to specify which package: there can be only one package to undeploy and
it's defined by the base package name of the application, from androidManifest.xml.

ECLIPSE
=======
You can use Gradle to generate an Eclipse project for you. The Android plugin enhances this
process by setting up the Eclipse project correctly for Android, which includes establishing
the correct class path and inserting the Android builders into the build process.

To use the Eclipse integration, first make sure that you apply the Gradle Eclipse plugin in
your build.gradle file:

    apply plugin: 'eclipse'

Then you can generate the Eclipse project files as follows:

    gradle eclipse


INSTRUMENTATION TESTS
=====================
The plugin is able to run instrumentation tests for you on a connected device or emulator:

    gradle androidInstrumentationTests

On projects that do not define any instrumentations in their manifest, this task will safely
be skipped. By default, the task runs all tests in the given project, using Android's default
test runner. If you want more control, you can add a configure closure to your test project:

    androidInstrumentationTests {
      runners {
        run with: "com.mydomain.MyTestRunner", options: "..."
      }
    }

The 'run' method can you be used in different ways. If used as above, all tests will be run
using the given test runner. The 'options' field can be used to route parameters to the
Activity manager that's used to run the instrumentation (cf. 'adb shell am instrument').
Note that you don't have to supply the "-w" parameter, that's done by default.

You can also partition your test suite to run with different runners. Note that this only
works if you don't also have a more general runner configured as seen above. Currently,
the plugin allows you to partition by test package and annotation:

    run testpackage: ".unit", with: ".MyUnitTestRunner"
    run annotation: "android.test.suitebuilder.annotation.Smoke", with: ".MySmokeTestRunner"

If your test project's package ID is com.myapp.test, then this configuration will first run
all test cases within the com.myapp.test.unit package with the com.myapp.test.MyUnitTestRunner,
and then run all test cases carrying Android's "Smoke" annotation in the same manner.

There is also limited support for publishing JUnit compliant test reports that can be read
by build servers like Hudson. Currently, this only works if you're using the Android JUnit
test report runner or a sub-class of it: https://github.com/jsankey/android-junit-report
In that case, the test reports generated by that runner will be published to build/test-results.
We plan to make this more flexible and configurable in future versions.

START EMULATOR
=====================
For starting the emulator wih gradle, you need do define the AVD-Name from your android-emulator
in your build.gradle:

    androidEmulatorStart {
      avdName = "Main"
    }


LIMITATIONS
===========

* In the current version of the Android plugin, the proguard task is not very configurable.
* The androidManifest.xml file is not processed as a normal resource, i.e. there is
  no properties expansion (so, for example, you don't get the version set in the version tag,
  you have to align them manually).
