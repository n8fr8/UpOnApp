package info.guardianproject.upon;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

public class UpOnActivity extends Activity {

	public final static String TAG = "UpOn";
	
	private ToggleButton btn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.main);
		
		startService(new Intent(this,UpOnService.class));
		bindService();
		
		/*
		btn = (ToggleButton)findViewById(R.id.toggleButton1);
		btn.setOnClickListener (new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				
				boolean isChecked = btn.isChecked();
				
				if (isChecked)
				{
				}
				
			}
			
		});*/
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
		unbindService();
	}
	
	 /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	
        	Log.d(TAG,"service connected: " + className.getClassName());
        	
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
          //  mService = ITorService.Stub.asInterface(service);
       
            
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
              //  mService.registerCallback(mCallback);
           
            
            } catch (Exception e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            	Log.d(TAG,"error registering callback to service",e);
            }
       
          
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
          //  mService = null;
        	
        	Log.d(TAG,"service disconnected: " + className.getClassName());
          
        }
    };
    
    boolean mIsBound = false;
    
    private void bindService ()
    {
    	 bindService(new Intent(UpOnService.class.getName()),
                 mConnection, Context.BIND_AUTO_CREATE);
    	 
    	 mIsBound = true;
    
    }
    
    private void unbindService ()
    {
    	if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
    		/*
            if (mService != null) {
                try {
                    mService.unregisterCallback(mCallback);
                    
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }*/
            
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            
        }
    }

	
}
