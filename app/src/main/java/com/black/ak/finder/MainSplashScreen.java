package com.black.ak.finder;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Created by Akash.Jangir on 12/24/2015.
 */
public class MainSplashScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Thread background = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    sleep(2*1000);
                    SharedPreferences preferences = getSharedPreferences("com.finder.ak", MODE_PRIVATE);
                    String email = preferences.getString("email", null);
                    Intent intent;
                    if(email != null) {
                        intent = new Intent(MainSplashScreen.this,HomeActivity.class);
                        startActivity(intent);
                    }else {
                        intent = new Intent(MainSplashScreen.this, LoginActivity.class);
                        startActivity(intent);
                    }
                    finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        background.start();
    }
}
