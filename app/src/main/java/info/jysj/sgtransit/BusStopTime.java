package info.jysj.sgtransit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

public class BusStopTime extends AppCompatActivity {

    private SwipeRefreshLayout swipeContainer;

    String BusStopCode = null;
    String arrival1 = null;
    String arrival2 = null;
    String display1 = null;
    String display2 = null;
    String status = null;
    int arrivalTextViewStatus = 0;
    ListView yourListView = null;
    ListAdapter customAdapter = null;
    long queryTime = 0;

    // JSON Node names
    private static final String TAG_SERVICES = "Services";
    private static final String TAG_SERVICENO = "ServiceNo";
    private static final String TAG_STATUS = "Status";
    private static final String TAG_OPERATOR = "Operator";
    private static final String TAG_NEXTBUS = "NextBus";
    private static final String TAG_NEXTBUS_ESTIMATED_ARRIVAL = "EstimatedArrival";
    private static final String TAG_NEXTBUS_LOAD = "Load";
    private static final String TAG_NEXTBUS_FEATURE = "Feature";
    private static final String TAG_SUBSEQUENTBUS = "SubsequentBus";
    private static final String TAG_SUBSEQUENTBUS_ESTIMATED_ARRIVAL = "EstimatedArrival";
    private static final String TAG_SUBSEQUENTBUS_LOAD = "Load";
    private static final String TAG_SUBSEQUENTBUS_FEATURE = "Feature";

    // contacts JSONArray
    JSONArray busservices = null;
    JSONArray bustime = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> busservicetimeList;
    ArrayList<HashMap<String, String>> arrivaltimeList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bus_stop_list);

        Intent i = getIntent();
        BusStopCode = i.getExtras().getString("BusStopCode");

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(BusStopCode);

        busservicetimeList = new ArrayList<HashMap<String, String>>();
        arrivaltimeList = new ArrayList<HashMap<String, String>>(Collections.nCopies(100, new HashMap<String, String>()));
        // Calling async task to get json
        new BusStopsTime().execute();

        yourListView = (ListView) findViewById(android.R.id.list);
        customAdapter = new ListAdapter(this, R.layout.bus_stop_list_item, busservicetimeList, arrivaltimeList);
        yourListView.setAdapter(customAdapter);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                busservicetimeList = new ArrayList<HashMap<String, String>>();
                arrivaltimeList = new ArrayList<HashMap<String, String>>(Collections.nCopies(100, new HashMap<String, String>()));

                // Calling async task to get json
                new BusStopsTime().execute();
            }
        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_green_dark,
                android.R.color.holo_red_dark,
                android.R.color.holo_blue_dark,
                android.R.color.holo_orange_dark);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class BusStopsTime extends AsyncTask<Void, Void, Void> {

        //@Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // URL to get contacts JSON
            String url = "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID=" + BusStopCode + "&SST=True";

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);
            queryTime = SystemClock.elapsedRealtime();

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    busservices = jsonObj.getJSONArray(TAG_SERVICES);

                    // looping through All Bus Services
                    for (int i = 0; i < busservices.length(); i++) {
                        JSONObject c = busservices.getJSONObject(i);

                        String serviceno = c.getString(TAG_SERVICENO);
                        String status = c.getString(TAG_STATUS);
                        String operator = c.getString(TAG_OPERATOR);

                        JSONObject nextbus = c.getJSONObject(TAG_NEXTBUS);
                        arrival1 = nextbus.getString(TAG_NEXTBUS_ESTIMATED_ARRIVAL);
                        display1 = MessageDisplay(status, queryTime, arrival1);
                        String load1 = nextbus.getString(TAG_NEXTBUS_LOAD);
                        String feature1 = nextbus.getString(TAG_NEXTBUS_FEATURE);

                        JSONObject subsequentbus = c.getJSONObject(TAG_SUBSEQUENTBUS);
                        arrival2 = subsequentbus.getString(TAG_SUBSEQUENTBUS_ESTIMATED_ARRIVAL);
                        display2 = MessageDisplay(status, queryTime, arrival2);
                        String load2 = subsequentbus.getString(TAG_SUBSEQUENTBUS_LOAD);
                        String feature2 = subsequentbus.getString(TAG_SUBSEQUENTBUS_FEATURE);

                        // tmp hashmap
                        HashMap<String, String> busservicetime = new HashMap<String, String>();

                        // adding each child node to HashMap key => value
                        busservicetime.put("ServiceNo", serviceno);
                        busservicetime.put("Arrival1", display1);
                        busservicetime.put("Arrival2", display2);

                        // adding contact to contact list
                        busservicetimeList.add(busservicetime);
                    }

                    Collections.sort(busservicetimeList, new MapComparator("ServiceNo"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }

            // p.get("Arrival1")
            arrivalTextViewStatus = 1;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            customAdapter.setbusservicetimeList(busservicetimeList);
            customAdapter.notifyDataSetChanged();
            swipeContainer.setRefreshing(false);
        }

    }

    private class UpdateTime extends AsyncTask<Void, Void, Void> {
        String ServiceNumber;
        int position;

        UpdateTime(String ServiceNumber, int position) {
            this.ServiceNumber = ServiceNumber;
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

                        // Phone node is JSON Object
                        JSONObject nextbus = o.getJSONObject(TAG_NEXTBUS);
                        arrival1 = nextbus.getString(TAG_NEXTBUS_ESTIMATED_ARRIVAL);
                        display1 = MessageDisplay(status, queryTime, arrival1);

                        // Phone node is JSON Object
                        JSONObject subsequentbus = o.getJSONObject(TAG_SUBSEQUENTBUS);
                        arrival2 = subsequentbus.getString(TAG_SUBSEQUENTBUS_ESTIMATED_ARRIVAL);
                        display2 = MessageDisplay(status, queryTime, arrival2);

                    } else {
                        display1 = "Not a Bus Stop";
                        display2 = "Not a Bus Stop";
                    }

                    // tmp hashmap for single contact
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

                // tmp hashmap for single contact
                HashMap<String, String> arrivaltime = new HashMap<String, String>();

                // adding each child node to HashMap key => value
                arrivaltime.put("Arrival1", display1);
                arrivaltime.put("Arrival2", display2);

                // adding arrival to arrival list
                arrivaltimeList.set(position, arrivaltime);
            }

            // q.get("Arrival1")
            arrivalTextViewStatus = 2;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
           // customAdapter.setarrivaltimeList(arrivaltimeList);
            customAdapter.notifyDataSetChanged();
        }
    }

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
                holder.BusService = (TextView) row.findViewById(R.id.busservice);
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

            if (holder.BusService != null) {
                holder.BusService.setText(p.get("ServiceNo"));
            }

            if (holder.Arrival != null) {
                if (q.get("Arrival1") != null && selected[position] == 1 && arrivalTextViewStatus == 2) {      // q.get("Arrival1")
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
                } else if (p.get("Arrival1") != null && arrivalTextViewStatus == 1) {    // p.get("Arrival1")
                    holder.spinner.setVisibility(View.GONE);
                    if (("No Est. Available".equals(p.get("Arrival1")) && "No Est. Available".equals(p.get("Arrival2"))) || ("Not Operating Now".equals(p.get("Arrival1")) && "Not Operating Now".equals(p.get("Arrival2"))))
                        holder.Arrival.setText(p.get("Arrival1"));
                    else if ("Not a Bus Stop".equals(p.get("Arrival1")) || "Not a Bus Stop".equals(p.get("Arrival2")))
                        holder.Arrival.setText("");
                    else {
                        SpannableString ss1 = new SpannableString(p.get("Arrival1"));
                        ss1.setSpan(new RelativeSizeSpan(4f), 0, p.get("Arrival1").length(), 0); // set size
                        ss1.setSpan(new ForegroundColorSpan(Color.GREEN), 0, p.get("Arrival1").length(), 0);// set color

                        SpannableString ss2 = new SpannableString("");
                        if (isInteger(p.get("Arrival2"), 10)) {
                            ss2 = new SpannableString(p.get("Arrival2"));
                            ss2.setSpan(new RelativeSizeSpan(1f), 0, p.get("Arrival2").length(), 0); // set size
                            ss2.setSpan(new ForegroundColorSpan(Color.GRAY), 0, p.get("Arrival2").length(), 0);// set color
                        }

                        holder.Arrival.setText(TextUtils.concat(ss1, "\n", ss2));
                    }
                } else if (p.get("Arrival1") == null) {
                    holder.Arrival.setText("");
                }
            }

            holder.Arrival.setTag(position);
            holder.Arrival.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) v.getTag();
                    String ServiceNo = busservicetimeList.get(position).get("ServiceNo");
                    selected[holder.Arrival.getId()] = 1;
                    if (isOnline()) {   // Check online connection
                        holder.spinner.setVisibility(View.VISIBLE);
                        new UpdateTime(ServiceNo, position).execute();
                    } else {
                        holder.spinner.setVisibility(View.GONE);
                        new UpdateTime(ServiceNo, position).execute();
                        if (isInteger(ServiceNo, 10)) {
                            CharSequence text = "No Network Connection. Please Turn on Mobile Data or WiFi.";
                            int duration = Toast.LENGTH_LONG;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }
                    }
                }
            });

            return row;
        }

        public void setbusservicetimeList(ArrayList<HashMap<String, String>> busservicetimeList) {
            this.MessageAdapter = busservicetimeList;
        }

/*        public void setarrivaltimeList(ArrayList<HashMap<String, String>> arrivaltimeList) {
            this.ArrivalAdapter = arrivaltimeList;
        }*/

        class Holder {
            TextView BusService;
            TextView Arrival;
            ProgressBar spinner;
        }
    }

    public String MessageDisplay(String status, long queryTime, String arrival) {
        String display = null;
        if (status.equals("In Operation") && (arrival.equals("") || arrival.equals("null")))
            display = "No Est. Available";
        else if (status.equals("Not In Operation") && (arrival.equals("") || arrival.equals("null")))
            display = "Not Operating Now";
        else {
            arrival = arrival.substring(0, 20) + String.valueOf("0800");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            try {
                Date arrivaltime = format.parse(arrival);
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

    class MapComparator implements Comparator<HashMap<String, String>> {
        private final String key;

        public MapComparator(String key) {
            this.key = key;
        }

        public int compare(HashMap<String, String> first,
                           HashMap<String, String> second) {
            // TODO: Null checking, both for maps and values
            String firstValue = first.get(key);
            String secondValue = second.get(key);
            return firstValue.compareTo(secondValue);
        }
    }

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

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

}