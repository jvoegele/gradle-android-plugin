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

package com.jvoegele.gradle.enhancements

import org.gradle.api.Project
import org.gradle.plugins.ide.eclipse.model.BuildCommand
import org.gradle.plugins.ide.eclipse.model.SourceFolder;

class EclipseEnhancement extends GradlePluginEnhancement {
  def androidConvention = project.convention.plugins.android

  EclipseEnhancement(Project project) {
    super(project)
  }

  void apply() {
    project.gradle.taskGraph.whenReady { taskGraph ->
      if (!project.plugins.hasPlugin('eclipse'))
        return;

      def androidLibraryProjects = detectAndroidLibraryProjects()

      project.eclipse.project {
        natures 'com.android.ide.eclipse.adt.AndroidNature'

        def builders = new LinkedList(buildCommands)
        builders.addFirst(new BuildCommand('com.android.ide.eclipse.adt.PreCompilerBuilder'))
        builders.addFirst(new BuildCommand('com.android.ide.eclipse.adt.ResourceManagerBuilder'))
        builders.addLast(new BuildCommand('com.android.ide.eclipse.adt.ApkBuilder'))
        buildCommands = new ArrayList(builders)

        // add an Eclipse link to every library project's src folder (= type 2)
        androidLibraryProjects.each {
          linkedResource name: it.sourceName, type: "2", location: it.sourcePath
        }
      }

      project.eclipse.classpath {
        containers.removeAll { it == 'org.eclipse.jdt.launching.JRE_CONTAINER' }
        containers 'com.android.ide.eclipse.adt.ANDROID_FRAMEWORK'

        file {
          whenMerged { classpath ->
            // the ADT use a top-level gen/ folder, whereas the plugin uses build/gen, so swap them
            classpath.entries.removeAll { it instanceof SourceFolder && it.dir?.path == androidConvention.genDir.path }
            classpath.entries.add(new SourceFolder('gen', null))

            androidLibraryProjects.each {
              classpath.entries.add(new SourceFolder(it.sourceName, null))

              // now remove the artifact JAR from the classpath, or it would clash
              // with the classes compiled from src
              classpath.entries.removeAll { entry -> entry.path == it.artifact.file.absolutePath }
            }
          }
        }
      }
    }
  }

  // TODO: this currently only works if a Gradle dependency's artifact name equals the folder
  // name of the library project referenced in default.properties
  private def detectAndroidLibraryProjects() {
    // Android's SetupTask sets this for use based on library references in default.properties
    def librarySrcPaths = project.ant.references['project.libraries.src']?.list()

    if (!librarySrcPaths?.any()) {
      return []
    }

    def libraryProjects = []

    // try to match the project's resolved dependencies against the libraries in default.properties
    project.configurations.compile.resolvedConfiguration.resolvedArtifacts.each { artifact ->
      def matchingSrcPath = librarySrcPaths.find { it ==~ /.*\/${artifact.name}\/src$/ }

      if (matchingSrcPath) {
        def libraryProject = new Expando()
        libraryProject.artifact = artifact
        libraryProject.sourcePath = matchingSrcPath
        libraryProject.sourceName = "${artifact.name}_src"
        libraryProjects << libraryProject
      }
    }

    return libraryProjects
  }
}
