package com.arcoelectronica.aroctrack;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.arcoelectronica.arcotrack.R;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity implements GpsStatus.Listener {

    TextView tvLatitud, tvLongitud, tvInfo, tvAltitud, tvVelocidad, tvCurso, tvHora, tvImei, tvUser;
    WifiManager admin_wifi;
    Socket socket;
    int puerto = 8086;
    PrintWriter printWriter;

    String fecha, cadena, latNS, lonES, altitud, curso, velocidad, estado, satelites, horas, tiempo, user, imei, identificador;
    Double lon, lat;

    LocationManager mlocManager;
    GpsStatus status;
    Iterable<GpsSatellite> satellites;
    int contador;
    long tiempos;
    float precision;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLatitud = findViewById(R.id.tvlatitud);
        tvLongitud = findViewById(R.id.tvLongitud);
        tvInfo = findViewById(R.id.tvInfo);
        tvAltitud = findViewById(R.id.tvAltitud);
        tvVelocidad = findViewById(R.id.tvVelocidad);
        tvCurso = findViewById(R.id.tvCurso);
        tvHora = findViewById(R.id.tvHora);
        tvImei = findViewById(R.id.tvImei);
        tvUser = findViewById(R.id.tvUser);

        //Mantengo la pantalla en vertical siempre
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = extras.getString("user");
            imei = extras.getString("imei");
            identificador = user;
            tvUser.setText(user);
            tvImei.setText(imei);
        } else {
            Toast.makeText(getApplicationContext(), "Ha habido algún problema. por favor, vuelva a introducir sus datos", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }

        Intent intent = new Intent(this, MyService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }else{
            startService(intent);
        }

        admin_wifi = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (admin_wifi.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Por favor, desconecte el wifi, para que la aplciación funcione correctamente", Toast.LENGTH_SHORT).show();
        }


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return;
        } else {
            locationStart();
        }
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationStart();
                    return;
                }
            }
        }
    }



    public void locationStart() {

        contador = 0;
        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Localizacion Local = new Localizacion();
        Local.setMainActivity(this);
        final boolean gpsEnabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
        }

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION,}, 1000);
            return;
        }

        loadPermissionPage(this);

        //mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 500, (LocationListener) Local);//Compruebo coordenadas cuando cambian y cada 60 segundos; Proveedor - internet
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 500, (LocationListener) Local);//Compruebo coordenadas cuando cambian y cada 60 segundos; Proveedor - GPS

            status = mlocManager.getGpsStatus(null);
            satellites = status.getSatellites();
            Iterator<GpsSatellite> satI = satellites.iterator();
            while (satI.hasNext()) {
                contador++;
            }
            satelites = String.valueOf(contador);

            tvInfo.setText("Localización agregada");

    }

    private void loadPermissionPage(Activity activity){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, 0);
    }


    public void enviaTrama(Location location) {

        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        tiempos = location.getTime();
        tiempo = df.format(location.getTime());
        fecha = tiempo.substring(0, 10);
        horas = tiempo.substring(11, 19);
        lon = location.getLongitude();
        lat = location.getLatitude();
        altitud = String.valueOf((int)location.getAltitude());


        df.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        String tiempoMadrid = df.format(location.getTime());
        tiempoMadrid = tiempoMadrid.substring(11,19);
        tvHora.setText(tiempoMadrid);


        if(estado == null){
            estado = "0";
        }

        if (lat < 0) {
            latNS = "S";
        } else {
            latNS = "N";
        }

        if (lon < 0) {
            lonES = "W";
        } else {
            lonES = "E";
        }

        tvLatitud.setText(String.valueOf(lat));
        tvLongitud.setText(String.valueOf(lon));
        tvAltitud.setText(altitud);
        //tvHora.setText(horas);

        String proveedor = location.getProvider();
//        if(proveedor.equals("network")){
//            // el proveedor es la red
//            cadena = identificador + ",20,20,20,20,20,20,20," + fecha + "," + horas + "," + Math.abs(lon) + "," + Math.abs(lat) + "," + latNS + "," + lonES + "," + altitud + "," + velocidad + "," + estado + "," + satelites + "," + curso + "," + 1 + "," + 0 + "," + "VM_0";
//        }else{
            // el proveedor es el GPS
            cadena = identificador + ",0,0,0,0,0,0,0," + fecha + "," + horas + "," + Math.abs(lon) + "," + Math.abs(lat) + "," + latNS + "," + lonES + "," + altitud + "," + velocidad + "," + estado + "," + satelites + "," + curso + "," + 1 + "," + 0 + "," + "VM_0";
       // }

        Log.d("Cadena", cadena);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("gps.arcoelectronica.net", puerto);
                    printWriter = new PrintWriter(socket.getOutputStream());
                    printWriter.write(cadena);
                    printWriter.flush();
                    printWriter.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    @Override
    public void onGpsStatusChanged(int event) {
        contador = 0;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        switch (event){
            case GpsStatus.GPS_EVENT_STARTED:
                estado = estado + "1";
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                estado = estado + "2";
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                estado = estado + "3";
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                estado = estado + "4";
                status = mlocManager.getGpsStatus(null);
                satellites = status.getSatellites();
                contador = 0;
                Iterator<GpsSatellite> satI = satellites.iterator();
                while(satI.hasNext()){
                    contador++;
                }
                satelites = String.valueOf(contador);
                break;
        }

    }


    /* Aqui empieza la Clase Localizacion */
    public class Localizacion implements LocationListener {

        MainActivity mainActivity;
        public MainActivity getMainActivity() {
            return mainActivity;
        }

        public void setMainActivity(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onLocationChanged(Location loc) {

            float velocity = (loc.getSpeed()*3.6f);
            velocidad = String.valueOf((int)velocity);
            curso = String.valueOf((int)loc.getBearing());
            tvVelocidad.setText(velocidad);
            tvCurso.setText(curso);
            precision = loc.getAccuracy();
            estado = String.valueOf((int)precision);


            if(!tvLongitud.getText().equals("")) {
                Location locationA = new Location("punto A");
                locationA.setLatitude(Double.parseDouble(tvLatitud.getText().toString()));
                locationA.setLongitude(Double.parseDouble(tvLongitud.getText().toString()));
                Location locationB = new Location("punto B");
                locationB.setLatitude(loc.getLatitude());
                locationB.setLongitude(loc.getLongitude());
            }

            Log.d("Trama recibida: ",loc.getLatitude() + ", " + loc.getLongitude());
            enviaTrama(loc);

        }

        @Override
        public void onProviderDisabled(String provider) {
            // Este metodo se ejecuta cuando el GPS es desactivado
            tvInfo.setText("GPS Desactivado");
        }
        @Override
        public void onProviderEnabled(String provider) {
            // Este metodo se ejecuta cuando el GPS es activado
            tvInfo.setText("GPS Activado");
        }


        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.d("debug", "LocationProvider.AVAILABLE");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                    break;
            }
        }
    }

}