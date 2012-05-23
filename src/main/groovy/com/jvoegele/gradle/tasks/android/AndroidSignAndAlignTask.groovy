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

import com.jvoegele.gradle.plugins.android.AndroidPluginConvention
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class AndroidSignAndAlignTask extends DefaultTask {
  @Optional @Input String keyStore
  @Optional @Input String keyAlias
  @Optional @Input String keyStorePassword
  @Optional @Input String keyAliasPassword
  boolean verbose

  private File customUnsingedArchivePath
  private AndroidPluginConvention androidConvention = project.convention.plugins.android

  private File buildUnalignedArchivePath() {
    return new File(project.libsDir, "${androidConvention.apkBaseName}-unaligned.apk")
  }

  @InputFile
  File getUnsignedArchivePath() {
    customUnsingedArchivePath ?: androidConvention.unsignedArchivePath
  }

  void setUnsignedArchivePath(File unsignedArchivePath) {
    customUnsingedArchivePath = unsignedArchivePath
  }

  @OutputFile
  File getApkArchivePath() {
    return androidConvention.apkArchivePath
  }

  @TaskAction
  void process() {
    createDirs()
    sign()
    zipAlign()
  }

  private void createDirs() {
    apkArchivePath.parentFile.mkdirs()
    unsignedArchivePath.parentFile.mkdirs()
    buildUnalignedArchivePath().parentFile.mkdirs()
  }

  private void sign() {
    if (getKeyStore() || getKeyAlias()) {
      signWithProvidedKey()
    } else {
      signWithDebugKey()
    }
  }

  private void signWithProvidedKey() {
    if (!getKeyStorePassword() || !getKeyAliasPassword()) {
      def console = System.console()
      keyStorePassword = new String(console.readPassword(
          "Please enter keystore password (store:${keyStore}): "))
      keyAliasPassword = new String(console.readPassword(
          "Please enter password for alias '${keyAlias}': "))
    }

    logger.info("Signing final apk...")

    project.ant.signjar(
        jar: unsignedArchivePath.absolutePath,
        signedjar: buildUnalignedArchivePath().absolutePath,
        keystore: getKeyStore(),
        storepass: getKeyStorePassword(),
        alias: getKeyAlias(),
        keypass: getKeyAliasPassword(),
        verbose: verbose)
  }

  private String retrieveDebugKeystore() {
      File debugKeystore = new File(System.getProperty('user.home'), '.android/debug.keystore')

      if (!debugKeystore.exists()) {
        logger.info('Creating a new debug key...')

        project.ant.genkey(
            alias: 'androiddebugkey',
            storepass: 'android',
            keystore: debugKeystore.absolutePath,
            keypass: 'android',
            validity: 10 * 365,
            storetype: 'JKS',
            dname: 'CN=Android Debug,O=Android,C=US')
      }

      return debugKeystore.absolutePath
  }

  private void signWithDebugKey() {
    logger.info("Signing final apk with debug key...")

    project.ant.signjar(
        jar: unsignedArchivePath.absolutePath,
        signedjar: buildUnalignedArchivePath().absolutePath,
        keystore: retrieveDebugKeystore(),
        storepass: 'android',
        alias: 'androiddebugkey',
        keypass: 'android',
        verbose: verbose)
  }

  private void zipAlign() {
    logger.info("Running zip align on final apk...")
    String inPath = buildUnalignedArchivePath().absolutePath
    String outPath = apkArchivePath.absolutePath
    project.exec {
      executable ant.zipalign
      if (verbose) args '-v'
      args '-f', 4, inPath, outPath
    }
  }
}
