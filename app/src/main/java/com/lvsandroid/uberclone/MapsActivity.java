package com.lvsandroid.uberclone;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import android.Manifest;

import java.util.List;

@RuntimePermissions
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Double latitude = 0d;
    private Double longitude = 0d;
    private TextView infoTextView;
    private FloatingActionButton fabRequestUber;

    private Boolean requestActive = false;

    private void requestUber() {
        if(!requestActive) {
            Log.i("DBG", "Uber requested");

            ParseObject request = new ParseObject("Requests");
            request.put("requesterUsername", ParseUser.getCurrentUser().getUsername());

            ParseACL parseACL = new ParseACL();
            parseACL.setPublicWriteAccess(true);
            parseACL.setPublicReadAccess(true);
            request.setACL(parseACL);

            request.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        infoTextView.setText(getResources().getString(R.string.msg_finding_uber));
                        fabRequestUber.setImageResource(R.drawable.ic_car_redcross);
                        requestActive = true;
                    }
                }
            });
        } else {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Requests");
            query.whereEqualTo("requesterUsername",ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null) {
                        if(objects.size() > 0) {
                            for (ParseObject object:objects) {
                                object.deleteInBackground(new DeleteCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if(e == null) {
                                            Log.i("DBG","Request deleted");
                                            infoTextView.setText("Uber cancelled");
                                            fabRequestUber.setImageResource(R.drawable.ic_car);
                                            requestActive = false;
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    @NeedsPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            MapsActivityPermissionsDispatcher.getLocationWithCheck(this);
        else {
            LocationManager locationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);



            LocationListener locationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    setMapLocation(latitude,longitude);
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

            };

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 10, locationListener);
        }
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationDenied() {
        Toast.makeText(this, R.string.map_fine_location_denied, Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
        Toast.makeText(this, R.string.map_fine_location_never_askagain, Toast.LENGTH_SHORT).show();
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showRationaleForFineLocation(PermissionRequest request) {
        showRationaleDialog(R.string.map_fine_location_rationale, request);
    }

    private void showRationaleDialog(@StringRes int messageResId, final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.map_button_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.map_button_deny,  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(messageResId)
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        infoTextView = (TextView)findViewById(R.id.infoTextView);

        fabRequestUber = (FloatingActionButton) findViewById(R.id.requestUber);
        fabRequestUber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestUber();
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getLocation();

        setMapLocation(latitude, longitude);

    }

    private void setMapLocation(double latitude, double longitude) {
        if(latitude != 0 && longitude != 0) {

            mMap.clear();
            // Add a marker in Sydney and move the camera
            LatLng currentPosition = new LatLng(latitude, longitude);
            Log.i("Position Latitude", String.valueOf(latitude));
            Log.i("Position Longitude", String.valueOf(longitude));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition,10));
            mMap.addMarker(new MarkerOptions().position(currentPosition).title("Your location"));
        }

    }












}
