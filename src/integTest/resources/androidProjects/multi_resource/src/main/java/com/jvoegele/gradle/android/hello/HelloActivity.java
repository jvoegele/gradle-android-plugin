package com.jvoegele.gradle.android.hello;

import android.app.Activity;
import android.os.Bundle;

public class HelloActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}
