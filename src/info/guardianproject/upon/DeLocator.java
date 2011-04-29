package info.guardianproject.upon;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;

public class DeLocator implements Runnable
{

	LocationManager lm;
	private boolean keepRunning = false;
	
    public DeLocator(Context context) {
        
        lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        
    }
    
    
    
    public void run ()
    {
    	Looper.prepare();
    	
    	while (keepRunning)
    	{
    		doIt();
    		
    		try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }

    public void start ()
    {

    	keepRunning = true;
        new Thread(this).start();
        
    }
    
    public void stop ()
    {
    	keepRunning = false;
    }

	private void doIt ()
    {

    	lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {       
    	            @Override
    	            public void onStatusChanged(String provider, int status, Bundle extras) {}
    	            @Override
    	            public void onProviderEnabled(String provider) {}
    	            @Override
    	            public void onProviderDisabled(String provider) {}
    	            @Override
    	            public void onLocationChanged(Location location) {}
    	});
    	            
    	/* Set a mock location for debugging purposes */
    	setMockLocation(15.387653, 73.872585, 500);
    }
    
    private void setMockLocation(double latitude, double longitude, float accuracy) {
    	
        lm.addTestProvider (LocationManager.GPS_PROVIDER,
                            "requiresNetwork" == "",
                            "requiresSatellite" == "",
                            "requiresCell" == "",
                            "hasMonetaryCost" == "",
                            "supportsAltitude" == "",
                            "supportsSpeed" == "",
                            "supportsBearing" == "",
                             android.location.Criteria.POWER_LOW,
                             android.location.Criteria.ACCURACY_FINE);      

        Location newLocation = new Location(LocationManager.GPS_PROVIDER);

        newLocation.setLatitude(latitude);
        newLocation.setLongitude(longitude);
        newLocation.setAccuracy(accuracy);

        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        lm.setTestProviderStatus(LocationManager.GPS_PROVIDER,
                                 LocationProvider.AVAILABLE,
                                 null,System.currentTimeMillis());    
      
        lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation);      

    }

}