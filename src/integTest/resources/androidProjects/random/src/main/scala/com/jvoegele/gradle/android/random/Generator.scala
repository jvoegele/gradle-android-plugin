package com.jvoegele.gradle.android.random

import java.util.Random

class Generator {
  val random = new Random

  def generate = random.nextInt(9999) + 1
}
