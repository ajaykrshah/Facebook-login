package com.example.ajay.facebooklogin;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements LocationListener {

    TextView mtextViewDetails,mtextViewLocation;
    LoginButton mbuttonlogin;
    CallbackManager callbackManager;
    ProfilePictureView mprofilePictureViewImage;
    LocationManager locationManager;
    double latitude=0,longitude=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * Initializing the facebook sdk and callbackManager for facebook login.
         */
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        initializer();
        login();

        /**
         * This is used for trscking the token and perform logout event if user logout.
         */
        AccessTokenTracker accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {

                if (currentAccessToken == null){
                    mtextViewDetails.setText("");
                    mprofilePictureViewImage.setVisibility(View.INVISIBLE);

                }
            }
        };
    }


    /**
     * @author ajay
     * @date 05-09-2018
     * @purpose To initialize the components for the login Activity.
     */
    private void initializer(){

        mtextViewDetails = (TextView) findViewById(R.id.tv_details);
        mtextViewLocation =(TextView) findViewById(R.id.tv_location);
        mbuttonlogin = (LoginButton) findViewById(R.id.btn_fblogin);
        mprofilePictureViewImage = (ProfilePictureView) findViewById(R.id.view_profile);
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

        }

    }


    /**
     * @author ajay
     * @date 05-09-2018
     * @purpose This method is used to perform the login integration with facebook and
     *  handling the login events such as success, failed or cancel.
     */

    private void login(){
        mtextViewDetails.setVisibility(View.VISIBLE);
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {

                                try {
                                    String id = object.getString("id");
                                    String email = object.getString("email");
                                    String name = object.getString("name");

                                    mprofilePictureViewImage.setVisibility(View.VISIBLE);
                                    mtextViewDetails.setText("Facebook ID : "+id+"\n"+
                                            "Email : "+email+"\n"+
                                            "Name : "+name);
                                    mprofilePictureViewImage.setPresetSize(ProfilePictureView.NORMAL);
                                    mprofilePictureViewImage.setProfileId(object.getString("id"));


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id, name, email, gender, birthday");
                request.setParameters(parameters);
                request.executeAsync();

                //Call for gps location
                getLocation();
            }

            @Override
            public void onCancel() {
                mtextViewDetails.setText("Login Cancelled.");
            }

            @Override
            public void onError(FacebookException error) {
                mtextViewDetails.setText("Login error:"+error.getMessage());
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        // MessageDialog.show(MainActivity.this, content);
    }


    /**
     * getLocation is used to provide the location by using geocoding API and display that location
     * in textView.
     */
    private void getLocation() {

        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);
            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="+latitude+","+
                    longitude+"&key="+"AIzaSyDQ8uqhT9jUlc5OspTaufkhM8f6wDe7OHE";

             // Calling GeoCoding API with location latitude,longitude and API key.
            networkCallForGeoCodingAPI(url);
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * @use To perform network call with geoCoding API
     * @param url : utrl for geoCoding API with lattitude and longitude and API key.
     */
    private void networkCallForGeoCodingAPI(String url){
        RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                /**
                 * parseJsonFromAPI isa used to parse the result from json format
                 */
                try {
                    parseJsonFromAPI(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                    mtextViewLocation.setText("You have used you daily quota");
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        mRequestQueue.add(mStringRequest);
    }


    /**
     * @use It is used to provide the formatted_address from the Json
     * @param result    response string from the API call
     * @throws JSONException JsonException that can be occuor in API call.
     */
    private void parseJsonFromAPI(String result) throws JSONException {
        JSONObject objectResult = new JSONObject(result);
        JSONArray resultArray = objectResult.getJSONArray("result");
        String location = resultArray.getJSONObject(0).getString("formatted_address");
        if(objectResult.getString("status")=="OK"){
                mtextViewLocation.setText(location);
        }


    }



    @Override
    public void onLocationChanged(Location location) {
        latitude =  location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
