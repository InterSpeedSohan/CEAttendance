package com.example.ceattendance;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class LoginActivity extends AppCompatActivity {


    public static final String apkVersion = "1.2";  //please sepecify the apk version first
    public static final String SECURITY_TAG = "Security Permission";
    private static final int REQUEST_Code = 0;
    private int PERMISSION_ALL = 1;
    public static String code = "", message = "", userid = "", id = "", empid = "",empcode = "";
    private static String[] PERMISSIONS_LIST = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
    };
    private ImageView iv;
    Button btnlogin;
    EditText edtid, edtpass;
    JSONObject jsonObject;
    JSONArray jsonArray;
    String Json_String;
    boolean networkAvailable = false;
    SweetAlertDialog sweetAlertDialog;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        //logo animation blink.
        Animation animation = new AlphaAnimation(1, (float) 0.70); //to change visibility from visible to invisible
        animation.setDuration(1000); //1 second duration for each animation cycle
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
        animation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
        iv = (ImageView) findViewById(R.id.iv);
        iv.startAnimation(animation); //to start animation


        checkPermission();
        btnlogin = (Button) findViewById(R.id.btnlogin);
        edtid = findViewById(R.id.edtid);
        edtpass = findViewById(R.id.edtpass);
        LoginButton();
        sharedPreferences = getSharedPreferences("ce_user",MODE_PRIVATE);
        if(sharedPreferences.contains("user_name"))
        {
           edtid.setText(sharedPreferences.getString("user_name",null));
           edtpass.setText(sharedPreferences.getString("user_pass",null));
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    private void checkPermission() {
        if (!hasPermissions(this, PERMISSIONS_LIST)) {
            Log.e("per","error perm");
            ActivityCompat.requestPermissions(this, PERMISSIONS_LIST, PERMISSION_ALL);
        }
    }


    public void LoginButton() {

        btnlogin.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        networkAvailable = CustomUtility.haveNetworkConnection(LoginActivity.this);
                        //checking for
                        if (networkAvailable) {
                            String id  = edtid.getText().toString();
                            userid = id;
                            String pass = edtpass.getText().toString();
                            if(id.equals("") | pass.equals("")) {
                                CustomUtility.showWarning(LoginActivity.this,"You can't leave any field blank","Required");
                            }
                            else {
                                sweetAlertDialog = new SweetAlertDialog(LoginActivity.this,SweetAlertDialog.PROGRESS_TYPE);
                                sweetAlertDialog.setTitleText("Loading");
                                sweetAlertDialog.show();
                                login(id,pass);
                            }
                        }
                        else {
                            CustomUtility.showAlert(LoginActivity.this, "Please Check your internet connection","Network Warning !!!");
                        }
                    }
                }
        );
    }




    public void login(final String user_name, final String user_pass)
    {
        Log.e("uname+upass",user_name + " "+user_pass);
        String upLoadServerUri = "https://atmdbd.com/ce/api/login/login.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, upLoadServerUri,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            sweetAlertDialog.dismiss();
                            Log.e("response",response);
                            jsonObject = new JSONObject(response);
                            code = jsonObject.getString("success");
                            message = jsonObject.getString("message");
                            if (code.equals("true")) {
                                jsonObject = jsonObject.getJSONObject("userData");
                                userid = edtid.getText().toString();
                                SharedPreferences.Editor editor = getSharedPreferences("ce_user",MODE_PRIVATE).edit();
                                editor.putString("user_name",user_name);
                                editor.putString("user_pass",user_pass);
                                editor.apply();
                                id = jsonObject.getString("RecordId");
                                empid = jsonObject.getString("EmployeeId");
                                empcode = jsonObject.getString("EmployeeCode");

                                Bundle IDbundle = new Bundle();
                                IDbundle.putString("id", id);
                                IDbundle.putString("empid", empid);
                                IDbundle.putString("empcode", empcode);
                                IDbundle.putString("name", jsonObject.getString("UserFullName"));
                                IDbundle.putString("type", jsonObject.getString("UserTypeName"));
                                IDbundle.putString("typeId", jsonObject.getString("UserTypeId"));
                                Intent intent = new Intent(getApplicationContext(), AttendanceActivity.class);
                                intent.putExtras(IDbundle);
                                startActivity(intent);
                                finish();

                            }
                            else{
                                Log.e("mess",message);
                                CustomUtility.showError(LoginActivity.this,message,"Login Failed");
                            }
                        } catch (JSONException e) {
                            CustomUtility.showError(LoginActivity.this, e.getMessage(), "Getting Response");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                sweetAlertDialog.dismiss();
                Log.e("res",error.toString());
                CustomUtility.showError(LoginActivity.this, "Network Error, try again!", "Login failed");
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("LoginName",user_name);
                params.put("LoginPass",user_pass);
                return params;
            }
        };

        MySingleton.getInstance(LoginActivity.this).addToRequestQue(stringRequest);
    }


}
