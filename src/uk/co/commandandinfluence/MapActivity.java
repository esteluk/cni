package uk.co.commandandinfluence;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		
		GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_map)).getMap();
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		
		Intent intent = getIntent();
		String lat = intent.getStringExtra("lat");
		String lng = intent.getStringExtra("lng");
		
		if (!lat.equals("")) {
			map.addMarker(new MarkerOptions()
					.position(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)))
					.title("Target"));
		}
				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

}
