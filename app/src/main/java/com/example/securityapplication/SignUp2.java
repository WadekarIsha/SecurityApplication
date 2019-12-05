package com.example.securityapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;




import com.example.securityapplication.model.User;

import java.util.regex.Pattern;


public class SignUp2 extends AppCompatActivity {
    private final AppCompatActivity activity = SignUp2.this;

    private TextInputLayout mobile;
    private TextInputLayout aadhar;
    private TextInputLayout location;


    private TextInputEditText input_mobile;
    private TextInputEditText input_aadhar;
    private TextInputEditText input_location;
    private TextView error_message;
    private Button btn_submit;

    private InputValidation inputValidation;
    private  SQLiteDBHelper DBHelper;
    private User user;
    private String blockcharset = "~#^|$%&*!,.";
    //adding requestCode variable for requestPermission
    private int RC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup2);

        initViews();
        initListeners();
        initObjects();

        //mPlaceDetectionClient = Places.getPlaceDetectionClient(this);


    }

    /**Initialize Views*/
    private void initViews(){
       // mobile = findViewById(R.id.mobile);
       // aadhar = findViewById(R.id.aadhar);
       // location = findViewById(R.id.location);

        input_mobile = findViewById(R.id.input_mobile);
        input_aadhar = findViewById(R.id.input_aadhar);
        input_location = findViewById(R.id.input_location);

       // error_message = findViewById(R.id.error_message);
        btn_submit = findViewById(R.id.btn_submit);

    }

    private void initListeners(){
        btn_submit.setOnClickListener(new View.OnClickListener() {
           // @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                //Call the method to validate the fields
                boolean valid_mobile = false;
                boolean valid_aadhar = false;
                boolean valid_location = false;
                boolean empty_mobile = inputValidation.is_Empty(input_mobile, "PLEASE ENTER MOBILE NO. ");
                if (!empty_mobile) {
                    if (!inputValidation.isMinLength(input_mobile, 10, getString(R.string.no_phone)) || !inputValidation.is_numeric(input_mobile)) {

                        input_mobile.setError(getString(R.string.no_phone));

                    }
                     else {
                        valid_mobile = true;
                        input_mobile.setError(null);
                    }
                }

                boolean empty_aadhar = inputValidation.is_Empty(input_aadhar, getString(R.string.no_aadhar));
                if (!empty_aadhar) {
                    if (!inputValidation.isMinLength(input_aadhar, 12, getString(R.string.no_aadhar)) ||
                            !inputValidation.is_numeric(input_aadhar)) {

                                input_aadhar.setError(getString(R.string.no_aadhar));
                        }

                    else {
                        valid_aadhar = true;
                        input_aadhar.setError(null);
                    }
                }

                if (input_location.getText().toString().isEmpty()) {
                    input_location.setError(getString(R.string.no_location));
                }
                else{
                    valid_location = true;
                    input_location.setError(null);
                }
                /**NOTE:MOVED empty if statement here to ensure error messages are displayed before exiting
                 //if(empty){
                 //  return;

                 }*/

                //Moved IMEI reading code to this place so IMEI can be stored for user object
                boolean empty = inputValidation.all_Empty(input_mobile,input_aadhar,input_location,getString(R.string.message));

                Log.d("SIgnUP2"," Valid Aadhar:"+valid_aadhar+" Valid Mobile:"+valid_mobile+" Valid Location:"+valid_location);
                boolean valid = valid_mobile && valid_aadhar && valid_location;

                Log.d("SIgnUP2","Empty:"+empty+" Valid"+valid);

                //NOTE: Allow database entry only if not empty AND valid
                if(!empty && valid){
                    String imei = null;
                    TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                    String permission = Manifest.permission.READ_PHONE_STATE;
                    int res = getApplicationContext().checkCallingOrSelfPermission(permission);
                    if (res == PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            imei = tm.getImei(1);
                            Log.d("IMEI", "IMEI Number of slot 1 is:" + imei);

                        } else {
                            imei=tm.getDeviceId();
                            Log.d("SignUp2", "SDK Version not of required level");
                            Log.d("SignUp2", "Using getDeviceId()"+imei);

                        }
                    } else {
                        Log.d("SIgnUP2", "PERMISSION FOR READ STATE NOT GRANTED, REQUESTING PERMSISSION...");
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.READ_PHONE_STATE}, RC);

                    }


                    if (DBHelper.checkUser(input_aadhar.getText().toString().trim()) ){

                        user.setMobile(input_mobile.getText().toString().trim());
                        user.setAadhar(input_aadhar.getText().toString().trim());
                        user.setLocation(input_location.getText().toString().trim());
                        user.setImei(imei);//setting IMEI
                        //added conditional checking and showing respective Toast message
                        if (DBHelper.addUser(user))
                        {   Toast.makeText(getApplicationContext(), "YOU ARE NOW A SAVIOUR", Toast.LENGTH_LONG).show();
                            setResult(10,null);//to finish sing up 1 activity
                            activity.finish();}
                        else
                            Toast.makeText(getApplicationContext(), "SOMETHING WENT WRONG", Toast.LENGTH_LONG).show();

                    } else {
                        Log.d("SIgnUp2", "User exists ");
                        Toast.makeText(getApplicationContext(), "AADHAR NO ALREADY EXISTS", Toast.LENGTH_LONG).show();
                    }
                }//

                }
            //
            }

        );
    }

    private void initObjects(){
        inputValidation = new InputValidation(activity);
        DBHelper = new SQLiteDBHelper(activity);
        user = getIntent().getParcelableExtra("User"); //getting the User object from previous signup activity
    }
}
