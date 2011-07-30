package com.jvoegele.gradle.android

import org.gradle.testfixtures.ProjectBuilder

import com.jvoegele.gradle.android.support.TestProject

class AbstractIntegrationTest {
  def androidProjectsDir = new File(System.getProperty('integTest.androidProjects'))

  def project(path) {
    def projectDir = new File(androidProjectsDir, path)
    def project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    return new TestProject(project: project)
  }
}
