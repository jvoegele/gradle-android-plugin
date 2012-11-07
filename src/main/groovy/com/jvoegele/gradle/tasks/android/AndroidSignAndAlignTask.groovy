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
import org.gradle.api.GradleException

import org.apache.tools.ant.util.JavaEnvUtils

class AndroidSignAndAlignTask extends DefaultTask {
  @Optional @Input String keyStore
  @Optional @Input String keyAlias
  @Optional @Input String keyStorePassword
  @Optional @Input String keyAliasPassword
  @Optional @Input String sigalg
  @Optional @Input String digestalg
  @Optional @Input String keyalg
  boolean verbose

  private File customUnsingedArchivePath
  private AndroidPluginConvention androidConvention = project.convention.plugins.android

  private void doSign(String keystore, String keypass, String storepass, String alias) {
    project.ant.copyfile(src: unsignedArchivePath.absolutePath,
                         dest: buildUnalignedArchivePath().absolutePath,
                         forceoverwrite: true)

    if (!(new File(keystore)).exists())
      throw new GradleException("Keystore file ${keystore} not found")

    def args = [JavaEnvUtils.getJdkExecutable('jarsigner'),
                '-verbose',
                '-sigalg', sigalg != null ? sigalg : 'MD5withRSA',
                '-digestalg', digestalg != null ? digestalg : 'SHA1',
                '-keystore', keystore,
                '-keypass', keypass,
                '-storepass', storepass,
                buildUnalignedArchivePath().absolutePath,
                alias]

    println "Signing with command:"
    for (String s : args)
      print s + " "
    println ""
    def proc = args.execute()
    def outRedir = new StreamRedir(proc.inputStream, System.out)
    def errRedir = new StreamRedir(proc.errorStream, System.out)
    
    outRedir.start()
    errRedir.start()

    def result = proc.waitFor()
    outRedir.join()
    errRedir.join()
	
    if (result != 0)
      throw new GradleException('Couldn\'t sign')
 
  }

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

    doSign(getKeyStore(), keyAliasPassword, keyStorePassword, getKeyAlias())
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
            dname: 'CN=Android Debug,O=Android,C=US',
            sigalg: sigalg != null ? sigalg : 'MD5withRSA',
            keyalg: keyalg != null ? keyalg : 'RSA')
      }

      return debugKeystore.absolutePath
  }

  private void signWithDebugKey() {
    logger.info("Signing final apk with debug key...")

    doSign(retrieveDebugKeystore(), 'android', 'android', 'androiddebugkey')
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

class StreamRedir extends Thread {
  private inStream
  private outStream

  public StreamRedir(inStream, outStream) {
    this.inStream = inStream
    this.outStream = outStream
  }

  public void run() {
    int b;
    while ((b = inStream.read()) != -1)
      outStream.write(b)
  }
}
