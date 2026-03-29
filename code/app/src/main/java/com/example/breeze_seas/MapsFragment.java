package com.example.breeze_seas;

import static android.preference.PreferenceManager.*;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * A fragment that displays an interactive map using OSMDroid.
 */
public class MapsFragment extends Fragment {

    private MapView map = null;
    private Event currentEvent;
    private Map mapObj;
    private SessionViewModel sessionViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        currentEvent = sessionViewModel.getEventShown().getValue();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        map = view.findViewById(R.id.map);

        map.setTilesScaledToDpi(true);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setHorizontalMapRepetitionEnabled(true);
        map.setVerticalMapRepetitionEnabled(false);
        map.setMinZoomLevel(4.0);
        map.setMaxZoomLevel(20.0);
        map.setMultiTouchControls(true);

        GeoPoint startPoint = new GeoPoint(53.5232, -113.5263);
        map.getController().setZoom(15.0);
        map.getController().setCenter(startPoint);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);
        view.findViewById(R.id.maps_back_button).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
        TextView subtitleView = view.findViewById(R.id.maps_subtitle);
        if (currentEvent != null && currentEvent.getName() != null && !currentEvent.getName().trim().isEmpty()) {
            subtitleView.setText(currentEvent.getName());
        }
        mapObj = new Map(currentEvent);
        mapObj.fetchLocation(new Map.FetchedLocationListener() {
            @Override
            public void onLocationFetched(HashMap<GeoPoint,String[]> location) {
                if(isAdded() && map != null){
                    drawPoints(location);
                }
            }

            @Override
            public void onFailure(Error e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * Converts a vector drawable resource into a tinted BitmapDrawable for map markers.
     * @param vectorId The resource ID of the vector asset.
     * @param color The ARGB color to tint the icon.
     * @param size The dimension in pixels for the icon width/height.
     * @return A themed BitmapDrawable suitable for OSMDroid markers.
     */
    public BitmapDrawable getMarker(int vectorId, int color, int size){
        Drawable drawable= androidx.core.content.ContextCompat.getDrawable(requireContext(), vectorId);
        if (drawable == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

        drawable = androidx.core.graphics.drawable.DrawableCompat.wrap(drawable).mutate();
        androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, color);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);

        return new BitmapDrawable(getResources(), bitmap);
    }


    /**
     * Iterates through the fetched locations and places markers on the map.
     * @param locations A map of GeoPoints to String arrays containing [username, status].
     */
    public void drawPoints(HashMap<GeoPoint,String[]> locations){
        map.getOverlays().clear();
        int iconSize = 150;
        for(java.util.Map.Entry<GeoPoint, String[]> entry: locations.entrySet()){

            GeoPoint point = entry.getKey();
            String username = entry.getValue()[0];
            String status = entry.getValue()[1];

            int color = Color.GRAY;
            if (status != null) {
                switch (status){
                    case "accepted":
                        color = Color.GREEN;
                        break;
                    case "waiting":
                        color = Color.BLUE;
                        break;
                    case "pending":
                        color = Color.RED;
                        break;
                }
            }

            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setIcon(getMarker(R.drawable.map_pointer, color, iconSize));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            marker.setTitle(username);
            marker.setSnippet("Status: " + status);
            map.getOverlays().add(marker);
        }
        if (!locations.isEmpty()) {
            GeoPoint first = locations.keySet().iterator().next();
            map.getController().animateTo(first);
        }
        map.invalidate();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}