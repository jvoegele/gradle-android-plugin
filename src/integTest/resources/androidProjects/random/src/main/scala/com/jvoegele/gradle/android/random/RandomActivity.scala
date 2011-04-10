package com.jvoegele.gradle.android.random

import android.app.Activity
import android.os.Bundle
import android.widget.{TextView, Button}
import android.view.View

class RandomActivity extends Activity {
  val generator = new Generator

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    val number = findViewById(R.id.number).asInstanceOf[TextView]

    val button = findViewById(R.id.button).asInstanceOf[Button]
    button.setOnClickListener(new View.OnClickListener() {
      def onClick(view: View) = {
        number.setText("" + generator.generate)
      }
    })
  }
}
