package com.example.securityapplication;


import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.concurrent.Executor;

//import androidx.annotation.NonNull;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth mAuth;
    private TextView mStatus;
    private EditText mEmail;
    private EditText mPassword;
    private Button mSignInButton;

    //FaceBookLogin
    private LoginButton mFaceBookLoginButton;
    private CallbackManager callbackManager;
    private static final String EMAIL = "email";
    private FaceBookLoginIn faceBookLoginIn;

    //GoogleFirebaseSignIn
    private SignInButton mGoogleSignInButton;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleFirebaseSignIn googleFirebaseSignIn;
    private static final int RC_SIGN_IN = 9001;

    //persistent service
    private Intent mSosPlayerIntent;

    //Permissions request code
    int RC;

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if(i== R.id.signInButton){
            if(mAuth.getCurrentUser()==null)
                signIn(mEmail.getText().toString(),mPassword.getText().toString());
            else {
                signOut();
                updateUI(null);
            }
        }
        else if (i==R.id.googleSignInButton){
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
        else if (i==R.id.signUpButton){
            Intent signUpIntent = new Intent(this,SignUp1Activity.class);
            startActivity(signUpIntent);
        }
        //Added Reset Passowrd Activity
        else if (i==R.id.ForgetPassword){
            Intent forgetPasswordIntent = new Intent(this,ResetPasswordActivity.class);
            startActivity(forgetPasswordIntent);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEmail=findViewById(R.id.editEmail);
        mPassword=findViewById(R.id.editPassword);
        mStatus= findViewById(R.id.status);
        mSignInButton=findViewById(R.id.signInButton);
        mGoogleSignInButton=findViewById(R.id.googleSignInButton);
        FirebaseApp.initializeApp(this);
        mAuth=FirebaseAuth.getInstance();

        findViewById(R.id.signInButton).setOnClickListener(this);

        /**  GOOGLE LOGIN  **/

        findViewById(R.id.googleSignInButton).setOnClickListener(this);

        //get single instance of user if logged in through google from user defined class GoogleFirebaseSignIn
        googleFirebaseSignIn = GoogleFirebaseSignIn.getInstance();
        //initialize user defined class GoogleFirebaseSignIn with Firebase user instance and MainActivity using user defined init method
        googleFirebaseSignIn.init(this, mAuth);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        /**  END GOOGLE LOGIN  **/


        /**   FACEBOOK LOGIN   **/

        //get single instance of user if logged in through facebook from user defined class FaceBookLoginIn
        faceBookLoginIn = FaceBookLoginIn.getInstance();
        //initialize user defined class FaceBookLoginIn with Firebase user instance and MainActivity using user defined init method
        faceBookLoginIn.init(MainActivity.this, mAuth);

        callbackManager = CallbackManager.Factory.create();
        mFaceBookLoginButton = findViewById(R.id.facebook_login_button);
        mFaceBookLoginButton.setPermissions(Arrays.asList(EMAIL));

        // Callback registration
        mFaceBookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("FB", "facebook:onSuccess:" + loginResult);
                faceBookLoginIn.handleFacebookAccessToken(loginResult.getAccessToken());
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if(faceBookLoginIn.isLoggedIn())
                    updateUI(currentUser);
                else
                    updateUI(null);
            }

            @Override
            public void onCancel() {
                Log.d("FB", "facebook:onCancel");
                updateUI(null);
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d("FB", "facebook:onError", exception);
                updateUI(null);
            }
        });
        /**  END FACEBOOK LOGIN  **/

        /** SosPlayer Service intent**/
        mSosPlayerIntent=new Intent(this, SosPlayer .class);
        //checks if service is running and if not running then starts it
        if (!isMyServiceRunning(SosPlayer.class)){
            startService(mSosPlayerIntent);
        }

        //Log.d("MAinActivity","SMS intent");
        //check permissions

        while(!checkSMSPermission());

        if(!checkGPSPermission());

        Intent mGpsServiceIntent = new Intent(this, GetGPSCoordinates.class);
        startService(mGpsServiceIntent);

        if(!checkMicrophonePermission());

        Intent mSpeechToTextIntent = new Intent(this,SpeechToTextService.class);
        startService(mSpeechToTextIntent);
    }

    public boolean checkMicrophonePermission(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this,"Permission required for sending alert in case of SOS", Toast.LENGTH_LONG).show();
            Log.d("MainActivity","PERMISSION FOR MICROPHONE NOT GRANTED, REQUESTING PeRMISSION...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, RC);

        }
        return ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)== PackageManager.PERMISSION_GRANTED;
    }

    public  boolean checkSMSPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permission Required for sending SMS in case of SOS", Toast.LENGTH_LONG).show();
            Log.d("MainActivity", "PERMISSION FOR SEND SMS NOT GRANTED, REQUESTING PERMSISSION...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, RC);
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)== PackageManager.PERMISSION_GRANTED;
    }

    public boolean checkGPSPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permission Required for sending SMS in case of SOS", Toast.LENGTH_LONG).show();
            Log.d("MainActivity", "PERMISSION FOR FINE GPS NOT GRANTED, REQUESTING PERMSISSION...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, RC);
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permission Required for tracking Location in case of SOS", Toast.LENGTH_LONG).show();
            Log.d("MainActivity", "PERMISSION FOR FINE GPS NOT GRANTED, REQUESTING PERMSISSION...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, RC);
        }
        boolean coarse_loc_permission=ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED;
        boolean fine_loc_permission=ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED;

        return (coarse_loc_permission && fine_loc_permission);

    }

    public void onStart(){
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
    private boolean validateForm() {
        boolean valid = true;

        String email = mEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmail.setError("Required.");
            valid = false;
        } else {
            mEmail.setError(null);
        }

        String password = mPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPassword.setError("Required.");
            valid = false;
        } else {
            mPassword.setError(null);
        }

        return valid;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            googleFirebaseSignIn.handleSignInResult(task);
        }

        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void signIn(String email, String password){

        if(!validateForm()){
            return;
        }
        else {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(
                            this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        updateUI(user);
                                    } else {
                                        updateUI(null);
                                    }
                                }
                            }
                    );
        }
    }

    private void signOut(){
        //Firebase signOut
        mAuth.signOut();

        //Google signOut
        if(GoogleSignIn.getLastSignedInAccount(this) != null) {
            mGoogleSignInClient.signOut()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // [START_EXCLUDE]
                            //updateUI(null);
                            // [END_EXCLUDE]
                        }
                    });
        }

        //Facebook signOut
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedInFaceBook = accessToken != null && !accessToken.isExpired();
        if(isLoggedInFaceBook)
            LoginManager.getInstance().logOut();
    }

    public void updateUI(FirebaseUser user){
        if(user==null){
            mStatus.setText(R.string.not_logged);
            mSignInButton.setText(R.string.sign_in_text);
            mEmail.setVisibility(View.VISIBLE);
            mPassword.setVisibility(View.VISIBLE);
            mGoogleSignInButton.setVisibility(View.VISIBLE);
            mFaceBookLoginButton.setVisibility(View.VISIBLE);
        }
        else if(user!=null){
            mStatus.setText(R.string.logged);
            mSignInButton.setText(R.string.sign_out_text);
            mEmail.setVisibility(View.GONE);
            mPassword.setVisibility(View.GONE);
            mGoogleSignInButton.setVisibility(View.GONE);
            mFaceBookLoginButton.setVisibility(View.GONE);
        }
    }

    /**Checks whether service is running or not**/
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    @Override
    protected void onDestroy() {
        stopService(mSosPlayerIntent);
        Log.i("Mainactivity destroyed", "onDestroy!");
        super.onDestroy();

    }
}
