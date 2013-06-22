package uk.co.commandandinfluence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.Gson;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PresenceChannel;
import com.pusher.client.channel.PresenceChannelEventListener;
import com.pusher.client.channel.User;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.HttpAuthorizer;

public class MainActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	// Global constants
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 900;
	
	// Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
	
	// Global variable
	private LocationClient mLocationClient;
	private SharedPreferences mPrefs;
	private SharedPreferences.Editor mEditor;
	LocationRequest mLocationRequest;
	boolean mUpdatesRequested;
	Location mCurrentLocation;
	boolean mChannelSubscriptionState;
	PresenceChannel channel;
	
	private static final String TAG = "MainActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		HttpAuthorizer authoriser = new HttpAuthorizer("http://jakexks.com/pusher/auth.php");
		PusherOptions options = new PusherOptions().setAuthorizer(authoriser);
		
		Pusher pusher = new Pusher("7d3ebc72c0912d712cd6", options);
		
		final ListView list = (ListView) findViewById(R.id.main_list);
		
		final List<String> commands = new ArrayList<String>();
		
		/**
		 * Configure the list view to an array backup data
		 */
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, commands);
		list.setAdapter(adapter);
		
		// Connect
		pusher.connect(new ConnectionEventListener() {

			@Override
			public void onConnectionStateChange(ConnectionStateChange change) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(String message, String code, Exception e) {
				// TODO Auto-generated method stub
				
			}
			
		}, ConnectionState.ALL);
		
		mChannelSubscriptionState = false;
		
		// Subscribe to channel
		channel = pusher.subscribePresence("presence-game-1", new PresenceChannelEventListener() {

			@Override
			public void onAuthenticationFailure(String message, Exception e) {
				// TODO Auto-generated method stub
				Log.d(TAG, message);
				Log.d(TAG, e.getMessage());
			}

			@Override
			public void onSubscriptionSucceeded(String channelName) {
				Log.d(TAG, "subscriptionsucceeded YAY");
				mChannelSubscriptionState = true;
			}

			@Override
			public void onEvent(String channelName, String eventName, String data) {
				// I don't know what this is for.
			}

			@Override
			public void onUsersInformationReceived(String channelName, Set<User> users) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void userSubscribed(String channelName, User user) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void userUnsubscribed(String channelName, User user) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		channel.bind("client-command", new PresenceChannelEventListener() {

			@Override
			public void onAuthenticationFailure(String arg0, Exception arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSubscriptionSucceeded(String arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onEvent(String arg0, String arg1, String data) {
				
				// Do something!
				Gson gson = new Gson();
				final Command command = gson.fromJson(data, Command.class);
				
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						commands.add(0, command.command);
						adapter.notifyDataSetChanged();
					}
					
				});
			}

			@Override
			public void onUsersInformationReceived(String arg0, Set<User> arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void userSubscribed(String arg0, User arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void userUnsubscribed(String arg0, User arg1) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		
		/**
		 * Create location client
		 */
		if (!servicesConnected()) {
			return;
		}
		mLocationClient = new LocationClient(this, this, this);
		mLocationRequest = LocationRequest.create();
		// high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
		
		mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
		mEditor = mPrefs.edit();
		mUpdatesRequested = false;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		mLocationClient.connect();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		/*
         * Get any previous setting for location updates
         * Gets "false" if an error occurs
         */
        if (mPrefs.contains("KEY_UPDATES_ON")) {
            mUpdatesRequested =
                    mPrefs.getBoolean("KEY_UPDATES_ON", false);

        // Otherwise, turn off location updates
        } else {
            mEditor.putBoolean("KEY_UPDATES_ON", false);
            mEditor.commit();
        }
	}
	
	@Override
	protected void onPause() {
		mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
        mEditor.commit();
		
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		// If the client is connected
        if (mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
            mLocationClient.removeLocationUpdates(this);
        }
        /*
         * After disconnect() is called, the client is
         * considered "dead".
         */
        mLocationClient.disconnect();
        super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * A dialog fragment that displays an error dialog
	 */
	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;
		
		// Default constructor
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}
		
		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}
		
		// Return a dialog to the dialogfragment
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}
	
	/**
	 * Handle results returned to the FragmentActivity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {
		case CONNECTION_FAILURE_RESOLUTION_REQUEST :
			/*
			 * If the result code is Activity.OK, try again!
			 */
			switch (resultCode) {
			case Activity.RESULT_OK :
				/*
				 * Try the request again!
				 */
				break;
			}
		}
	}
	
	private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
        	ConnectionResult connectionResult = new ConnectionResult(resultCode, null);
            // Get the error code
            int errorCode = connectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(),
                        "Location Updates");
            }
            return false;
        }
    }
	

	/*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
        	// TODO
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }

	@Override
	public void onConnected(Bundle dataBundle) {
		// Yep
		Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
		mCurrentLocation = mLocationClient.getLastLocation();
		Log.d(TAG, mCurrentLocation.toString());
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	@Override
	public void onDisconnected() {
		// Nope!
		Toast.makeText(this, "Disconnected!", Toast.LENGTH_SHORT).show();
	}
	
	// Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        
        if (mChannelSubscriptionState) {
        	String user = mPrefs.getString("USER_ID", "");
        	channel.trigger("client-location", "{\"location\" : {\"latitude\" : \"" + location.getLatitude()
        			+ "\", \"longitude\" : \"" + location.getLongitude()  + "\"}, \"user\" : \"" + user + "\"}");
        }
    }
    
    /**
	 * A class for the Command object returned by Pusher
	 * 
	 * @author nathan
	 */
	class Command {
		private String command = "";
		
		Command() {
			
		}
	}
}
