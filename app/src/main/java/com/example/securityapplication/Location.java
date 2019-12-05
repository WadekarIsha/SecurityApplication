package com.example.securityapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
//import android.location.Location;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class Location extends AppCompatActivity implements View.OnClickListener {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private LocationManager locationManager = null;
    private LocationListener locationListener = null;

    private Button btnGetLocation = null;
    EditText editLocation = null;
    ProgressBar pb = null;

    private static final String TAG = "Debug";
    private Boolean flag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        setRequestedOrientation(ActivityInfo
                .SCREEN_ORIENTATION_PORTRAIT);

        pb = (ProgressBar) findViewById(R.id.progressBar1);
        pb.setVisibility(View.INVISIBLE);

        editLocation = (EditText) findViewById(R.id.editTextLocation);

        btnGetLocation = (Button) findViewById(R.id.btnLocation);
        //btnGetLocation.setOnClickListener(this);
        findViewById(R.id.btnLocation).setOnClickListener(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }


    public void onClick(View view) {
        ActivityCompat.requestPermissions(Location.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_LOCATION);
        flag = displayGpsStatus();
        if (flag) {
            Log.v(TAG, "onClick");
            editLocation.setText("Please move device to see change in co-ordinates");
            pb.setVisibility(View.VISIBLE);
            locationListener = new MyLocationListener();
            try {

                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        1000, 1, locationListener);

            } catch (SecurityException e) {
                //Handle exception
            }
        }
    }

    private Boolean displayGpsStatus() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           return false;

        }
        return true;

    }
}

 class MyLocationListener extends Location implements LocationListener{
     @Override
     public void onLocationChanged(android.location.Location loc) {
         Log.d("Location.java","Inside onLocationChanged");
         editLocation.setText("");
         pb.setVisibility(View.VISIBLE);
         Toast.makeText(getBaseContext(),
                 "Location changed : Latitude :" +loc.getLatitude()
                        +"Longitude :" +loc.getLongitude(),
                        Toast.LENGTH_LONG).show();
         String latitude = Double.toString(loc.getLatitude());
         String longitude = Double.toString(loc.getLongitude());
     }

     @Override
     public void onStatusChanged(String s, int i, Bundle bundle) {

     }

     @Override
     public void onProviderEnabled(String s) {

     }

     @Override
     public void onProviderDisabled(String s) {

     }
 }
