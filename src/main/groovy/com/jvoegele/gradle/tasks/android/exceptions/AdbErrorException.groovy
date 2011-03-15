package com.jvoegele.gradle.tasks.android.exceptions

import org.gradle.api.GradleException

class AdbErrorException extends GradleException {
  def AdbErrorException(String message) {
    super(message)
  }
}
