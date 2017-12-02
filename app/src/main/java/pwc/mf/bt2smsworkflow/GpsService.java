package pwc.mf.bt2smsworkflow;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

public class GpsService extends Service {

    private LocationListener locationListener;
    private LocationManager locationManager;
    private String provider;

    public static Location _lastLocation;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int res = super.onStartCommand(intent, flags, startId);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
            Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }

        provider = LocationManager.GPS_PROVIDER;


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return res;
        }
        _lastLocation = locationManager.getLastKnownLocation(provider);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                _lastLocation=location;
            }

            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                switch (status) {
                    case LocationProvider.OUT_OF_SERVICE:
                        // locationManager.removeUpdates(locationListener);
                        break;
                    case LocationProvider.AVAILABLE:
                        // requestGpsLocation();
                        break;
                }
            }

            public void onProviderEnabled(String provider) {
                // log("provider " + provider + " enabled");
            }

            public void onProviderDisabled(String provider) {
                // log("provider " + provider + " disabled");
            }
        };

        // Request the first GPS location
        requestGpsLocation();


        return res;

    }

    private void requestGpsLocation() {

        locationManager.removeUpdates(locationListener);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager
                .requestLocationUpdates(provider, 0, 0, locationListener);
    }


    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


}
