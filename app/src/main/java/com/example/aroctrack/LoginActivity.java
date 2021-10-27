package com.example.aroctrack;

import com.example.arcotrack.R;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    TextView tvCodigo;
    EditText edtUser, edtPass;
    String code, user, pass;
    Button btnEntrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tvCodigo = findViewById(R.id.tvCodigo);
        edtUser = findViewById(R.id.edtUser);
        edtPass = findViewById(R.id.edtPass);
        btnEntrar = findViewById(R.id.btnEntrar);

        //Mantengo la pantalla en vertical siempre
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        informa();

        //Compruebo si la aplicación ya tiene un código único asignado. Si no es así, lo creo y se lo asigno
        SharedPreferences preferences = getSharedPreferences("codigoInicial", Context.MODE_PRIVATE);
        if (preferences == null){
            creaCodigo();
        }else{
            code = preferences.getString("codigo","");
            code = code.replace("-","");
            code = code.substring(0,1) + code.substring(4,5) + code.substring(9,10) + code.substring(14,15) + code.substring(19,20);
            code = code.toUpperCase();
            tvCodigo.setText(code);
        }


        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user = edtUser.getText().toString().toUpperCase();
                pass = edtPass.getText().toString().toUpperCase();

                if(user.equals("") || pass.equals("")){
                    Toast.makeText(getApplicationContext(), "Por favor introduzca el usuario y la contraseña", Toast.LENGTH_SHORT).show();
                }else {
                    comprobarCodigo(user, pass, code);
                }
            }
        });
    }

    public void informa(){
        AlertDialog.Builder dialogo = new AlertDialog.Builder(LoginActivity.this)
                .setMessage("Esta aplicación hace uso de la ubicación en segundo plano y estará siempre activada, aunque la aplicación esté cerrada. " +
                        "Para detenerla, deberá forzar su detención. El uso de estos datos, será estrictamente profesional. Desea continuar?" )
                .setCancelable(false)
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity();
                    }
                })
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        dialogo.show();

    }


    public void creaCodigo(){
        SharedPreferences preferences = getSharedPreferences("codigoInicial", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("codigo", UUID.randomUUID().toString());
        editor.commit();
    }

    public void comprobarCodigo(String usuario, String codigo, String imei){

        String codigoFinal;
        String code;

        code = usuario+imei;

        try{
            byte[] codeByte = code.getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(codeByte);
            byte[] cod = md.digest();

            StringBuffer hexString = new StringBuffer();
            for(final byte element : cod){
                final String hex = Integer.toHexString(0xFF & element);
                if(hex.length()==1){
                    hexString.append('0');
                }
                hexString.append(hex);

            }
            codigoFinal = hexString + "";
            codigoFinal = codigoFinal.substring(0,1) + codigoFinal.substring(4,5) + codigoFinal.substring(9,10) + codigoFinal.substring(14,15) + codigoFinal.substring(19,20);
            codigoFinal = codigoFinal.toUpperCase();

            if(codigo.equals(codigoFinal)){
                Toast.makeText(getApplicationContext(), "Bienvenido " + user, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("user", user);
                intent.putExtra("imei", imei);
                startActivity(intent);
            }else{
                Toast.makeText(getApplicationContext(), "Los datos introducidos, no son correctos.", Toast.LENGTH_SHORT).show();
            }

        }catch (NoSuchAlgorithmException ex){
            ex.printStackTrace();
        }

    }

}