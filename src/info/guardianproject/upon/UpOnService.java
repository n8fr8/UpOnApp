package info.guardianproject.upon;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class UpOnService extends Service implements Runnable
{
 
    SensorManager sensorManager;
    Sensor accelerometerSensor;
  	boolean accelerometerPresent;

  	boolean lastState = false;
  	boolean keepPlayingNoise = false;
  	
  	private float ySwitchPoint = -5f;
  	private float zSwitchPoint = 2f;
  	
	private static final int NOTIFY_ID = 007;

	private Thread thread;
	private boolean keepRunning = true;
	
	private int checkInterval = 1000;
	
	  float y_value = 0;
	  float z_value = 0;
  	
  	 TelephonyManager mTelephonyMgr;
  	 
		
  	 private int _callState = TelephonyManager.CALL_STATE_IDLE;
  	 
  	Ringtone ringtoneUp;
  	Ringtone ringtoneDown;
  	
  	private DeLocator delocator;
  	
  	 private void init ()
  	 {
  		 
  		ringtoneDown = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
  		ringtoneUp = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
  		
  		 
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
	      
	      thread = new Thread (this);
	      
	      thread.start();
	      
	      delocator = new DeLocator(this);
	      delocator.start();
	      
	   //   notifyAirplane ();
	  }

  	 @Override
	public void onCreate() {
		
		super.onCreate();
		
		init();
	}
  	 
  	public void run ()
	  {
		  while (keepRunning)
		  {
			  
			  try { Thread.sleep(checkInterval);}
			  catch (Exception e){}

			  doTestState();
			  
		  }
	  }
  	
  	 public void doTestState ()
	  {
		   
		   boolean isOfflinePosition = false;
		   
		   Log.d("onup", "y,z value: " + y_value + "," + z_value);
		   
		   if (z_value < zSwitchPoint)
		   {   
			   isOfflinePosition = true;			   
			  
		   }
		   /*
		   else if (y_value <= ySwitchPoint){
			   isOfflinePosition = true;			   
			  
		   }*/
			
		   if (isOfflinePosition != lastState && _callState == TelephonyManager.CALL_STATE_IDLE)
		   {
			   
			   goDark (isOfflinePosition);
			 //  lockMic(thisState);
			   lastState = isOfflinePosition;
		   }
		  
	  }

	@Override
	public void onDestroy() {
		super.onDestroy();
		keepRunning = false;
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

		  
		  //smooth out code
		  
		   y_value = arg0.values[1];
		   z_value = arg0.values[2];
		   
	  }
	  
	 
	  
	};
	
	private void goDark (boolean isEnabled)
	{
		doNoticeAndNoise(isEnabled);
		
		//Toast.makeText(this, "setting airplane: " + isEnabled, Toast.LENGTH_SHORT).show();
		// toggle airplane mode
		Settings.System.putInt(
		      this.getContentResolver(),
		      Settings.System.AIRPLANE_MODE_ON, isEnabled ? 1 : 0);

		//Settings.System.AIRPLANE_MODE_RADIOS
		
		//Settings.System.
		if (receiver == null)
			notifyAirplane();
		
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

	BroadcastReceiver receiver;
	 boolean isAirplane;
	 
	private void notifyAirplane ()
	{
		IntentFilter intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");

		receiver = new BroadcastReceiver() 
		{
		      @Override
		      public void onReceive(Context context, Intent intent) {
		    	  Log.d("AirplaneMode", "Service state changed: " + isAirplane);
		    	  isAirplane = isAirplane();
		      }
		};

		registerReceiver(receiver, intentFilter);
	}

	private void doNoticeAndNoise (boolean isEnabled)
	{

      
        
        String msg = "Every move you make, they'll be watching you!";
		   int icon = R.drawable.rednotify;
		
		   if (isEnabled)
		   {
			  msg = "Your phone is no longer broadcasting your every whimsical move";
			  icon = R.drawable.greennotify;

			   
			   Thread thread = new Thread ()
			   {
				   public void run ()
				   {
					   //makeCancelNoise();
					   makeWhiteNoise();
				   }
				   
				  
			   };
			   
			   thread.start();
		   }
		   else
		   {
			  // ringtoneUp.play();
			  keepPlayingNoise = false;
		   }
	   showToolbarNotification(msg, icon);
  
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
	

	public AudioRecord audioRecord; 
	public int mSamplesRead; //how many samples read 
	public int buffersizebytes; 
	public int buflen; 
	public int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO; 
	public int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; 
	public static short[] buffer; //+-32767 
	public static final int SAMPPERSEC = 22050; //samp per sec 8000, 11025, 22050 44100 or 48000 
	
	 public void makeCancelNoise ()
	 {
		 
		keepPlayingNoise = true;
	
		buffersizebytes = AudioRecord.getMinBufferSize(SAMPPERSEC,channelConfiguration,audioEncoding); //4096 on ion 
		buffer = new short[buffersizebytes]; 
		buflen=buffersizebytes/2; 
		
		audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,SAMPPERSEC, 
		channelConfiguration,audioEncoding,buffersizebytes); //constructor 
		
		int playBufferSize = AudioTrack.getMinBufferSize(SAMPPERSEC, channelConfiguration, audioEncoding);
  		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,SAMPPERSEC,channelConfiguration,audioEncoding,playBufferSize,AudioTrack.MODE_STREAM);
  		
  		audioTrack.play();
  		
  	    long tim = System.currentTimeMillis(); 
  		
  		short cancelMod = -5;
  		
  		audioRecord.startRecording(); 
  		
  		short[] fingerprint = new short[SAMPPERSEC*4];
  		int fIdx = 0;
  		
  		while (keepPlayingNoise)
  		{
  			
  			mSamplesRead = audioRecord.read(buffer, 0, buffersizebytes); 

  			for (int i = 0; i < mSamplesRead; i++)
  			{
  				buffer[i] = (short)(buffer[i] * cancelMod);
  			}

  			audioTrack.write(buffer,0,mSamplesRead);
  			
  			/*
  			if (fIdx < fingerprint.length)
  				fingerprint[fIdx++] = mostFrequent(buffer);
  			
  			audioTrack.write(fingerprint,0,fIdx);
  			*/
  			

  		}
  		
  		audioRecord.stop();
  		audioTrack.stop();
  		
  	//	audioRecord.release();
	 }
	 
	 short mostFrequent(short... ary) {
		    Map<Short, Short> m = new HashMap<Short, Short>();

		    for (short a : ary) {
		        Short freq = m.get(a);
		        m.put(a, (freq == null) ? ((short)1) : (short)(freq + 1));
		    }

		    int max = -1;
		    short mostFrequent = -1;

		    for (Map.Entry<Short, Short> e : m.entrySet()) {
		        if (e.getValue() > max) {
		            mostFrequent = e.getKey();
		            max = e.getValue();
		        }
		    }

		    return mostFrequent;
		}
	 
	 public void makeWhiteNoise ()
	 {
		keepPlayingNoise = true;
	
  		int SAMPLE_RATE = 11025;
  		int minSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
  		
  		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,SAMPLE_RATE,AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);
  		
  		audioTrack.play();
  		
  	    long tim = System.currentTimeMillis(); 
        Random random = new Random(tim); 
  		
  		
  		while (keepPlayingNoise)
  		{
  			short[] buffer = {(short)random.nextInt()};
  			
  			audioTrack.write(buffer,0,buffer.length);
  			
  		}
	 }
    
}
