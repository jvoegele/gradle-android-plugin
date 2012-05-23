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

package com.jvoegele.gradle.tasks.android

class ApkBuilderTask_r6 extends AndroidAntTask {
  ApkBuilderTask_r6(project) {
    super(project)
  }

  /**
   * Execute the apkbuilder task.
   * @param args Map of keyword arguments.  Supported keywords are sign and
   *             verbose, both of which should be boolean values if provided.
   */
  void execute(Map args) {
    assert ant != null
    ant.apkbuilder(outfolder: project.buildDir,
      basename: project.name,
      signed: args.get('sign', false),
      'verbose': args.get('verbose', false)) {
        ant.file(path: androidConvention.intermediateDexFile.absolutePath)
        //sourcefolder(path: project.sourceSets.main.java)
        nativefolder(path: androidConvention.nativeLibsDir)
        //jarfolder(path: androidConvention.nativeLibsDir)
      }
  }
}
