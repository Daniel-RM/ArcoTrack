package com.example.envioposicionflotas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.icu.text.Edits;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements GpsStatus.Listener {

    TextView tvLatitud, tvLongitud, tvInfo, tvAltitud, tvVelocidad, tvCurso, tvHora;
    WifiManager admin_wifi;
    Socket socket;
    int puerto = 8086;
    PrintWriter printWriter;
    //String identificador = "COMARCO012";
    String identificador = "PRUEBAD999";
    String fecha, cadena, latNS, lonES, altitud, curso, velocidad, estado, satelites, horas, tiempo;
    Double lon, lat;
    Button btnInicio;

    LocationManager mlocManager;
    GpsStatus status;
    Iterable<GpsSatellite> satellites;
    int contador;


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
        btnInicio = findViewById(R.id.btnInicio);
        btnInicio.setVisibility(View.INVISIBLE);

        //Mantengo la pantalla en vertical siempre
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        admin_wifi = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        if (admin_wifi.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Por favor, desconecte el wifi, para que la aplciación funcione correctamente", Toast.LENGTH_SHORT).show();
        }
        miUbicacion();

    }

    public void miUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        } else {
            locationStart();
        }
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationStart();
                return;
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return;
        }

        mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 1000, (LocationListener) Local);//Compruebo coordenadas cuando cambian y cada 60 segundos; Proveedor - internet
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 1000, (LocationListener) Local);//Compruebo coordenadas cuando cambian y cada 60 segundos; Proveedor - GPS

        status = mlocManager.getGpsStatus(null);
        satellites = status.getSatellites();
        Iterator<GpsSatellite> satI = satellites.iterator();
        while(satI.hasNext()){
            GpsSatellite satellite = satI.next();
            contador++;
        }
        satelites = String.valueOf(contador);

        tvInfo.setText("Localización agregada");

    }

    public void enviaTrama(Location location) {

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.screenBrightness = 0;
        getWindow().setAttributes(params);

        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        tiempo = df.format(location.getTime());
        fecha = tiempo.substring(0, 10);
        horas = tiempo.substring(11, 19);
        lon = location.getLongitude();
        lat = location.getLatitude();
        altitud = String.valueOf(location.getAltitude());
        velocidad = String.valueOf(location.getSpeed());
        curso = String.valueOf(location.getBearing());
        if(estado == null){
            estado = "0";
        }


        //satelites = "8";
        //estado = "363";

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
        tvCurso.setText(curso);
        tvHora.setText(horas);
        tvVelocidad.setText(velocidad);

        cadena = identificador + ",0,0,0,0,0,0,0," + fecha + "," + horas + "," + Math.abs(lon) + "," + Math.abs(lat) + "," + latNS + "," + lonES + "," + altitud + "," + velocidad + "," + estado + "," + satelites + "," + curso + "," + 1 + "," + 0 + "," + "VM_0";
        Log.d("Cadena", cadena);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //socket = new Socket("192.168.0.64", puerto);
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
        //contador = 0;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        switch (event){
            case GpsStatus.GPS_EVENT_STARTED:
                estado = "1";
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                estado = "2";
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                estado = "3";
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                estado = "4";
                status = mlocManager.getGpsStatus(null);
                satellites = status.getSatellites();
                contador = 0;
                Iterator<GpsSatellite> satI = satellites.iterator();
                while(satI.hasNext()){
                    GpsSatellite satellite = satI.next();
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