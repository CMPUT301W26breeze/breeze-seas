package com.example.breeze_seas;

import static android.preference.PreferenceManager.*;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;

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
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setHorizontalMapRepetitionEnabled(false);
        map.setVerticalMapRepetitionEnabled(false);
        map.setTilesScaledToDpi(true);
        GeoPoint startPoint = new GeoPoint(53.5232, -113.5263);
        map.getController().setZoom(15.0);
        map.getController().setCenter(startPoint);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        mapObj=new Map(currentEvent);
        mapObj.fetchLocation(new Map.FetchedLocationListener() {
            @Override
            public void onLocationFetched(ArrayList<GeoPoint> location) {
                if(isAdded() && map!=null){
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

    public void drawPoints(ArrayList<GeoPoint> locations){
        map.getOverlays().clear();
        for(GeoPoint point: locations){
            Marker marker=new Marker(map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle("Participant");
            map.getOverlays().add(marker);

            if (!locations.isEmpty()) {
                map.getController().animateTo(locations.get(0));
            }
            map.invalidate();
        }

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