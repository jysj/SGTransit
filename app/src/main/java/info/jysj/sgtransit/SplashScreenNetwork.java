package info.jysj.sgtransit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class SplashScreenNetwork extends Activity {

    private long appStartTime;
    private long unixTime;
    private ProgressBar spinner;
    private static final String TAG_TIME_STAMP = "timestamp";
    private static final String TAG_QUERY_STATUS = "status";
    private static final String TAG_GMT_OFFSET = "gmtOffset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_network);

        TextView textView = (TextView) findViewById(R.id.splashNetwork);
        spinner = (ProgressBar)findViewById(R.id.progressBar);
        spinner.getIndeterminateDrawable().setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.MULTIPLY);   // Set spinner colour to white
        spinner.setVisibility(View.VISIBLE);

        /**
         * Showing splashscreen while making network calls to get unix time
         * before launching the app Will use AsyncTask to make http call
         */
        // Get Current Time
        if (isOnline()) {
            textView.setText(getString(R.string.splash_check_network));
            new GetCurrentTime().execute();
        } else {
            textView.setText(getString(R.string.splash_no_network));
            // Execute some code after 2 seconds have passed
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 3000);
        }
    }

    /**
     * Async Task to make http call
     */

    private class GetCurrentTime extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // Get Current Time

            try {

                String queryStatus = null;

                do {
                    // Creating service handler class instance
                    ServiceHandler sh = new ServiceHandler();
                    // Making a request to url and getting response
                    String currentTimeUrl = "http://api.timezonedb.com/?zone=Asia/Singapore&key=M1H79D4RJBAQ&format=json";
                    JSONObject urlJsonObj = new JSONObject(sh.getCurrentTime(currentTimeUrl));
                    appStartTime = SystemClock.elapsedRealtime();

                    // Getting JSON Array node
                    unixTime = urlJsonObj.getLong(TAG_TIME_STAMP);
                    // *1000 is to convert seconds to milliseconds
                    unixTime = (unixTime - urlJsonObj.getLong(TAG_GMT_OFFSET)) * 1000L;
                    queryStatus = urlJsonObj.getString(TAG_QUERY_STATUS);
                } while (queryStatus.equals("FAIL"));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // After completing http call
            // will close this activity and launch main activity
            Intent i = new Intent(SplashScreenNetwork.this, MainActivity.class);
            i.putExtra("appStartTime", (Long) appStartTime);
            i.putExtra("unixTime", (Long) unixTime);
            startActivity(i);
        }

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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
