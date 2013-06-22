package uk.co.commandandinfluence;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.cloudbase.CBHelper;
import com.cloudbase.CBHelperResponder;
import com.cloudbase.CBHelperResponse;
import com.cloudbase.CBQueuedRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.PlusClient;

public class AuthActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener, OnClickListener, CBHelperResponder {

	private static final int REQUEST_CODE_RESOLVE_ERR = 9000;

    private ProgressDialog mConnectionProgressDialog;
    private PlusClient mPlusClient;
    private ConnectionResult mConnectionResult;
    
    private static final String TAG = "AuthActivity";
    
    CBHelper cbHelper;
    
    // GCM constants
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME =
            "onServerExpirationTimeMs";
    /**
     * Default lifespan (100 days) of a reservation until it is considered expired.
     */
    public static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 100;
    
    String SENDER_ID = "455450713084";
    
    GoogleCloudMessaging gcm;
    Context applicationContext;
    String regId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_auth);
		
		mPlusClient = new PlusClient.Builder(this, this, this)
		        .setVisibleActivities("http://schemas.google.com/AddActivity", "http://schemas.google.com/BuyActivity")
		        .build();
		
		// Init cloudbase link
		cbHelper = new CBHelper("commandandinfluence", getString(R.string.cloudbase_secret_key), this);
		cbHelper.setPassword(md5(getString(R.string.cloudbase_app_password)));
		
		// Set on click listener for button
		findViewById(R.id.auth_signinbutton).setOnClickListener(this);
		
		// Progress bar to be displayed if the connection failure is not resolved.
		mConnectionProgressDialog = new ProgressDialog(this);
		mConnectionProgressDialog.setMessage("Signing in...");
		
		// Check if GCMd
		applicationContext = getApplicationContext();
		regId = getRegistrationId(applicationContext);
		
		if (regId.length() == 0) {
			registerBackground();
		} else {
			Log.d(TAG, regId);
		}
		gcm = GoogleCloudMessaging.getInstance(this);
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		mPlusClient.connect();
	}
	
	@Override
	protected void onStop() {
		mPlusClient.disconnect();
		
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.auth, menu);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
	    if (requestCode == REQUEST_CODE_RESOLVE_ERR && responseCode == RESULT_OK) {
	        mConnectionResult = null;
	        mPlusClient.connect();
	    }
	}
	
	@Override
	public void onClick(View view) {
	    if (view.getId() == R.id.auth_signinbutton && !mPlusClient.isConnected()) {
	        if (mConnectionResult == null) {
	            mConnectionProgressDialog.show();
	        } else {
	            try {
	                mConnectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
	            } catch (SendIntentException e) {
	                // Try connecting again.
	                mConnectionResult = null;
	                mPlusClient.connect();
	            }
	        }
	    }
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
	       if (mConnectionProgressDialog.isShowing()) {
	               // The user clicked the sign-in button already. Start to resolve
	               // connection errors. Wait until onConnected() to dismiss the
	               // connection dialog.
	               if (result.hasResolution()) {
	                       try {
	                               result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
	                       } catch (SendIntentException e) {
	                               mPlusClient.connect();
	                       }
	               }
	       }

	       // Save the intent so that we can start an activity when the user clicks
	       // the sign-in button.
	       mConnectionResult = result;
	}

	@Override
	public void onConnected(Bundle connectionHint) {
	 // We've resolved any connection errors.
		mConnectionProgressDialog.dismiss();
	    Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onDisconnected() {
		Log.d(TAG, "Disconnected!");
	}
	
	/**
	 * Gets the current registration id for application on GCM service.
	 * <p>
	 * If result is empty, the registration has failed.
	 *
	 * @return registration id, or empty string if the registration is not
	 *         complete.
	 */
	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.length() == 0) {
	        Log.v(TAG, "Registration not found.");
	        return "";
	    }
	    // check if app was updated; if so, it must clear registration id to
	    // avoid a race condition if GCM sends a message
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion || isRegistrationExpired()) {
	        Log.v(TAG, "App version changed or registration expired.");
	        return "";
	    }
	    return registrationId;
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
	    return getSharedPreferences(AuthActivity.class.getSimpleName(), 
	            Context.MODE_PRIVATE);
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}

	/**
	 * Checks if the registration has expired.
	 *
	 * <p>To avoid the scenario where the device sends the registration to the
	 * server but the server loses it, the app developer may choose to re-register
	 * after REGISTRATION_EXPIRY_TIME_MS.
	 *
	 * @return true if the registration has expired.
	 */
	private boolean isRegistrationExpired() {
	    final SharedPreferences prefs = getGCMPreferences(applicationContext);
	    // checks if the information is not stale
	    long expirationTime =
	            prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
	    return System.currentTimeMillis() > expirationTime;
	}
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration id, app versionCode, and expiration time in the 
	 * application's shared preferences.
	 */
	private void registerBackground() {
	    new AsyncTask<Void, Integer, String>() {
	        @Override
			protected String doInBackground(Void... params) {
	            String msg = "";
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(applicationContext);
	                }
	                regId = gcm.register(SENDER_ID);
	                msg = "Device registered, registration id=" + regId;

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.

	                // For this demo: we don't need to send it because the device
	                // will send upstream messages to a server that echo back the message
	                // using the 'from' address in the message.

	                // Save the regid - no need to register again.
	                setRegistrationId(applicationContext, regId);
	                cbHelper.notificationSubscribeDevice(regId, "user", false);
	        		
	            } catch (IOException ex) {
	                msg = "Error :" + ex.getMessage();
	            }
	            return msg;
	        }

	        @Override
	        protected void onPostExecute(String msg) {
	        	Log.d(TAG, msg);
	        }
	    }.execute(null, null, null);
	}

	/**
	 * Stores the registration id, app versionCode, and expiration time in the
	 * application's {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration id
	 */
	private void setRegistrationId(Context context, String regId) {
		
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.v(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;

	    Log.v(TAG, "Setting registration expiry time to " +
	            new Timestamp(expirationTime));
	    editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
	    editor.commit();
	}
	
	private static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
            	hexString.append(String.format("%02x", messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

	@Override
	public void handleResponse(CBQueuedRequest req, CBHelperResponse res) {
		// TODO Auto-generated method stub
		Log.v("logTag", (res.isSuccess()?"OK":"FAILED"));
		Log.v("logTag", res.getResponseDataString());
		Log.d(TAG, res.getErrorMessage());
	}
}
