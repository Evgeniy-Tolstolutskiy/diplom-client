package com.diplom.routeoptimization;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.app.FragmentTransaction;

import com.diplom.routeoptimization.fragments.FragmentClear;
import com.diplom.routeoptimization.fragments.FragmentComputeRoute;
import com.diplom.routeoptimization.fragments.FragmentSelectCity;
import com.diplom.routeoptimization.fragments.FragmentSelectTransport;
import com.diplom.routeoptimization.fragments.FragmentViewAdditionalInfo;
import com.diplom.routeoptimization.fragments.FragmentTools;
import com.diplom.routeoptimization.parser.DirectionsJSONParser;
import com.diplom.routeoptimization.rest.RestClient;
import com.diplom.routeoptimization.util.Util;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;

public class MapsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    public static final int MY_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private List<Marker> markers = new ArrayList<>();
    private FragmentSelectTransport fragmentSelectTransport;
    private FragmentComputeRoute fragmentComputeRoute;
    private FragmentSelectCity fragmentSelectCity;
    private FragmentClear fragmentClear;
    private FragmentViewAdditionalInfo fragmentViewAdditionalInfo;
    private FragmentTools fragmentTools;

    public double time;
    public double distance;

    private static final int ANIMATE_SPEED_TURN = 1;

    public GoogleMap getmMap() {
        return mMap;
    }

    public void clear() {
        this.getmMap().clear();
        markers.clear();
        time = 0;
        distance = 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragmentComputeRoute = new FragmentComputeRoute();
        fragmentSelectTransport = new FragmentSelectTransport();
        fragmentSelectCity = new FragmentSelectCity();
        fragmentClear = new FragmentClear();
        fragmentViewAdditionalInfo = new FragmentViewAdditionalInfo();
        fragmentTools = new FragmentTools();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng location = Location.getLocation();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)      // Sets the center of the map
                .zoom(15)                   // Sets the zoom
                .bearing(0)                // -90 = west, 90 = east
                .tilt(0)
                .build();

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (markers.size() < 2) {
                    markers.add(mMap.addMarker(new MarkerOptions().position(latLng)));
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                markers.remove(marker);
                marker.remove();
                return true;
            }
        });
    }

    //    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        LatLng location = mMap.getCameraPosition().target;
//        mMap.animateCamera(CameraUpdateFactory.newLatLng(location));
//        return true;
//    }

    public void getRoute(View v) {
        ArrayList<NameValuePair> headers = new ArrayList<NameValuePair>();
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

        params.add(new BasicNameValuePair("start", markers.get(0).getPosition().latitude + "," + markers.get(0).getPosition().longitude));
        params.add(new BasicNameValuePair("end", markers.get(1).getPosition().latitude + "," + markers.get(1).getPosition().longitude));
        params.add(new BasicNameValuePair("transport", Util.getTransport()));
        params.add(new BasicNameValuePair("city", Util.getCity()));

        RestClient restClient = new RestClient(RestClient.RequestMethod.GET, Util.getProperty("server.url", getApplicationContext()), headers, params) {
            @Override
            protected void onPostExecute(String s) {
                this.response = s;
                List<Point> points = new Gson().fromJson(response, new TypeToken<List<Point>>() {
                }.getType());
                drawRoute(points);
            }
        };

        restClient.execute();
    }

    private void drawRoute(List<Point> coordinates) {
        coordinates.add(0, new Point(markers.get(0).getPosition().latitude, markers.get(0).getPosition().longitude));
        coordinates.add(new Point(markers.get(1).getPosition().latitude, markers.get(1).getPosition().longitude));
        for (int i = 0; i < coordinates.size() - 1; i++) {
            String mode = "transit";
            if (i == 0 || i == coordinates.size() - 2) {
                mode = "walking";
            }
            LatLng start = new LatLng(coordinates.get(i).getX(), coordinates.get(i).getY());
            LatLng end = new LatLng(coordinates.get(i + 1).getX(), coordinates.get(i + 1).getY());
            String url = getDirectionsUrl(start, end, mode);
            DownloadTask downloadTask = new DownloadTask();
            downloadTask.execute(url);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        FragmentTransaction ftrans = getFragmentManager().beginTransaction();

        if (id == R.id.nav_gallery) {
            ftrans.replace(R.id.container, fragmentComputeRoute);

        } else if (id == R.id.nav_slideshow) {
            ftrans.replace(R.id.container, fragmentSelectCity);

        } else if (id == R.id.nav_manage) {
            ftrans.replace(R.id.container, fragmentSelectTransport);

        } else if (id == R.id.nav_additional_info) {
            ftrans.replace(R.id.container, fragmentViewAdditionalInfo);

        } else if (id == R.id.nav_clear) {
            ftrans.replace(R.id.container, fragmentClear);

        }
        ftrans.commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest, String modeValue) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=" + modeValue;
        String transitMode = "transit_mode=" + Util.getTransport();
        String key = "key=" + Util.getProperty("server_key", getApplicationContext());

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode + "&" + transitMode + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    private class DownloadTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }

    private class ParserTask extends AsyncTask<Object, Object, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(Object... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject((String) jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                distance += Double.parseDouble(path.get(0).get("distance").split(" ")[0]);
                time += Double.parseDouble(path.get(1).get("duration").split(" ")[0]);

                for (int j = 2; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(5);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);
                mMap.addPolyline(lineOptions);
            }
        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}
