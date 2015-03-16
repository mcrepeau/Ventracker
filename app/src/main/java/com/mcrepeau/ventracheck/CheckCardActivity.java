package com.mcrepeau.ventracheck;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcB;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class CheckCardActivity extends Activity {

    private CheckCardTask mCheckCardTask = null;

    // UI references.
    private TextView mTextView;
    private EditText mCardNBView;
    private EditText mExpiryMonthView;
    private EditText mExpiryYearView;
    private View mProgressView;
    private View mAddCardFormView;

    private NfcAdapter mNfcAdapter;

    PendingIntent pendingIntent;
    IntentFilter[] filters;
    String[][] techList;

    public final static String EXTRA_CARD_INFO = "com.mcrepeau.ventracheck.CARD_INFO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        //mTextView = (TextView) findViewById(R.id.textView);
        // Set up the login form.
        mCardNBView = (EditText) findViewById(R.id.card_nb);

        mExpiryMonthView = (EditText) findViewById(R.id.expiry_date_m);
        mExpiryYearView = (EditText) findViewById(R.id.expiry_date_y);

        Button mCheckCardButton = (Button) findViewById(R.id.add_card_button);
        mCheckCardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckCard();
            }
        });

        mAddCardFormView = findViewById(R.id.add_card_form);
        mProgressView = findViewById(R.id.add_card_progress);

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        filters = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) };
        techList = new String[][] {new String[] { IsoDep.class.getName() } };

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // get the Intent that started this Activity
        Intent in = getIntent();
        // get the Bundle that stores the data of this Activity
        Bundle b = in.getExtras();
        // getting data from bundle

        handleIntent(in);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techList);
    }

    @Override
    protected void onPause() {
        mNfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }


    /**
     * Adds the card to the SQLite DB
     */
    public void CheckCard() {
        if (mCheckCardTask != null) {
            return;
        }

        // Reset errors.
        mCardNBView.setError(null);
        mExpiryMonthView.setError(null);
        mExpiryYearView.setError(null);

        // Store values at the time of the login attempt.
        String nbcard = mCardNBView.getText().toString();
        String expmonth = mExpiryMonthView.getText().toString();
        String expyear = mExpiryYearView.getText().toString();

        boolean cancel = false;
        View focusView = null;

/*
        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_card));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_invalid_card));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_card));
            focusView = mEmailView;
            cancel = true;
        }
*/
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mCheckCardTask = new CheckCardTask(nbcard, expmonth, expyear);
            mCheckCardTask.execute((Void) null);
        }
    }

    private boolean isCardNBValid(String nbcard) {
        return nbcard.length() == 2;
    }

    private boolean isExpiryInfoValid(String expmonth, String expyear) {
        return (expmonth.length() == 2 && expmonth.length() ==2);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mAddCardFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mAddCardFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAddCardFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mAddCardFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class CheckCardTask extends AsyncTask<Void, Void, Boolean> {

        private final String mNBCard;
        private final String mExpMonth;
        private final String mExpYear;

        CheckCardTask(String nbcard, String expmonth, String expyear) {
            mNBCard = nbcard;
            mExpMonth = expmonth;
            mExpYear = expyear;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                getCardInfo(mNBCard, mExpMonth, mExpYear);
                //Thread.sleep(2000);
            } catch (Exception e) {
                return false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mCheckCardTask = null;
            showProgress(false);

            if (success) {
                //
            } else {
                //
            }
        }

        @Override
        protected void onCancelled() {
            mCheckCardTask = null;
            showProgress(false);
        }
    }

    public void getCardInfo(String cardnb, String expmonth, String expyear) {
        // Gets the URL from the UI's text field.
        JSONObject JSONcardinfo;
        JSONObject cardinfo = null;
        String errorinfo = null;

        VentraHttpInterface ventraHttpInterface = new VentraHttpInterface();

        Intent intent = new Intent(this, CardInfoActivity.class);

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            ventraHttpInterface.loadPage();
            JSONcardinfo = ventraHttpInterface.makePostRequest(cardnb, expmonth, expyear);
            //Parse JSONCardInfo and process its output
            try{
                if(JSONcardinfo.getJSONObject("d").getBoolean("success") == true){
                    cardinfo = JSONcardinfo.getJSONObject("d").getJSONObject("result");
                    intent.putExtra(EXTRA_CARD_INFO, cardinfo.toString());
                    //Start CardInfoActivity
                    startActivity(intent);
                }
                else {
                    //TODO Better and more comprehensive error handling
                    errorinfo = JSONcardinfo.getJSONObject("d").getString("error");
                    mCardNBView.setHintTextColor(Color.RED);
                    mExpiryMonthView.setHintTextColor(Color.RED);
                    mExpiryYearView.setHintTextColor(Color.RED);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            //mTextView.setText("No network connection available.");
        }
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        JSONObject carddata = new JSONObject();

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Log.v("Ventra NFC", "IsoDep Tag detected");
            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            VentraCardReader ventraCardReader = new VentraCardReader();
            carddata = ventraCardReader.readCardData(tag);

            try{
                mCardNBView.setText(carddata.getString("cardnumber"));
                mExpiryMonthView.setText(carddata.getString("expmonth"));
                mExpiryYearView.setText(carddata.getString("expyear"));
            } catch (Exception e){
                e.printStackTrace();
            }

            CheckCard();

        }
    }

}



