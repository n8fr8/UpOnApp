package info.guardianproject.upon;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class UpOnService extends Service 
{
 
    SensorManager sensorManager;
    Sensor accelerometerSensor;
  	boolean accelerometerPresent;

  	boolean lastState = false;
  	
  	private float ySwitchPoint = -5f;
  	private float zSwitchPoint = 3f;
  	
	private static final int NOTIFY_ID = 007;

  	
  	 TelephonyManager mTelephonyMgr;
  	 
		MediaRecorder recorder;
		
  	 private int _callState = TelephonyManager.CALL_STATE_IDLE;
  	 
	  /** Called when the activity is first created. */
	 
  	 private void init ()
  	 {
	     
	      //setContentView(R.layout.main);
	      

	       
	        mTelephonyMgr = (TelephonyManager)
	                getSystemService(Context.TELEPHONY_SERVICE); 
	        
	        mTelephonyMgr.listen(new PhoneStateListener()
	        {
	        	public void onCallStateChanged(int state, String incomingNumber)
	        	{
	        		_callState = state;
	        	}
	        	
	        }, PhoneStateListener.LISTEN_CALL_STATE);
	    
	    
	      sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
	      List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
	      if(sensorList.size() > 0){
	       accelerometerPresent = true;
	       accelerometerSensor = sensorList.get(0);  
	      }
	      else{
	       accelerometerPresent = false;  
	      // face.setText("No accelerometer present!");
	      }
	  }

  	 @Override
	public void onCreate() {
		
		super.onCreate();
		
		init();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopSensor();
		goDark(false);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		startSensor();
	}

	private void startSensor()
  	 {
		  if(accelerometerPresent){
			   sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);  
			  }
	 }

	 private void stopSensor ()
	 {
		  if(accelerometerPresent){
		   sensorManager.unregisterListener(accelerometerListener);  
		  }
	 }
 
	 private SensorEventListener accelerometerListener = new SensorEventListener(){
	
	  @Override
	  public void onAccuracyChanged(Sensor arg0, int arg1) {
	  
	   
	  }
	
	  @Override
	  public void onSensorChanged(SensorEvent arg0) {

		   float y_value = arg0.values[1];

		   float z_value = arg0.values[2];
		   
		   boolean isOfflinePosition = false;
		   String msg = "You are online";
		   
		   Log.d("onup", "y,z value: " + y_value + "," + z_value);
		   
		   if (z_value < zSwitchPoint)
		   {   
			   isOfflinePosition = true;			   
			   msg = "You are offline";
		   }
		   else if (y_value <= ySwitchPoint){
			   isOfflinePosition = true;			   
			   msg = "You are offline";
		   }
			
		   if (isOfflinePosition != lastState && _callState == TelephonyManager.CALL_STATE_IDLE)
		   {
			   showToolbarNotification(msg, R.drawable.icon);
			   
			   goDark (isOfflinePosition);
			 //  lockMic(thisState);
			   lastState = isOfflinePosition;
		   }
	  }
	  
	};
	
	private void goDark (boolean isEnabled)
	{
		
		//Toast.makeText(this, "setting airplane: " + isEnabled, Toast.LENGTH_SHORT).show();
		// toggle airplane mode
		Settings.System.putInt(
		      this.getContentResolver(),
		      Settings.System.AIRPLANE_MODE_ON, isEnabled ? 1 : 0);

		//Settings.System.AIRPLANE_MODE_RADIOS
		
		//Settings.System.
		
		
		// Post an intent to reload
		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", !isEnabled);
		sendBroadcast(intent);
	}
	
	private boolean isAirplane ()
	{
		return Settings.System.getInt(
			      this.getContentResolver(), 
			      Settings.System.AIRPLANE_MODE_ON, 0) == 1;
	}

	
	private void notifyAirplane ()
	{
		IntentFilter intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");

		BroadcastReceiver receiver = new BroadcastReceiver() {
		      @Override
		      public void onReceive(Context context, Intent intent) {
		            Log.d("AirplaneMode", "Service state changed: " + isAirplane());
		      }
		};

		this.registerReceiver(receiver, intentFilter);
	}
	
	private void lockMic (boolean doLock)
	{
		if (recorder != null)
		{
			// Stop and tidy up
			recorder.stop();
			recorder.release();
		}
		
		if (doLock)
		{
			if (recorder == null)
				recorder = new MediaRecorder();
	
			// Prepare recorder source and type
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	
			// File to which audio should be recorded
			File outputFile = getFileStreamPath("foo");
			recorder.setOutputFile(outputFile.getAbsolutePath());
	
			// Get ready!
			try {
				recorder.prepare();
		
				// Start recording
				recorder.start();
		
				
			
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {

		startSensor();
		return null;
	}
	
	private void showToolbarNotification (String notifyMsg, int icon)
	{

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		
		CharSequence tickerText = notifyMsg;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = getString(R.string.app_name);
		CharSequence contentText = notifyMsg;
		
		Intent notificationIntent = new Intent(this, UpOnActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);


		mNotificationManager.notify(NOTIFY_ID, notification);


	}
    
}
