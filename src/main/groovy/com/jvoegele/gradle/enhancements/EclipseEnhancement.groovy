package com.jvoegele.gradle.enhancements

import org.gradle.api.Project 
import org.gradle.plugins.ide.eclipse.model.BuildCommand

class EclipseEnhancement extends GradlePluginEnhancement {
  public EclipseEnhancement(Project project) {
    super(project)
  }

  public void apply() {
    project.gradle.taskGraph.whenReady { taskGraph ->

      if (!project.plugins.hasPlugin('eclipse'))
        return;

      project.configure(project.eclipseProject) {
        beforeConfigured {
          natures 'com.android.ide.eclipse.adt.AndroidNature'
          def builders = new LinkedList(buildCommands)
          builders.addFirst(new BuildCommand('com.android.ide.eclipse.adt.PreCompilerBuilder'))
          builders.addFirst(new BuildCommand('com.android.ide.eclipse.adt.ResourceManagerBuilder'))
          builders.addLast(new BuildCommand('com.android.ide.eclipse.adt.ApkBuilder'))
          buildCommands = new ArrayList(builders)
        }
      }

      project.configure(project.eclipseClasspath) {
        beforeConfigured {
          containers.removeAll { it == 'org.eclipse.jdt.launching.JRE_CONTAINER' }
          containers 'com.android.ide.eclipse.adt.ANDROID_FRAMEWORK'
          sourceSets = project.sourceSets
          sourceSets.main.java.srcDir 'gen'
        }
      }
    }
  }

}
