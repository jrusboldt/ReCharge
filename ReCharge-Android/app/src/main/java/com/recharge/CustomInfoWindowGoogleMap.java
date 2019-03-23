package com.recharge;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowGoogleMap implements GoogleMap.InfoWindowAdapter {

    private Context context;

    public CustomInfoWindowGoogleMap(Context ctx) {
        context = ctx;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = ((Activity) context).getLayoutInflater()
                .inflate(R.layout.adapter_info_window, null);

        TextView name_tv = view.findViewById(R.id.name);
        TextView details_tv = view.findViewById(R.id.details);
        ImageView img = view.findViewById(R.id.image);

        TextView distance_tv = view.findViewById(R.id.distance);
        TextView chargingAvailability_tv = view.findViewById(R.id.charging_availability);
        TextView parkingAvailability_tv = view.findViewById(R.id.parking_availability);
        TextView isFree_tv = view.findViewById(R.id.isFree);

        name_tv.setText(marker.getTitle());
        details_tv.setText("Address: " + marker.getSnippet());

        InfoWindowData infoWindowData = (InfoWindowData) marker.getTag();

        int imageId = context.getResources().getIdentifier(infoWindowData.getImage().toLowerCase(),
                "drawable", context.getPackageName());
        //img.setImageResource(imageId);

        Double rounded = Math.round(infoWindowData.getDistance() * 100.0) / 100.0;
        distance_tv.setText("Distance: " + Double.toString(rounded) + " meters");
        chargingAvailability_tv.setText("Charging Availability: " + infoWindowData.getChargingAvailability());
        parkingAvailability_tv.setText("Parking Availability: " + infoWindowData.getParkingAvailability());
        isFree_tv.setText("Public Status: " + infoWindowData.getPublicStatus());

        return view;
    }
}
