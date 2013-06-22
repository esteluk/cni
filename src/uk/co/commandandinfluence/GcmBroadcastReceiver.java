package uk.co.commandandinfluence;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmBroadcastReceiver extends BroadcastReceiver {

	static final String TAG = "GcmBroadcastReceiver";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    Context ctx;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        ctx = context;
        String messageType = gcm.getMessageType(intent);
        
        sendNotification(intent.getExtras(), messageType);
        
        setResultCode(Activity.RESULT_OK);
    }

    // Put the GCM message into a notification and post it.
    private void sendNotification(Bundle extras, String messageType) {
    	Log.d("GcmBroadcastReceiver", "sendNotification");
        mNotificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        String msg = extras.getString("message");
        
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, AuthActivity.class), 0);

        
	    Notification noti = new Notification.BigPictureStyle(
	    	      new Notification.Builder(ctx)
	    	         .setContentTitle("Your commander needs you!")
	    	         .setSmallIcon(R.drawable.ic_cloud)
	    	         .addAction(R.drawable.ic_cancel, "Decline", contentIntent)
	    	         .addAction(R.drawable.ic_accept, "Accept", contentIntent)
	    	         .setAutoCancel(true)
	    	         .setContentIntent(contentIntent))
	    	         //.setLargeIcon(aBitmap))
	    	      .bigPicture(BitmapFactory.decodeResource(ctx.getResources(), R.drawable.me))
	    	      .setSummaryText(msg)
	    	      .build();
	    	 
	    mNotificationManager.notify(NOTIFICATION_ID, noti);
    }
}
