package info.jysj.sgtransit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class Tab1 extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {

    private ProgressDialog pDialog;
    private SwipeRefreshLayout swipeContainer;

    // URL to get contacts JSON
    private static String url = "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID=59009&SST=True";

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
    JSONArray contacts = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> contactList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactList = new ArrayList<HashMap<String, String>>();
        // Calling async task to get json
        new BusStops().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_1, container, false);

        swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(this);
        swipeContainer.setColorSchemeResources(android.R.color.holo_green_dark,
                android.R.color.holo_red_dark,
                android.R.color.holo_blue_dark,
                android.R.color.holo_orange_dark);
        return v;
    }

    @Override
    public void onRefresh() {
        // Your code to refresh the list here.
        // Make sure you call swipeContainer.setRefreshing(false)
        // once the network request has completed successfully.
        contactList = new ArrayList<HashMap<String, String>>();

        // Calling async task to get json
        new BusStops().execute();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class BusStops extends AsyncTask<Void, Void, Void> {

        //@Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    contacts = jsonObj.getJSONArray(TAG_SERVICES);

                    // looping through All Contacts
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);

                        String serviceno = c.getString(TAG_SERVICENO);
                        String status = c.getString(TAG_STATUS);
                        String operator = c.getString(TAG_OPERATOR);

                        // Phone node is JSON Object
                        JSONObject nextbus = c.getJSONObject(TAG_NEXTBUS);
                        String arrival1 = nextbus.getString(TAG_NEXTBUS_ESTIMATED_ARRIVAL);
                        String load1 = nextbus.getString(TAG_NEXTBUS_LOAD);
                        String feature1 = nextbus.getString(TAG_NEXTBUS_FEATURE);

                        // Phone node is JSON Object
                        JSONObject subsequentbus = c.getJSONObject(TAG_SUBSEQUENTBUS);
                        String arrival2 = subsequentbus.getString(TAG_SUBSEQUENTBUS_ESTIMATED_ARRIVAL);
                        String load2 = subsequentbus.getString(TAG_SUBSEQUENTBUS_LOAD);
                        String feature2 = subsequentbus.getString(TAG_SUBSEQUENTBUS_FEATURE);

                        // tmp hashmap for single contact
                        HashMap<String, String> contact = new HashMap<String, String>();

                        // adding each child node to HashMap key => value
                        contact.put("ServiceNo", serviceno);
                        contact.put("NextBusArrival", arrival1);
                        contact.put("SubsequentBusArrival", arrival2);

                        // adding contact to contact list
                        contactList.add(contact);
                    }

                    Collections.sort(contactList, new MapComparator("ServiceNo"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new SimpleAdapter(
                    getActivity(), contactList,
                    R.layout.list_item, new String[]{"ServiceNo", "NextBusArrival",
                    "SubsequentBusArrival"}, new int[]{R.id.serviceno,
                    R.id.arrival1, R.id.arrival2});

            setListAdapter(adapter);
            swipeContainer.setRefreshing(false);
        }

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

}