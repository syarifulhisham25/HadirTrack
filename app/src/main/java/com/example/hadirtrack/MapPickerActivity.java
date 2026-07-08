package com.example.hadirtrack;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap googleMap;

    EditText searchLocationInput;
    Button searchLocationButton, confirmLocationButton;
    TextView selectedLocationText;

    double selectedLatitude = 0.0;
    double selectedLongitude = 0.0;
    boolean locationSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        searchLocationInput = findViewById(R.id.searchLocationInput);
        searchLocationButton = findViewById(R.id.searchLocationButton);
        selectedLocationText = findViewById(R.id.selectedLocationText);
        confirmLocationButton = findViewById(R.id.confirmLocationButton);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        searchLocationButton.setOnClickListener(v -> searchLocation());

        confirmLocationButton.setOnClickListener(v -> {
            if (!locationSelected) {
                Toast.makeText(this, "Please select location first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedLatitude);
            resultIntent.putExtra("longitude", selectedLongitude);

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        double initialLat = getIntent().getDoubleExtra("currentLatitude", 3.0738);
        double initialLng = getIntent().getDoubleExtra("currentLongitude", 101.5183);

        LatLng initialLocation = new LatLng(initialLat, initialLng);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 16));

        googleMap.setOnMapClickListener(latLng -> {
            setSelectedLocation(latLng, "Selected Class Location");
        });
    }

    private void searchLocation() {
        String locationName = searchLocationInput.getText().toString().trim();

        if (locationName.isEmpty()) {
            searchLocationInput.setError("Enter location name");
            return;
        }

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);

            if (addresses == null || addresses.isEmpty()) {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Address address = addresses.get(0);

            LatLng searchedLocation = new LatLng(
                    address.getLatitude(),
                    address.getLongitude()
            );

            String markerTitle = address.getFeatureName() != null
                    ? address.getFeatureName()
                    : locationName;

            setSelectedLocation(searchedLocation, markerTitle);

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchedLocation, 17));

        } catch (Exception e) {
            Toast.makeText(this, "Failed to search location: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setSelectedLocation(LatLng latLng, String title) {
        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;
        locationSelected = true;

        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title));

        selectedLocationText.setText(
                "Selected: " + selectedLatitude + ", " + selectedLongitude
        );
    }
}