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
   Process classes and JARs with ProGuard
   -> :classes

:androidPackageDebug
   Creates the Android application apk package, signed with debug key
   -> :proguard
   (:assemble lifecycle task depends on this task)

:androidPackageRelease
   Creates the Android application apk package, which must be signed
   before it is published
   -> :proguard

:androidInstall
   Installs the debug package onto a running emulator or device
   -> :androidPackageDebug

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
either move the source code to the directory expected by Gradle, or tell Gradle to use the src directory of your project directly.

1) Create a build.gradle file in the root directory of the project, and
include the Android plugin as follows:

buildscript {
  repositories {
    mavenRepo(urls: 'http://jvoegele.com/maven2/')
  }
  dependencies {
    classpath 'com.jvoegele.gradle.plugins:android-plugin:1.0'
  }
}
apply plugin: com.jvoegele.gradle.plugins.android.AndroidPlugin
repositories {
    mavenCentral()
}

2) The android create project command created the source code directly in the src directory of the project.  The Android plugin tries to conform to the
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


LIMITATIONS
===========

In the current version of the Android plugin, the proguard task is
incorporated into the build but is not very configurable.  It can be
disabled, however, by setting "proguard.enabled = false" in your
build.gradle file.


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

