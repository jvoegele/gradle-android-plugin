This is the Android plugin for the Gradle build system.  This plugin
enables the creation of Android applications using Gradle, with all of
the power and flexibility you've come to expect from Gradle.


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
   Generate R.java source file from Android resource XML files
   (:compileJava task depends on this task)

:proguard
   Process classes and JARs with ProGuard (by default, this task is disabled,
   more on this below)
   -> :jar

:androidPackage
   Creates the Android application apk package, signed with debug key
   or provided key (more on this below)
   -> :proguard
   (:assemble lifecycle task depends on this task)

:androidInstall
   Installs the built package onto a running emulator or device
   -> :assemble

:androidUninstall
   Uninstalls the application from a running emulator or device


USAGE
=====

To use the Android plugin for Gradle you must first create the
application skeleton using the android command-line tool.  For example:

$ android create project --target 2 --path ./MyAndroidApp \
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
    mavenRepo(urls: 'http://jvoegele.com/maven2/')
  }
  dependencies {
    classpath 'com.jvoegele.gradle.plugins:android-plugin:0.9.5'
  }
}
apply plugin: com.jvoegele.gradle.plugins.android.AndroidPlugin
repositories {
    mavenCentral()
}

2) The android create project command created the source code directly in the
src directory of the project.  The Android plugin tries to conform to the
conventions established by Android's Ant-based build, but in this case
it is better to conform to Gradle's "source sets" convention, since it allows
you to have separate test source code, or to use multiple languages.
Therefore, I recommend that the source should be moved to src/main/java
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

(Note that this file should not be checked in to your version control
system as it will likely differ across various development
environments.)

Once you've performed these steps you can build your Android application
by invoking the tasks described above.

A complete minimal but real-world example is as follows.


build.gradle
============

buildscript {
  repositories {
    mavenRepo(urls: 'http://jvoegele.com/maven2/')
  }
  dependencies {
    classpath 'com.jvoegele.gradle.plugins:android-plugin:0.9.5'
  }
}
apply plugin: com.jvoegele.gradle.plugins.android.AndroidPlugin
repositories {
    mavenCentral()
}

// Sets the package version
version = "x.y.z"

// Signing configuration, valid for all builds (1)
androidPackage {
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

=============

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


LIMITATIONS
===========

* In the current version of the Android plugin, the proguard task is not very configurable.
* Gingerbread SDK is not currently supported (it will be soon).
* The androidManifest.xml file is not processed as a normal resource, i.e. there is
  no properties expansion (so, for example, you don't get the version set in the version tag,
  you have to align them manually).

FUTURE DIRECTIONS
=================

* In a future version of the Android plugin, I would like to integrate
  with the Eclipse plugin to ensure that Eclipse projects generated with
  "gradle eclipse" are optimized for Android development.
* Make it easier to declare the plugin in the build.gradle file, ideally
  by simply saying:
    usePlugin 'android'
  or
    apply plugin: 'android'

