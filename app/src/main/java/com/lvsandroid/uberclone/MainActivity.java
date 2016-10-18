package com.lvsandroid.uberclone;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MainActivity extends AppCompatActivity {
    Switch swRiderOrDriver;

    public void getStarted(View view) {
        String riderOrDriver = "rider";

        if(swRiderOrDriver.isChecked()) {
            riderOrDriver = "driver";
        }

        ParseUser.getCurrentUser().put("riderOrDriver",riderOrDriver);
        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null) {
                    Log.i("DBG","user signed up");
                    redirectUser();
                } else {
                    Log.e("ERROR",e.toString());
                }
            }
        });

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swRiderOrDriver = (Switch) findViewById(R.id.switchRiderOrDriver);

        //=== Hide toolbar
        getSupportActionBar().hide();


        if(ParseUser.getCurrentUser() == null) {
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e == null) {
                        Log.i("ERROR", "Anonymous user logged in");
                    } else {
                        Log.e("ERROR", "Anonymous login failed");
                    }
                }
            });
        } else {
            if(ParseUser.getCurrentUser().get("riderOrDriver") != null) {
                Log.i("DBG","Redirect user");
                redirectUser();
            }
        }
    }

    private void redirectUser() {
        if(ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")) {
            Intent i = new Intent(getApplicationContext(),MapsActivity.class);
            startActivity(i);
        } else {
            Log.i("DBG","Go to request screen");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
