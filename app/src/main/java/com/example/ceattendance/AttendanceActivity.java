package com.example.ceattendance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AttendanceActivity extends AppCompatActivity {

    public static String presentLat = "", presentLon = "", presentAcc = "";
    private static final int MY_PERMISSIONS_REQUEST = 0;
    public LocationManager locationManager;
    public GPSLocationListener listener;
    public Location previousBestLocation = null;
    private static final int TWO_MINUTES = 1000 * 60 * 1;
    public static final String BROADCAST_ACTION = "gps_data";
    Intent intent;
    static Bitmap bitmap;
    Button inBtn, outBtn, leaveBtn,submitBtn,cameraBtn,fromBtn,toBtn;
    SweetAlertDialog sweetAlertDialog, progressDialog;
    JSONObject jsonObject;
    JSONArray jsonArray;
    Bundle IDbundle;

    Uri photoURI;
    static final int REQUEST_IMAGE_CAPTURE = 99;
    String currentPhotoPath = "";
    String activeBtn = "", photoName = "", fromDate = "", toDate = "",place = "",leaveType = "",retailerCode = "",reason = "",deviceDate = "";

    TextView location,textFrom, textTo, tvName, tvId, tvType;

    ConstraintLayout inout_layout, leave_layout,retailerOptionLay;
    final Calendar myCalendar = Calendar.getInstance();
    final Calendar myCalendar2 = Calendar.getInstance();

    Spinner retailerSpinner, optionSpinner, leaveTypeSpinner;
    String[] optionList = new String[] {"Distribution House", "Retail Point", "Head Office","In Transit","Others"};
    String[] retailerList = new String[] {"N/A"};
    String[] leaveTypeList = new String[] {"Casual Leave", "Half Day Leave", "Sick Leave", "On Training", "On Meeting"};
    List<String> DMSCode = new ArrayList<String>();
    EditText edtReason;
    String imageString = "";
    String typeId = "";
    Button gpsTextBtn;
    TextView gpsText, takeSelfieText;

    boolean photoFlag = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission","Storage permission already granted");
        }
        else {
            //Permission is not granted so you have to request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST);
            Log.e("Permission","Requesting for storage permission");
        }

        tvName = findViewById(R.id.tvName);
        tvId = findViewById(R.id.tvId);
        tvType = findViewById(R.id.tvArea);

        IDbundle = getIntent().getExtras();
        tvName.setText(IDbundle.getString("name"));
        tvId.setText("Employe id: "+IDbundle.getString("empcode"));
        tvType.setText(IDbundle.getString("type"));

        photoName = LoginActivity.userid + "_"+ CustomUtility.getTimeStamp("yyyyMMddhhmmss") + ".jpg";

        intent = new Intent(BROADCAST_ACTION);

        inout_layout = findViewById(R.id.in_out_layout);
        leave_layout = findViewById(R.id.leavelay);
        retailerOptionLay = findViewById(R.id.retailerlay);

        edtReason = findViewById(R.id.reason);


        inBtn = findViewById(R.id.inbtn);
        outBtn = findViewById(R.id.outbtn);
        leaveBtn = findViewById(R.id.leavebtn);
        cameraBtn = findViewById(R.id.camera);
        fromBtn = findViewById(R.id.frombtn);
        toBtn = findViewById(R.id.tobtn);
        submitBtn = findViewById(R.id.submitbtn);
        gpsTextBtn = findViewById(R.id.hideGpsText);

        takeSelfieText = findViewById(R.id.takeSelfieText);
        gpsText = findViewById(R.id.gpsText);
        location = findViewById(R.id.location);
        textFrom = findViewById(R.id.fromdate);
        textTo = findViewById(R.id.todate);

        optionSpinner = findViewById(R.id.option);
        retailerSpinner = findViewById(R.id.retailer_option);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, optionList);
        optionSpinner.setAdapter(adapter);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, R.layout.spinner_item, retailerList);
        retailerSpinner.setAdapter(adapter1);
        leaveTypeSpinner = findViewById(R.id.Option);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, R.layout.spinner_item, leaveTypeList);
        leaveTypeSpinner.setAdapter(adapter2);

        optionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                place = optionSpinner.getSelectedItem().toString();
                if(place.equals("Retail Point"))
                {
                    retailerOptionLay.setVisibility(View.VISIBLE);
                    progressDialog = new SweetAlertDialog(AttendanceActivity.this,SweetAlertDialog.PROGRESS_TYPE);
                    progressDialog.setTitle("Please wait...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    getRetailList();
                }
                else
                {
                    retailerOptionLay.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        retailerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                retailerCode = retailerSpinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        inBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                activeBtn = "in";
                leaveBtn.setBackgroundResource(R.drawable.inactive);
                inBtn.setBackgroundResource(R.drawable.active);
                outBtn.setBackgroundResource(R.drawable.inactive);
                inout_layout.setVisibility(View.VISIBLE);
                leave_layout.setVisibility(View.GONE);
                optionSpinner.setAdapter(adapter);
                deleteTakenPicture();
                currentPhotoPath = "";
                takeSelfieText.setText("Take selfie");
                photoFlag = false;
                takeSelfieText.getResources().getColor(R.color.text_color);
            }
        });

        outBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeBtn = "out";
                leaveBtn.setBackgroundResource(R.drawable.inactive);
                inBtn.setBackgroundResource(R.drawable.inactive);
                outBtn.setBackgroundResource(R.drawable.active);
                inout_layout.setVisibility(View.VISIBLE);
                leave_layout.setVisibility(View.GONE);
                optionSpinner.setAdapter(adapter);
                deleteTakenPicture();
                currentPhotoPath = "";
                takeSelfieText.setText("Take selfie");
                photoFlag = false;
                takeSelfieText.getResources().getColor(R.color.text_color);
            }
        });

        leaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeBtn = "leave";
                leaveBtn.setBackgroundResource(R.drawable.active);
                inBtn.setBackgroundResource(R.drawable.inactive);
                outBtn.setBackgroundResource(R.drawable.inactive);
                inout_layout.setVisibility(View.GONE);
                leave_layout.setVisibility(View.VISIBLE);
            }
        });

        leaveTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                leaveType = leaveTypeSpinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        CustomUtility.showAlert(AttendanceActivity.this, ex.getMessage(), "Creating Image");
                        return;
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                "com.example.ceattendance.fileprovider",
                                photoFile);
                        if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT))
                            photoURI = FileProvider.getUriForFile(getApplicationContext(), "com.example.ceattendance.fileprovider", photoFile);
                        else
                            photoURI = Uri.fromFile(photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                    }
                }
            }
        });


        final DatePickerDialog.OnDateSetListener fromdate = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateFromDate();
            }

        };

        fromBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(AttendanceActivity.this, fromdate, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });


        final DatePickerDialog.OnDateSetListener todate = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar2.set(Calendar.YEAR, year);
                myCalendar2.set(Calendar.MONTH, monthOfYear);
                myCalendar2.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                if(myCalendar.compareTo(myCalendar2) == 1)
                {
                    textTo.setText("");
                    CustomUtility.showWarning(AttendanceActivity.this,"Select correct date","Failed");
                }
               else{
                    updateToDate();
                }

            }

        };

        toBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fromDate.equals(""))
                {
                    CustomUtility.showWarning(AttendanceActivity.this,"Select from date first","Failed");
                }
                else
                {
                    new DatePickerDialog(AttendanceActivity.this, todate, myCalendar2
                            .get(Calendar.YEAR), myCalendar2.get(Calendar.MONTH),
                            myCalendar2.get(Calendar.DAY_OF_MONTH)).show();
                }

            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reason = edtReason.getText().toString();
                int flag = checkfeild();
                deviceDate = getDeviceDate();
                Log.e("DeviceDate: ",deviceDate);
                if(flag != -1)
                {
                    if(flag == 0 ) {
                        if (ContextCompat.checkSelfPermission(AttendanceActivity.this, Manifest.permission.INTERNET)
                                != PackageManager.PERMISSION_GRANTED) {
                            // Permission is not granted
                            Log.e("DXXXXXXXXXX", "Not Granted");
                            CustomUtility.showAlert(AttendanceActivity.this, "Permission not granted", "Permission");
                        } else {
                            sweetAlertDialog = new SweetAlertDialog(AttendanceActivity.this, SweetAlertDialog.WARNING_TYPE);
                            sweetAlertDialog.setTitle("Are you sure?");
                            sweetAlertDialog.setConfirmButton("Ok", new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    sweetAlertDialog.dismissWithAnimation();
                                    progressDialog = new SweetAlertDialog(AttendanceActivity.this,SweetAlertDialog.PROGRESS_TYPE);
                                    progressDialog.setTitle("Please wait...");
                                    progressDialog.setCancelable(false);
                                    progressDialog.show();
                                    upload();
                                }
                            });
                            sweetAlertDialog.setCancelButton("No", new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    sweetAlertDialog.dismissWithAnimation();
                                }
                            });
                            sweetAlertDialog.show();
                        }
                    }
                }

            }
        });
        GPS_Start();


        gpsTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsText.setVisibility(View.GONE);
                location.setVisibility(View.VISIBLE);
            }
        });


    }

    //after finishing camera intent whether the picture was save or not
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            photoFlag = true;
            takeSelfieText.setText("Take selfie (done)");
            takeSelfieText.getResources().getColor(R.color.input_color);
        }
    }


    public int checkfeild()
    {
        if(activeBtn.equals(""))
        {
            CustomUtility.showWarning(this,"Select attendance type In, Out or Leave","Required feild!");
            return -1;
        }
        else if((activeBtn.equals("in") | activeBtn.equals("out")) & place.equals(""))
        {
            CustomUtility.showWarning(this,"Select your position","Required feild!");
            return -1;
        }
        else if((activeBtn.equals("in") | activeBtn.equals("out")) & place.equals("Retailer") & retailerCode.equals(""))
        {
            CustomUtility.showWarning(this,"Select retail code","Required feild!");
            return -1;
        }
        else if(!place.equals("Retail Point") & !retailerCode.equals(""))
        {
            retailerCode = "";
        }
        else if(place.equals("Retail Point") & retailerCode.equals("N/A"))
        {
            CustomUtility.showWarning(this,"You have no retailer assigned select another retail option","Required feild!");
            return -1;
        }
        else if(presentAcc.equals("") | presentLat.equals("") | presentLon.equals(""))
        {
            CustomUtility.showWarning(this,"Turn on location setting","Required feild!");
            return -1;
        }
        else if((activeBtn.equals("in") | activeBtn.equals("out") ) & !photoFlag)
        {
            CustomUtility.showWarning(this,"Take selfie","Required feild!");
            return -1;
        }
        else if(activeBtn.equals("leave") & (fromDate.equals("") | toDate.equals("")))
        {
            CustomUtility.showWarning(this,"Select form and to date","Required feild!");
            return -1;
        }
        else if(activeBtn.equals("leave") & leaveType.equals(""))
        {
            CustomUtility.showWarning(this, "Select leave type","Required feild!");
            return -1;
        }
        else if(activeBtn.equals("leave") & reason.equals(""))
        {
            CustomUtility.showWarning(this,"Write the reason","Required feild!");
            return -1;
        }
        return 0;
    }

    public void upload()
    {
        if(activeBtn.equals("in")|activeBtn.equals("out"))
        {
            Uri uri = Uri.fromFile(new File(currentPhotoPath));
            try{
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                progressDialog.dismiss();
                String err = e.getMessage() + " May be storage full please uninstall then install the app again";
                CustomUtility.showAlert(this, e.getMessage(), "Problem Creating Bitmap at Submit");
                return;
            }
            imageString = CustomUtility.imageToString(bitmap);
        }
        String upLoadServerUri = "https://atmdbd.com/ce/api/android/attendance_panel1.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, upLoadServerUri,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Log.e("response",response);
                        try {
                            jsonObject = new JSONObject(response);
                            String code = jsonObject.getString("success");
                            String message = jsonObject.getString("message");
                            if(code.equals("true"))
                            {
                                code = "Successful";
                                deleteTakenPicture();
                                new SweetAlertDialog(AttendanceActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                                        .setTitleText("Successful")
                                        .setConfirmText("Ok")
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sDialog) {
                                                sDialog.dismissWithAnimation();
                                                finish();
                                                startActivity(getIntent());
                                            }
                                        })
                                        .show();
                            }
                            else
                            {
                                code = "Failed";
                                CustomUtility.showError(AttendanceActivity.this,message,code);
                            }


                        } catch (JSONException e) {
                            CustomUtility.showError(AttendanceActivity.this, e.getMessage(), "Failed");
                        }
                    }
                }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Log.e("response","onerrorResponse");
                CustomUtility.showError(AttendanceActivity.this, "Network slow, try again", "Failed");
            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("EmployeeId",IDbundle.getString("empid"));
                params.put("CreatedBy",IDbundle.getString("id"));
                params.put("EmployeeTypeId",IDbundle.getString("typeId"));
                params.put("AttendanceType",activeBtn);
                params.put("PlaceType",place);
                params.put("RetailerCode",retailerCode);
                params.put("LatValue",presentLat);
                params.put("LonValue",presentLon);
                params.put("GeoAccuracy",presentAcc);
                params.put("PictureSelfieData",imageString);
                params.put("DeviceDate",deviceDate);
                params.put("LeaveType",leaveType);
                params.put("LeaveDateStart",fromDate);
                params.put("LeaveDateEnd",toDate);
                params.put("Reason",reason);
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingleton.getInstance(this).addToRequestQue(stringRequest);
    }

    private void deleteTakenPicture() {
        File fdelete = new File(currentPhotoPath);
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                System.out.println("file Deleted :" + currentPhotoPath);
            } else {
                System.out.println("file not Deleted :" + currentPhotoPath);
            }
        }
    }

    private void getRetailList() {

        String upLoadServerUri="https://atmdbd.com/ce/api/android/get_retailer_list.php";
        StringRequest stringRequest=new StringRequest(Request.Method.POST, upLoadServerUri,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            progressDialog.dismiss();
                            Log.e("Server Response",response);
                            JSONObject jo = new JSONObject(response);
                            String Code;
                            String code = jo.getString("code");
                            String message = jo.getString("message");

                            if(code.equals("true"))
                            {
                                String retailersDetailsesponse = jo.getString("retailersDetails");
                                JSONArray jsonArr = new JSONArray(retailersDetailsesponse);

                                for (int i = 0; i < jsonArr.length(); i++) {
                                    JSONObject jsonObj = jsonArr.getJSONObject(i);
                                    Code=jsonObj.getString("retailCode");
                                    DMSCode.add(Code);
                                }
                                showDMSCode();

                            }else {
                                CustomUtility.showError(AttendanceActivity.this,"No retail DMS found by your id",message);
                            }

                        } catch (JSONException e) {
                            CustomUtility.showError(AttendanceActivity.this,e.getMessage(),"Failed");
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                CustomUtility.showError(AttendanceActivity.this,"","Network Error Try Again");
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params=new HashMap<>();
                //params.put("empId",IDbundle.getString("id"));
                params.put("empId",IDbundle.getString("empid"));
                return params;
            }
        };

        MySingleton.getInstance(AttendanceActivity.this).addToRequestQue(stringRequest);
    }

    public void showDMSCode()
    {
        ArrayAdapter<String> adapter5 = new ArrayAdapter<String>(this, R.layout.spinner_item, DMSCode);
        retailerSpinner.setAdapter(adapter5);
    }

    private void updateFromDate() {
        String myFormat = "yyyy-MM-dd"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        Log.e("FromdDate",sdf.format(myCalendar.getTime()));
        fromDate = sdf.format(myCalendar.getTime());
        textFrom.setText(sdf.format(myCalendar.getTime()));
    }

    private void updateToDate() {
        String myFormat = "yyyy-MM-dd"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        toDate = sdf.format(myCalendar2.getTime());
        textTo.setText(sdf.format(myCalendar2.getTime()));
    }

    public String getDeviceDate()
    {
        String myFormat = "yyyy-MM-dd H:m:s"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        Date date = new Date();
        return  sdf.format(date);
    }

    private File createImageFile() throws IOException {

        File storageDir = getExternalFilesDir("LegionCE/Photos");

        File image = new File(storageDir.getAbsolutePath() + File.separator + photoName);
        try {
            image.createNewFile();
        } catch (IOException e) {
            CustomUtility.showAlert(this, "Image Creation Failed. Please contact administrator", "Error");
        }
        currentPhotoPath = image.getAbsolutePath();
        Log.e("image path",currentPhotoPath);
        return image;
    }

    private void GPS_Start() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            listener = new GPSLocationListener();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
        } catch (Exception ex) {

        }
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public class GPSLocationListener implements LocationListener {
        public void onLocationChanged(final Location loc) {
            Log.i("**********", "Location changed");
            if (isBetterLocation(loc, previousBestLocation)) {


                loc.getAccuracy();
                location.setText(" " + loc.getAccuracy());

                presentLat = String.valueOf(loc.getLatitude());
                presentLon = String.valueOf(loc.getLongitude());
                presentAcc = String.valueOf(loc.getAccuracy());


//                Toast.makeText(context, "Latitude" + loc.getLatitude() + "\nLongitude" + loc.getLongitude(), Toast.LENGTH_SHORT).show();
                intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Provider", loc.getProvider());
                sendBroadcast(intent);
            }
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Toast.makeText(getApplicationContext(), "Status Changed", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onBackPressed() {
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Closing the application")
                .setContentText("Are you sure you want to close this application?")
                .setConfirmText("Yes")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        finish();
                    }
                })
                .setCancelText("No")
                .show();
    }

}
