package com.jvoegele.gradle.enhancements

import org.gradle.api.Project 

class EclipseEnhancement extends GradlePluginEnhancement {
  public EclipseEnhancement(Project project) {
    super(project)
  }

  public void apply() {
    project.gradle.taskGraph.whenReady { taskGraph ->
      if (taskGraph.hasTask(':eclipse')) {
        def eclipseProject = project.tasks['eclipseProject']
/*
        if (eclipseProject) {
          eclipseProject.natureNames += 'com.android.ide.eclipse.adt.AndroidNature'
          def buildCommands = ['com.android.ide.eclipse.adt.ResourceManagerBuilder',
                               'com.android.ide.eclipse.adt.PreCompilerBuilder']
          buildCommands.addAll(eclipseProject.buildCommandNames)
          buildCommands += 'com.android.ide.eclipse.adt.ApkBuilder'
          eclipseProject.buildCommandNames = new LinkedHashSet(buildCommands)
        }

        def eclipseClasspath = project.tasks.eclipseCp
        eclipseClasspath.srcDirs += androidConvention.genDir
      }
*/
    }
  }

}
