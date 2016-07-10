package info.jysj.sgtransit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

public class BusServiceTime extends AppCompatActivity {

    // Initialise variables
    String ServiceNumber = null;
    String ServiceDirection = null;
    String InfoID = null;
    String arrival1 = null;
    String arrival2 = null;
    String display1 = null;
    String display2 = null;
    String status = null;
    ListView yourListView = null;
    ListAdapter customAdapter = null;
    long queryTime = 0;

    // JSON Node names
    private static final String TAG_D = "d";
    private static final String TAG_SRSVCNUM = "SR_SVC_NUM";
    private static final String TAG_SRSVCDIR = "SR_SVC_DIR";
    private static final String TAG_SRROUTSEQ = "SR_ROUT_SEQ";
    private static final String TAG_SRBSCODE = "SR_BS_CODE";
    private static final String TAG_SERVICES = "Services";
    private static final String TAG_STATUS = "Status";
    private static final String TAG_NEXTBUS = "NextBus";
    private static final String TAG_NEXTBUS_ESTIMATED_ARRIVAL = "EstimatedArrival";
    private static final String TAG_SUBSEQUENTBUS = "SubsequentBus";
    private static final String TAG_SUBSEQUENTBUS_ESTIMATED_ARRIVAL = "EstimatedArrival";
    private static final String TAG_TIME_STAMP = "timestamp";
    private static final String TAG_QUERY_STATUS = "status";

    // JSONArray
    JSONArray busservicetime = null;
    JSONArray bustime = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> busservicetimeList;
    ArrayList<HashMap<String, String>> arrivaltimeList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bus_service_list);

        // Get data from Tab2
        Intent i = getIntent();
        ServiceNumber = i.getExtras().getString("ServiceNo");
        ServiceDirection = i.getExtras().getString("ServiceDir");
        InfoID = i.getExtras().getString("InfoID");

        // Creating The Toolbar and setting it as the Toolbar for the activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(ServiceNumber);

        busservicetimeList = new ArrayList<HashMap<String, String>>();
        arrivaltimeList = new ArrayList<HashMap<String, String>>(Collections.nCopies(100, new HashMap<String, String>()));
        LoadBusRouteInfoJSON();

        // Custom Adapter
        yourListView = (ListView) findViewById(android.R.id.list);
        customAdapter = new ListAdapter(this, R.layout.bus_service_list_item, busservicetimeList, arrivaltimeList);
        yourListView.setAdapter(customAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Load bus route information from JSON file
     */
    private void LoadBusRouteInfoJSON() {
        try {
            JSONObject jsonObj = new JSONObject(loadJSONFromAsset(InfoID + "RouteSet_" + ServiceNumber + "_" + ServiceDirection + ".JSON"));

            // Getting JSON Array node
            busservicetime = jsonObj.getJSONArray(TAG_D);

            // looping through All routes
            for (int j = 0; j < busservicetime.length(); j++) {
                JSONObject c = busservicetime.getJSONObject(j);

                String srroutseq = c.getString(TAG_SRROUTSEQ);
                String srbscode = c.getString(TAG_SRBSCODE);

                // tmp hashmap
                HashMap<String, String> busservicetime = new HashMap<String, String>();

                // adding each child node to HashMap key => value
                busservicetime.put("RouteSeq", srroutseq);
                busservicetime.put("BusStopCode", srbscode);

                // adding busservicetime to busservicetimeList
                busservicetimeList.add(busservicetime);
            }

            // Sort data in ascending order
            Collections.sort(busservicetimeList, new MapComparator("RouteSeq"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * AsyncTask
     * Update time when user clicks on button
     */
    private class UpdateTime extends AsyncTask<Void, Void, Void> {
        String BusStopCode;
        int position;

        UpdateTime(String BusStopCode, int position) {
            this.BusStopCode = BusStopCode;
            this.position = position;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            if (isOnline()) {   // Check online connection
                try {

                    if (isInteger(BusStopCode, 10)) {

                        String url = "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID=" + BusStopCode + "&ServiceNo=" + ServiceNumber + "&SST=True";

                        // Creating service handler class instance
                        ServiceHandler sh = new ServiceHandler();

                        // Making a request to url and getting response
                        JSONObject urlJsonObj = new JSONObject(sh.makeServiceCall(url, ServiceHandler.GET));
                        queryTime = SystemClock.elapsedRealtime();

                        // Getting JSON Array node
                        bustime = urlJsonObj.getJSONArray(TAG_SERVICES);

                        JSONObject o = bustime.getJSONObject(0);
                        status = o.getString(TAG_STATUS);

                        // Next Bus
                        JSONObject nextbus = o.getJSONObject(TAG_NEXTBUS);
                        arrival1 = nextbus.getString(TAG_NEXTBUS_ESTIMATED_ARRIVAL);
                        display1 = MessageDisplay(status, queryTime, arrival1);

                        // Subsequent Bus
                        JSONObject subsequentbus = o.getJSONObject(TAG_SUBSEQUENTBUS);
                        arrival2 = subsequentbus.getString(TAG_SUBSEQUENTBUS_ESTIMATED_ARRIVAL);
                        display2 = MessageDisplay(status, queryTime, arrival2);

                    } else {
                        display1 = "Not a Bus Stop";
                        display2 = "Not a Bus Stop";
                    }

                    // tmp hashmap
                    HashMap<String, String> arrivaltime = new HashMap<String, String>();

                    // adding each child node to HashMap key => value
                    arrivaltime.put("Arrival1", display1);
                    arrivaltime.put("Arrival2", display2);

                    // adding arrival to arrival list
                    arrivaltimeList.set(position, arrivaltime);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                display1 = null;
                display2 = null;

                // tmp hashmap
                HashMap<String, String> arrivaltime = new HashMap<String, String>();

                // adding each child node to HashMap key => value
                arrivaltime.put("Arrival1", display1);
                arrivaltime.put("Arrival2", display2);

                // adding arrival to arrival list
                arrivaltimeList.set(position, arrivaltime);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            customAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Custom adapter for ListView
     */
    public class ListAdapter extends ArrayAdapter<HashMap<String, String>> {

        final ListAdapter listadapter = this;
        ArrayList<HashMap<String, String>> MessageAdapter = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> ArrivalAdapter = new ArrayList<HashMap<String, String>>();
        Context context;
        int layoutResourceId;
        int[] selected = new int[100];

        public ListAdapter(Context context, int layoutResourceId, ArrayList<HashMap<String, String>> MessageAdapter, ArrayList<HashMap<String, String>> ArrivalAdapter) {
            super(context, layoutResourceId, MessageAdapter);
            this.MessageAdapter = MessageAdapter;
            this.context = context;
            this.layoutResourceId = layoutResourceId;
            this.ArrivalAdapter = ArrivalAdapter;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HashMap<String, String> p;
            HashMap<String, String> q;
            View row = convertView;
            final Holder holder;
            if (row == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = layoutInflater.inflate(layoutResourceId, parent, false);
                holder = new Holder();
                holder.BSCode = (TextView) row.findViewById(R.id.busstopcode);
                holder.RouteSeq = (TextView) row.findViewById(R.id.routeseq);
                holder.spinner = (ProgressBar) row.findViewById(R.id.progressbar);
                holder.Arrival = (TextView) row.findViewById(R.id.arrival);
                row.setTag(holder);
            } else {

                holder = (Holder) row.getTag();
            }

            p = new HashMap<String, String>();
            p = MessageAdapter.get(position);
            q = new HashMap<String, String>();
            q = ArrivalAdapter.get(position);
            holder.Arrival.setId(position);
            holder.BSCode.setId(position);

            // Display bus stop code
            if (holder.BSCode != null) {
                holder.BSCode.setText(p.get("BusStopCode"));
            }

            // Display route sequence
            if (holder.RouteSeq != null) {
                holder.RouteSeq.setText(p.get("RouteSeq"));
            }

            // Display arrival time
            if (holder.Arrival != null) {
                if (selected[position] == 0) {
                    holder.spinner.setVisibility(View.GONE);
                    holder.Arrival.setText("");
                } else if (q.get("Arrival1") != null) {
                    holder.spinner.setVisibility(View.GONE);
                    if (("No Est. Available".equals(q.get("Arrival1")) && "No Est. Available".equals(q.get("Arrival2"))) || ("Not Operating Now".equals(q.get("Arrival1")) && "Not Operating Now".equals(q.get("Arrival2"))))
                        holder.Arrival.setText(q.get("Arrival1"));
                    else if ("Not a Bus Stop".equals(q.get("Arrival1")) || "Not a Bus Stop".equals(q.get("Arrival2")))
                        holder.Arrival.setText("");
                    else {
                        SpannableString ss1 = new SpannableString(q.get("Arrival1"));
                        ss1.setSpan(new RelativeSizeSpan(4f), 0, q.get("Arrival1").length(), 0); // set size
                        ss1.setSpan(new ForegroundColorSpan(Color.GREEN), 0, q.get("Arrival1").length(), 0);// set color

                        SpannableString ss2 = new SpannableString("");
                        if (isInteger(q.get("Arrival2"), 10)) {
                            ss2 = new SpannableString(q.get("Arrival2"));
                            ss2.setSpan(new RelativeSizeSpan(1f), 0, q.get("Arrival2").length(), 0); // set size
                            ss2.setSpan(new ForegroundColorSpan(Color.GRAY), 0, q.get("Arrival2").length(), 0);// set color
                        }

                        holder.Arrival.setText(TextUtils.concat(ss1, "\n", ss2));
                    }
                } else if (q.get("Arrival1") == null) {
                    holder.Arrival.setText("");
                }
            }

            // OnClickListener to update bus arrival time
            holder.Arrival.setTag(position);
            holder.Arrival.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) v.getTag();
                    String BusStopCode = busservicetimeList.get(position).get("BusStopCode");
                    selected[holder.Arrival.getId()] = 1;
                    if (isOnline()) {   // Check online connection
                        holder.spinner.setVisibility(View.VISIBLE);
                        new UpdateTime(BusStopCode, position).execute();
                    } else {
                        holder.spinner.setVisibility(View.GONE);
                        new UpdateTime(BusStopCode, position).execute();
                        if (isInteger(BusStopCode, 10)) {
                            CharSequence text = "No Network Connection. Please Turn on Mobile Data or WiFi.";
                            int duration = Toast.LENGTH_LONG;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }
                    }
                }
            });

            // OnClickListener to launch new activity, BusStopTime
            holder.BSCode.setTag(position);
            holder.BSCode.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    int position = (Integer) v.getTag();
                    selected[holder.BSCode.getId()] = 1;
                    Intent i = new Intent(getApplicationContext(), BusStopTime.class);
                    i.putExtra("BusStopCode", busservicetimeList.get(position).get("BusStopCode"));
                    startActivity(i);
                }
            });

            return row;
        }

        class Holder {
            TextView BSCode;
            TextView RouteSeq;
            TextView Arrival;
            ProgressBar spinner;
        }
    }

    /**
     * Function to read JSON from file
     */
    public String loadJSONFromAsset(String file) {
        String jsonStr = null;
        try {
            InputStream is = getAssets().open("Route Info/" + file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonStr = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return jsonStr;
    }

    /**
     * Function to convert actual time to number of minutes
     */
    public String MessageDisplay(String status, long queryTime, String arrival) {
        String display = null;
        String hour = null;
        String date = null;
        if (status.equals("In Operation") && (arrival.equals("") || arrival.equals("null")))
            display = "No Est. Available";
        else if (status.equals("Not In Operation") && (arrival.equals("") || arrival.equals("null")))
            display = "Not Operating Now";
        else {
            arrival = arrival.substring(0, 20) + String.valueOf("0800");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            try {
                Date arrivaltime = format.parse(arrival);
                long test = MainActivity.appStartTime;
                long test1 = queryTime;
                Date currenttime = new Date(MainActivity.unixTime + (queryTime - MainActivity.appStartTime));
                long diff = arrivaltime.getTime() - currenttime.getTime();
                long diffMinutes = diff / (60 * 1000);
                if (diffMinutes <= 0)
                    display = "Arr";
                else
                    display = String.valueOf(diffMinutes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return display;
    }

    /**
     * Function to check if the value is integer
     */
    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

    /**
     * Function to check if the phone is online or offline
     */
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Function to sort data in ascending order
     */
    class MapComparator implements Comparator<HashMap<String, String>> {
        private final String key;

        public MapComparator(String key) {
            this.key = key;
        }

        public int compare(HashMap<String, String> first,
                           HashMap<String, String> second) {
            // TODO: Null checking, both for maps and values
            int firstValue = Integer.parseInt(first.get(key));
            int secondValue = Integer.parseInt(second.get(key));
            return firstValue > secondValue ? +1 : firstValue < secondValue ? -1 : 0;
        }
    }

}