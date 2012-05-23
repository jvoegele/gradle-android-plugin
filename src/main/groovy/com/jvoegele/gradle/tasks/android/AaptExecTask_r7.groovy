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

class AaptExecTask_r7 extends AndroidAntTask {
  AaptExecTask_r7(project) {
    super(project);
  }

  /* (non-Javadoc)
   * @see com.jvoegele.gradle.tasks.android.AndroidAntTask#execute(java.util.Map)
   */
  @Override
  void execute(Map args) {
    ant.aaptexec(
        executable: ant.aapt,
        command: args.get('command', 'package'),
        manifest: androidConvention.androidManifest.path,
        resources: androidConvention.resDir.path,
        assets: androidConvention.assetsDir,
        androidjar: ant['android.jar'],
        outfolder: project.libsDir,
        basename: project.name)
  }
}
