package com.jvoegele.gradle.tasks.android;

import org.gradle.api.GradleException;

public class AdbErrorException extends GradleException {
  public AdbErrorException(String message) {
    super(message);
  }
}
