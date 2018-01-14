package com.tigeroakes.tagit;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Scanner;


/**
  * Activity for reading data from an NDEF Tag.
  *
  * @author Ralf Wondratschek
  *
  */
public class MainActivity extends Activity {
public static final String MIME_TEXT_PLAIN = "text/plain";

public static final String TAG = "NfcDemo";
    private TextView mTextView;
    private NfcAdapter mNfcAdapter;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
 
        mTextView = (TextView) findViewById(R.id.textView_explanation);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
 
        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
 
        }
     
        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText("NFC is disabled.");
        } else {
            mTextView.setText(R.string.explanation);
        }
         
        handleIntent(getIntent());
    }
     
            @Override
    protected void onResume() {
        super.onResume();
         
        /**
                  * It's important, that the activity is in the foreground (resumed). Otherwise
                  * an IllegalStateException is thrown.
                  */
        setupForegroundDispatch(this, mNfcAdapter);
    }
     
            @Override
    protected void onPause() {
        /**
                  * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
                  */
        stopForegroundDispatch(this, mNfcAdapter);
         
        super.onPause();
    }
     
            @Override
    protected void onNewIntent(Intent intent) {
        /**
                  * This method gets called, when a new Intent gets associated with the current activity instance.
                  * Instead of creating a new activity, onNewIntent will be called. For more information have a look
                  * at the documentation.
                  *
                  * In our case this method gets called, when the user attaches a Tag to the device.
                  */
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
    String action = intent.getAction();
    if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
         
        String type = intent.getType();
        if (MIME_TEXT_PLAIN.equals(type)) {
 
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.d(TAG, "TESTTAG: tag" + tag);

            new NdefReaderTask().execute(tag);
             
        } else {
            Log.d(TAG, "Wrong mime type: " + type);
        }
    } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
         
        // In case we would still use the Tech Discovered Intent
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String[] techList = tag.getTechList();
        String searchedTech = Ndef.class.getName();
         
        for (String tech : techList) {
            if (searchedTech.equals(tech)) {
                new NdefReaderTask().execute(tag);

                break;
            }
        }
    }
    }
     
            /**
      * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
      * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
      */
            public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
 
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
 
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
 
        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }
         
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
 
            /**
      * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
      * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
      */
            public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }



    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                Log.d(TAG, "TESTTAG: NDEF is not supported by this Tag");

                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {

                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {

            byte[] payload = record.getPayload();

            return new String(payload);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mTextView.setText("NFC: " + result);
                String url_base = "https://Jopika.lib.id/test@dev/get_info/";
                String charset = "UTF-8";
                String tag_id = result;
                String name = "Jonathan";
                String query = "";
                try {
                     query = String.format("tag=%s&name=%s",
                            URLEncoder.encode(tag_id, charset),
                            URLEncoder.encode(name, charset));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    URLConnection connection = new URL(url_base + "?" + query).openConnection();
                    connection.setRequestProperty("Accept-Charset", charset);
                    InputStream response = connection.getInputStream();

                    Scanner s = new Scanner(response).useDelimiter("\\A");
                    String response_string = s.hasNext() ? s.next() : "";

                    System.out.println(response_string);


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
