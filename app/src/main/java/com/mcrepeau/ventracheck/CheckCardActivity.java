package com.mcrepeau.ventracheck;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class CheckCardActivity extends Activity {

    private AddCardTask mAddCardTask = null;

    // UI references.
    private TextView mTextView;
    private EditText mCardNBView;
    private EditText mExpiryMonthView;
    private EditText mExpiryYearView;
    private View mProgressView;
    private View mAddCardFormView;

    public final static String EXTRA_CARD_INFO = "com.mcrepeau.ventracheck.CARD_INFO";

    private String _cookie = "";
    private String _authtoken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        //mTextView = (TextView) findViewById(R.id.textView);
        // Set up the login form.
        mCardNBView = (EditText) findViewById(R.id.card_nb);

        mExpiryMonthView = (EditText) findViewById(R.id.expiry_date_m);
        mExpiryYearView = (EditText) findViewById(R.id.expiry_date_y);

        Button mEmailSignInButton = (Button) findViewById(R.id.add_card_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AddCard();
            }
        });

        mAddCardFormView = findViewById(R.id.add_card_form);
        mProgressView = findViewById(R.id.add_card_progress);
    }


    /**
     * Adds the card to the SQLite DB
     */
    public void AddCard() {
        if (mAddCardTask != null) {
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
            mAddCardTask = new AddCardTask(nbcard, expmonth, expyear);
            mAddCardTask.execute((Void) null);
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
    public class AddCardTask extends AsyncTask<Void, Void, Boolean> {

        private final String mNBCard;
        private final String mExpMonth;
        private final String mExpYear;

        AddCardTask(String nbcard, String expmonth, String expyear) {
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
            mAddCardTask = null;
            showProgress(false);

            if (success) {
                //
            } else {
                //
            }
        }

        @Override
        protected void onCancelled() {
            mAddCardTask = null;
            showProgress(false);
        }
    }

    public void getCardInfo(String cardnb, String expmonth, String expyear) {
        // Gets the URL from the UI's text field.
        String stringUrl = "https://www.ventrachicago.com/balance/";
        JSONObject JSONcardinfo;
        JSONObject cardinfo = null;
        String errorinfo = null;

        Intent intent = new Intent(this, CardInfoActivity.class);

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            loadPage(stringUrl);
            JSONcardinfo = makePostRequest(cardnb, expmonth, expyear);
            //Parse JSONCardInfo and process its output
            try{
                if(JSONcardinfo.getJSONObject("d").getBoolean("success") == true){
                    cardinfo = JSONcardinfo.getJSONObject("d").getJSONObject("result");
                    intent.putExtra(EXTRA_CARD_INFO, cardinfo.toString());
                    //Start CardInfoActivity
                    startActivity(intent);
                }
                else {
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

    private void loadPage(String url){
        URL urladdress;
        HttpsURLConnection urlConnection = null;

        try {
            urladdress = new URL(url);
            urlConnection = (HttpsURLConnection) urladdress.openConnection();
            int responseCode = urlConnection.getResponseCode();

            if(responseCode == HttpStatus.SC_OK){

                String headerName = null;
                if(_cookie == ""){
                    for(int i = 1; (headerName = urlConnection.getHeaderFieldKey(i)) != null; i++){
                        if(headerName.equalsIgnoreCase("Set-Cookie")){
                            String cookie = urlConnection.getHeaderField(i);
                            _cookie += cookie.substring(0, cookie.indexOf(";")) + "; ";
                        }
                    }
                }

                Log.v("HTTP GET Cookie", _cookie);

                String responseString = readStream(urlConnection.getInputStream());
                Log.v("HTTP GET Rsp", responseString);

                if(_authtoken == ""){
                    //parse the responseString for <input type="hidden" name="hdnRequestVerificationToken" id="hdnRequestVerificationToken" value="[a-zA-Z0-9]+"/>
                    Pattern p = Pattern.compile("<input type=\"hidden\" name=\"hdnRequestVerificationToken\" id=\"hdnRequestVerificationToken\" value=\"(.+?)\" />");
                    Matcher m = p.matcher(responseString);
                    while (m.find()) {
                        _authtoken = m.group(1);
                    }
                }

                Log.v("HTTP GET AuthToken", _authtoken);

            }else{
                Log.v("HTTP", "Response code:" + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(urlConnection != null)
                urlConnection.disconnect();
        }

    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

    private JSONObject makePostRequest(String serialnb, String expmonth, String expyear) {

        int responseCode;

        URL urladdress;

        HttpsURLConnection urlConnection = null;

        JSONObject cardinfo = new JSONObject();
        JSONObject request = new JSONObject();

        JSONObject JSONresponse = new JSONObject();

        //Formatting POST request
        try {
            cardinfo.put("SerialNumber", serialnb);
            cardinfo.put("ExpireMonth", expmonth);
            cardinfo.put("ExpireYear", expyear);

            request.put("TransitMediaInfo", cardinfo);
            request.put("s", 1);
            request.put("IncludePassSupportsTal", true);
        } catch (JSONException e){
            e.printStackTrace();
        }

        Log.v("HTTP POST Req", request.toString());

        try {
            String responseString;
            urladdress = new URL("https://www.ventrachicago.com/ajax/NAM.asmx/CheckAccountBalance");
            urlConnection = (HttpsURLConnection) urladdress.openConnection();

            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("RequestVerificationToken", _authtoken);

            urlConnection.setUseCaches(false);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            // If cookie exists, then send cookie
            if (_cookie != "") {
                urlConnection.setRequestProperty("Cookie", _cookie);
                urlConnection.connect();
            }

            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(request.toString());
            wr.flush();
            wr.close();

            responseCode = urlConnection.getResponseCode();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            responseString = readStream(in);
            Log.d("HTTP POST Rsp:", responseCode + " - " + responseString);
            in.close();
            JSONresponse = new JSONObject(responseString);
        } catch (ClientProtocolException e) {
            // Log exception
            e.printStackTrace();
        } catch (IOException e) {
            // Log exception
            e.printStackTrace();
        } catch(JSONException e){
            e.printStackTrace();
        }

        return JSONresponse;

    }

}



