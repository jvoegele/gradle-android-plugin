package com.jvoegele.gradle.android.support

import org.gradle.BuildAdapter
import org.gradle.api.invocation.Gradle
import org.gradle.initialization.ClassLoaderRegistry

class MyBuildListener extends BuildAdapter {
  @Override
  void projectsLoaded(Gradle gradle) {
    // This is a hack: Gradle Android Plugin integration tests have Gradle Android Plugin itself on their classpath,
    // but Gradle lets the buildscript only access a few selected packages, not the full classpath. That is a good
    // idea per se, as we should make the test buildscripts self-contained anyway, but I wasn't able to make it work
    // (a 'buildscript { ... }' section in test buildscript isn't enough, something is missing...). So I simply
    // adjust the Gradle filtering classloader to allow access to Gradle Android Plugin. It is not a public API,
    // but it's highly unlikely that it will change before we switch to using Gradle Tooling API. See TestProject too.
    gradle.rootProject.services.get(ClassLoaderRegistry.class).rootClassLoader.allowPackage("com.jvoegele.gradle.plugins.android")
  }
}
