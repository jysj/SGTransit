package info.jysj.sgtransit;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class Tab2 extends ListFragment {

    // JSON Node names
    private static final String TAG_D = "d";
    private static final String TAG_METADATA = "__metadata";
    private static final String TAG_URI = "uri";
    private static final String TAG_TYPE = "type";
    private static final String TAG_SMRTINFOID = "SMRTInfoID";
    private static final String TAG_SBSTINFOID = "SBSTInfoID";
    private static final String TAG_SISVCNUM = "SI_SVC_NUM";
    private static final String TAG_SISVCDIR = "SI_SVC_DIR";
    private static final String TAG_SISVCCAT = "SI_SVC_CAT";
    private static final String TAG_SIBSCODEST = "SI_BS_CODE_ST";
    private static final String TAG_SIBSCODEEND = "SI_BS_CODE_END";
    private static final String TAG_SIFREQAMPK = "SI_FREQ_AM_PK";
    private static final String TAG_SIFREQAMOF = "SI_FREQ_AM_OF";
    private static final String TAG_SIFREQPMPK = "SI_FREQ_PM_PK";
    private static final String TAG_SIFREQPMOF = "SI_FREQ_PM_OF";
    private static final String TAG_SILOOP = "SI_LOOP";
    private static final String TAG_SUMMARY = "Summary";
    private static final String TAG_CREATEDATE = "CreateDate";

    // Initialise variables
    public int index;
    public int top;

    // JSONArray
    JSONArray busservices = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> busservicesList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_2, container, false);
        busservicesList = new ArrayList<HashMap<String, String>>();
        // Calling async task to get json
        new BusServices().execute();
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView lv = (ListView) view.findViewById(android.R.id.list);

        // OnClickListener to launch new activity, BusServiceTime
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent i = new Intent(getActivity(), BusServiceTime.class);
                i.putExtra("ServiceNo", busservicesList.get(position).get("ServiceNo"));
                i.putExtra("ServiceDir", busservicesList.get(position).get("ServiceDir"));
                i.putExtra("InfoID", busservicesList.get(position).get("InfoID"));
                startActivity(i);
            }
        });

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
     * Load bus services from JSON file
     */
    private class BusServices extends AsyncTask<Void, Void, Void> {

        //@Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            try {

                String[] files = getActivity().getAssets().list("Service Info");

                for (int i = 0; i < files.length; i++) {
                    JSONObject jsonObj = new JSONObject(loadJSONFromAsset(files[i]));

                    // Getting JSON Array node
                    busservices = jsonObj.getJSONArray(TAG_D);

                    // looping through All Bus Services
                    for (int j = 0; j < busservices.length(); j++) {
                        JSONObject c = busservices.getJSONObject(j);

                        JSONObject metadata = c.getJSONObject(TAG_METADATA);
                        String uri = metadata.getString(TAG_URI);
                        String type = metadata.getString(TAG_TYPE);

                        String sisvcnum = c.getString(TAG_SISVCNUM);
                        String sisvcdir = c.getString(TAG_SISVCDIR);
                        //String sisvccat = c.getString(TAG_SISVCCAT);
                        //String sibscodest = c.getString(TAG_SIBSCODEST);
                        //String sibscodeend = c.getString(TAG_SIBSCODEEND);
                        //String sifreqampk = c.getString(TAG_SIFREQAMPK);
                        //String sifreqamof = c.getString(TAG_SIFREQAMOF);
                        //String sifreqpmpk = c.getString(TAG_SIFREQPMPK);
                        //String sifreqpmof = c.getString(TAG_SIFREQPMOF);
                        //String siloop = c.getString(TAG_SILOOP);
                        //String summary = c.getString(TAG_SUMMARY);
                        //String createdate = c.getString(TAG_CREATEDATE);

                        // tmp hashmap
                        HashMap<String, String> busservice = new HashMap<String, String>();

                        // adding each child node to HashMap key => value
                        busservice.put("ServiceNo", sisvcnum);
                        busservice.put("ServiceDir", sisvcdir);
                        if ((files[i].substring(0, 3)).equals(TAG_SMRTINFOID.substring(0, 3))) {
                            busservice.put("InfoID", "SMRT");
                        } else {
                            busservice.put("InfoID", "SBST");
                        }

                        // adding busservice to busservicesList
                        busservicesList.add(busservice);
                    }
                }

                // Sort data in ascending order
                Collections.sort(busservicesList, new MapComparator("ServiceNo"));

/*                for(HashMap<String, String> map: busservicesList) {
                    for(Map.Entry<String, String> mapEntry: map.entrySet()) {
                        String key = mapEntry.getKey();
                        String value = mapEntry.getValue();
                    }
                }*/

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new SimpleAdapter(
                    getActivity(), busservicesList, R.layout.list_item_2,
                    new String[]{"ServiceNo", "ServiceDir"},
                    new int[]{R.id.serviceno, R.id.servicedir});

            setListAdapter(adapter);
        }

    }

    /**
     * Function to read JSON from file
     */
    public String loadJSONFromAsset(String file) {
        String jsonStr = null;
        try {
            InputStream is = getActivity().getAssets().open("Service Info/" + file);
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
            String firstValue = first.get(key);
            String secondValue = second.get(key);
            return firstValue.compareTo(secondValue);
        }
    }

}