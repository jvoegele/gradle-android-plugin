package com.jvoegele.gradle.tasks.android

import org.gradle.api.DefaultTask
import com.jvoegele.gradle.plugins.android.AndroidPluginConvention
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

class AndroidSignAndAlignTask extends DefaultTask {
  @Optional @Input String keyStore
  @Optional @Input String keyAlias
  @Optional @Input String keyStorePassword
  @Optional @Input String keyAliasPassword
  boolean verbose

  private AndroidPluginConvention androidConvention = project.convention.plugins.android

  @InputFile
  File getUnsignedArchivePath() {
      return androidConvention.unsignedArchivePath
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
    androidConvention.unalignedArchivePath.parentFile.mkdirs()
  }

  private void sign() {
    if (keyStore || keyAlias) {
      signWithProvidedKey()
    } else {
      signWithDebugKey()
    }
  }

  private void signWithProvidedKey() {
    if (!keyStorePassword || !keyAliasPassword) {
      def console = System.console()
      keyStorePassword = new String(console.readPassword(
          "Please enter keystore password (store:${keyStore}): "))
      keyAliasPassword = new String(console.readPassword(
          "Please enter password for alias '${keyAlias}': "))
    }

    logger.info("Signing final apk...")
    project.ant.signjar(
        jar: unsignedArchivePath.absolutePath,
        signedjar: androidConvention.unalignedArchivePath.absolutePath,
        keystore: keyStore,
        storepass: keyStorePassword,
        alias: keyAlias,
        keypass: keyAliasPassword,
        verbose: verbose
    )
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
                  dname: 'CN=Android Debug,O=Android,C=US'
          )
      }
      return debugKeystore.absolutePath
  }

  private void signWithDebugKey() {
    logger.info("Signing final apk with debug key...")

    project.ant.signjar(
        jar: unsignedArchivePath.absolutePath,
        signedjar: androidConvention.unalignedArchivePath.absolutePath,
        keystore: retrieveDebugKeystore(),
        storepass: 'android',
        alias: 'androiddebugkey',
        keypass: 'android',
        verbose: verbose
    )
  }

  private void zipAlign() {
    logger.info("Running zip align on final apk...")
    String inPath = androidConvention.unalignedArchivePath.absolutePath
    String outPath = apkArchivePath.absolutePath
    project.exec {
      executable ant.zipalign
      if (verbose) args '-v'
      args '-f', 4, inPath, outPath
    }
  }
}
