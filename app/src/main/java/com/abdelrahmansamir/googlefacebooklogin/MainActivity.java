package com.abdelrahmansamir.googlefacebooklogin;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {


    CallbackManager callbackManager;
    AccessTokenTracker accessTokenTracker;
    Profile profile;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInOptions gso;
    private static final int RC_SIGN_IN = 169;
    String userName, userEmail, userID, userType, userPhoto;

    LoginButton loginButton;
    SignInButton signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.setApplicationId("361892290953430");
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_main);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        try {

            PackageInfo info = getPackageManager().getPackageInfo("com.abdelrahmansamir.googlefacebooklogin", PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {

                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.e("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));

            }

        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {

        }

        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("public_profile");
        loginButton.setReadPermissions("email");

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                Log.e("ID", loginResult.getAccessToken().getToken());

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        Log.e("Facebook Login", response.toString());

                        // Get facebook data from login

                        try {

                            userName = object.getString("first_name") + " " + object.getString("last_name");
                            userEmail = object.getString("email");
                            userID = object.getString("id");
                            userType = "Facebook";

                            Log.e("User Info ", userName + " " + userEmail + " " + userID + " " + userType);

                            try {

                                userPhoto = (new URL("https://graph.facebook.com/" + object.getString("id") + "/picture?width=200&height=250")).toString();
                                Log.e("UserPhoto", userPhoto);

                            } catch (MalformedURLException e) {
                                userPhoto = null;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        LoginManager.getInstance().logOut();

                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,first_name,last_name,email,gender,birthday,location");
                request.setParameters(parameters);
                request.executeAsync();

                accessTokenTracker = new AccessTokenTracker() {
                    @Override
                    protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                        Profile.fetchProfileForCurrentAccessToken();
                        AccessToken.setCurrentAccessToken(currentAccessToken);
                    }
                };
                accessTokenTracker.startTracking();

            }

            @Override
            public void onCancel() {
                Log.e("Facebook Cancel", "Cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e("Facebook Error", error.getMessage());
            }
        });


        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(gso.getScopeArray());
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

    }

    @Override
    protected void onStop() {

        super.onStop();

        try {
            accessTokenTracker.stopTracking();
        } catch (NullPointerException e) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // if Facebook login CallBack

        callbackManager.onActivityResult(requestCode, resultCode, data);


        // if Google login CallBack

        if (requestCode == RC_SIGN_IN) {

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()) {

                GoogleSignInAccount acct = result.getSignInAccount();

                // Get account information


                userName = acct.getDisplayName();
                userEmail = acct.getEmail();
                userID = acct.getId();

                userType = "Google";

                Log.e("User Info ", userName + " " + userEmail + " " + userID + " " + userType);

                try {
                    userPhoto = acct.getPhotoUrl().toString();
                    Log.e("userPhoto", userPhoto);
                } catch (Exception e) {
                    userPhoto = null;
                }


            } else {

                Log.e("Google Error", "Connection Error");

            }

            signOut();

        }
    }


    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                    }
                });
    }


    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                    }
                });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("Google Error", connectionResult.getErrorMessage());
    }
}
