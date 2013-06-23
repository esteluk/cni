package uk.co.commandandinfluence;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		final Intent intent = getIntent();
		String lat = intent.getStringExtra("lat");
		String lng = intent.getStringExtra("lng");
		
		LatLng pos = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
		
		GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_map)).getMap();
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		map.setMyLocationEnabled(true);
		map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
		
		if (!lat.equals("")) {
			map.addMarker(new MarkerOptions()
					.position(pos)
					.title("Target"));
		}
		
		findViewById(R.id.map_completebutton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent resultIntent = new Intent();
				resultIntent.putExtra("id", intent.getStringExtra("id"));
				resultIntent.putExtra("position", intent.getIntExtra("position", -1));
				setResult(RESULT_OK, resultIntent);
				finish();
			}
			
		});
		
		findViewById(R.id.map_failbutton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent failResultIntent = new Intent();
				failResultIntent.putExtra("id", intent.getStringExtra("id"));
				failResultIntent.putExtra("position", intent.getIntExtra("position", -1));
				setResult(RESULT_CANCELED, failResultIntent);
				finish();
			}
			
		});
				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

}
