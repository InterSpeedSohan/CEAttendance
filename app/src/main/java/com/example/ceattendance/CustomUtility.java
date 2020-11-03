package com.example.ceattendance;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Base64;

import androidx.appcompat.app.AlertDialog;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class CustomUtility {


    public static String imageToString(Bitmap bitmap)
    {
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        float aspectRatio = bitmap.getWidth() / (float)bitmap.getHeight();
        int width = 1280;
        int height = Math.round(width / aspectRatio);
        bitmap = Bitmap.createScaledBitmap(bitmap,width,height,false);
        bitmap.compress(Bitmap.CompressFormat.JPEG,30,byteArrayOutputStream);
        byte[] imgBytes=byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }


    public static String imageToStringLow(Bitmap bitmap)
    {
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,1,byteArrayOutputStream);
        byte[] imgBytes=byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    public static void showAlert(Context context, String message, String title)
    {
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton("Ok",null);
        builder.setTitle(title );
        builder.show();
    }

    public static void showWarning(Context context, String mess, String tittle)
    {
        new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(tittle)
                .setContentText(mess)
                .setConfirmText("Ok")
                .show();
    }
    public static void showError(Context context, String mess, String tittle)
    {
        new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(tittle)
                .setContentText(mess)
                .setConfirmText("Ok")
                .show();
    }
    public static void showSuccess(Context context, String mess, String tittle)
    {
        new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText(tittle)
                .setContentText(mess)
                .setConfirmText("Ok")
                .show();
    }

    public static String getTimeStamp(String format)
    {
        SimpleDateFormat s = new SimpleDateFormat(format);
        String timeStamp = s.format(new Date());
        return timeStamp;
    }

    public static boolean haveNetworkConnection(Context con) {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                    if (ni.isConnected())
                        haveConnectedWifi = true;
                if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (ni.isConnected())
                        haveConnectedMobile = true;
            }
        } catch (Exception e) {

        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    public static void haveGpsEnabled(final Context context)
    {
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(context)
                    .setMessage("GPS is not enabled")
                    .setPositiveButton("Location Setting", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .show();
        }
    }


}
