package com.jvoegele.gradle.enhancements

import org.gradle.api.Project 
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.plugins.ide.eclipse.model.BuildCommand
import org.gradle.plugins.ide.eclipse.model.SourceFolder

class EclipseEnhancement extends GradlePluginEnhancement
{
    public EclipseEnhancement(Project project)
    {
        super(project)
    }

    public void apply()
    {
        project.gradle.taskGraph.whenReady { taskGraph ->
            if (!project.plugins.hasPlugin('eclipse')) {
                return
            }
            project.eclipse.project {
                natures 'com.android.ide.eclipse.adt.AndroidNature'
                def builders = new LinkedList(buildCommands)
                builders.addFirst(new BuildCommand('com.android.ide.eclipse.adt.PreCompilerBuilder'))
                builders.addFirst(new BuildCommand('com.android.ide.eclipse.adt.ResourceManagerBuilder'))
                builders.addLast(new BuildCommand('com.android.ide.eclipse.adt.ApkBuilder'))
                buildCommands = new ArrayList(builders)

                project.configurations.androidLibs.dependencies.withType(ProjectDependency).all {
                    linkedResource name: '.lib_' + dependencyProject.name, type: "2",
                        location: dependencyProject.projectDir.absolutePath
                }
            }

            project.eclipse.classpath {
                containers.removeAll { it == 'org.eclipse.jdt.launching.JRE_CONTAINER' }
                containers 'com.android.ide.eclipse.adt.ANDROID_FRAMEWORK'

                file {
                    whenMerged { classpath ->
                        // the ADT use a top-level gen/ folder, whereas the plugin uses build/gen, so swap them
                        classpath.entries.removeAll { it instanceof SourceFolder && it.dir?.path == project.android.genDir.path }
                        classpath.entries.add(new SourceFolder('gen', null))

                        project.configurations.androidLibs.dependencies.withType(ProjectDependency).all {
                            def p = dependencyProject
                            def sourceSet = p.sourceSets.main
                            sourceSet.allJava.srcDirs.each { dir ->
                                classpath.entries.add(new SourceFolder('.lib_' + p.name + File.separator + p.relativePath(dir), null))
                            }
                        }
                    }
                }
            }
        }
    }
}
