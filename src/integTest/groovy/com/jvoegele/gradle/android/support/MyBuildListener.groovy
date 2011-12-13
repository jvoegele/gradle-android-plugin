/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
