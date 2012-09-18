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

package com.jvoegele.gradle.tasks.android;

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Uses the ProGuard tool to create a minimal JAR containing only those classes
 * and resources actually used by the application code.
 */
class ProGuard extends DefaultTask {
  private static final String PRO_GUARD_RESOURCE = "proguard/ant/task.properties"

  String artifactGroup = "net.sf.proguard"
  String artifactName = "proguard"
  String artifactVersion = "4.4"

  boolean warn = false
  boolean note = false
  boolean obfuscate = true

  private boolean proGuardTaskDefined = false

  ProGuard () {
    // By default, this task is disabled - it has to be explicitly enabled by user in build.gradle
    enabled = false
  }

  File getTempFile() {
    AndroidPluginConvention androidConvention = project.convention.plugins.android
    return new File (project.libsDir, androidConvention.getApkBaseName() + "-proguard-temp.jar")
  }

  File getProguardConfig() {
    return new File(project.projectDir, "proguard.cfg")
  }

  @TaskAction
  protected void process() {
    defineProGuardTask()

    String tempFilePath = getTempFile().getAbsolutePath()

    Map proguardOptions = [
      'warn': warn,
      'obfuscate': obfuscate
    ]

    if (proguardConfig.exists()) {
      project.logger.info("File ${proguardConfig} found. Using it.")
      proguardOptions['configuration'] = proguardConfig
    } else {
      project.logger.info("File ${proguardConfig} not found. Using default configuration.")
      // use some minimal configuration if proguard.cfg doesn't exist
      // this is basically the same as what "android create project" generates
      proguardOptions['optimizationpasses'] = 5
      proguardOptions['usemixedcaseclassnames'] = false
      proguardOptions['skipnonpubliclibraryclasses'] = false
      proguardOptions['preverify'] = false
      proguardOptions['verbose'] = true
    }

    ant.proguard(proguardOptions) {
      injar(path: project.jar.archivePath)

      // Add each dependency into the ProGuard-processed JAR
      project.configurations.compile.files.each { dependency ->
        injar(file: dependency)
      }

      outjar(file: tempFilePath)

      ant.references['android.target.classpath'].each { targetjar ->
        libraryjar(file: targetjar)
      }

      if (!proguardConfig.exists()) {
        // use some minimal configuration if proguard.cfg doesn't exist
        // this is basically the same as what "android create project" generates
        optimizations(filter: "!code/simplification/arithmetic,!field/*,!class/merging/*")

        keep(access: 'public', 'extends': 'android.app.Activity')
        keep(access: 'public', 'extends': 'android.app.Application')
        keep(access: 'public', 'extends': 'android.app.Service')
        keep(access: 'public', 'extends': 'android.content.BroadcastReceiver')
        keep(access: 'public', 'extends': 'android.content.ContentProvider')
        keep(access: 'public', 'extends': 'android.app.backup.BackupAgentHelper')
        keep(access: 'public', 'extends': 'android.preference.Preference')
        keep(access: 'public', name: 'com.android.vending.licensing.ILicensingService')
        keepclasseswithmembernames {
          method(access: 'native')
          constructor(access: 'public', parameters: 'android.content.Context,android.util.AttributeSet')
          constructor(access: 'public', parameters: 'android.content.Context,android.util.AttributeSet,int')
        }
        keepclassmembers('extends': 'java.lang.Enum') {
          method(access: 'public static', type: '**[]', name: 'values', parameters: '')
          method(access: 'public static', type: '**', name: 'valueOf', parameters: 'java.lang.String')
        }
        keepclassmembers('extends': 'android.app.Activity') {
          method(access: 'public', type: 'void', name: '*', parameters: 'android.view.View')
        }
        keep('implements': 'android.os.Parcelable') {
          field(access: 'public static final', type: 'android.os.Parcelable$Creator')
        }
      }

      keep(access: 'public', 'name': '**.R')
      keep('name': '**.R$*')
    }

    // Update the output file of this task
    ant.move(file: tempFilePath, toFile: project.jar.archivePath, overwrite: true)
  }

  private void defineProGuardTask() {
    if (!proGuardTaskDefined) {
      project.configurations {
        proguard
      }

      project.dependencies {
        proguard group: artifactGroup, name: artifactName, version: artifactVersion
      }

      ant.taskdef(resource: PRO_GUARD_RESOURCE, classpath: project.configurations.proguard.asPath)

      proGuardTaskDefined = true
    }
  }
}
