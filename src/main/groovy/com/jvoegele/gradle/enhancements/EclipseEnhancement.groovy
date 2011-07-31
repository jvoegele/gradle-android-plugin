package com.jvoegele.gradle.enhancements

import org.gradle.api.Project 
import org.gradle.plugins.ide.eclipse.model.BuildCommand
import org.gradle.plugins.ide.eclipse.model.SourceFolder;

class EclipseEnhancement extends GradlePluginEnhancement {
  public EclipseEnhancement(Project project) {
    super(project)
  }

  public void apply() {
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
            // add the 'gen' folder that includes R.java
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
