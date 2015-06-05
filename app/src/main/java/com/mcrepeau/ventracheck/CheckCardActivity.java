package com.mcrepeau.ventracheck;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class CheckCardActivity extends Activity {

    private CheckCardTask mCheckCardTask = null;

    private static final String TAG = "CheckCardActivity";

    /**
     * UI References
     */
    private TextView mTextView;
    private TextView mExpiryView;
    private EditText mCardNBView;
    private EditText mExpiryMonthView;
    private EditText mExpiryYearView;
    private View mProgressView;
    private View mCheckCardFormView;

    private NfcAdapter mNfcAdapter;

    PendingIntent pendingIntent;
    IntentFilter[] filters;
    String[][] techList;

    public final static String EXTRA_CARD_INFO = "com.mcrepeau.ventracheck.CARD_INFO";
    public final static String EXTRA_CARD_DATA = "com.mcrepeau.ventracheck.CARD_DATA";

    ComponentName prev_activity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_card);

        // We instantiate the different UI elements
        //mTextView = (TextView) findViewById(R.id.textView);
        mCardNBView = (EditText) findViewById(R.id.card_nb);
        mExpiryMonthView = (EditText) findViewById(R.id.expiry_date_m);
        mExpiryYearView = (EditText) findViewById(R.id.expiry_date_y);
        mExpiryView = (TextView) findViewById(R.id.expiry_text);

        Button mCheckCardButton = (Button) findViewById(R.id.check_card_button);
        mCheckCardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckCard();
            }
        });

        mCheckCardFormView = findViewById(R.id.check_card_form);
        mProgressView = findViewById(R.id.check_card_progress);

        // We set the Intent Filter for NFC reading
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        filters = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) };
        techList = new String[][] {new String[] { IsoDep.class.getName() } };

        // The UI changes slightly depending on whether NFC is supported
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            // adapter exists and is enabled.
            mCardNBView.setHint("Enter your card number or tap your card");
        }
        else{
            mCardNBView.setHint("Enter your card number");
        }

        // get the Intent that started this Activity
        Intent in = getIntent();

        // get the Bundle that stores the data of this Activity
        Bundle b = in.getExtras();
        // getting data from bundle

        handleIntent(in);
        prev_activity = this.getCallingActivity();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mNfcAdapter!=null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techList);
    }

    @Override
    protected void onPause() {
        if(mNfcAdapter!=null)
            mNfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    public void CheckCard() {
        if (mCheckCardTask != null) {
            return;
        }

        // Reset errors.
        mCardNBView.setError(null);
        mExpiryMonthView.setError(null);
        mExpiryYearView.setError(null);

        // Store values at the time of the card checking attempt.
        String nbcard = mCardNBView.getText().toString();
        String expmonth = mExpiryMonthView.getText().toString();
        String expyear = mExpiryYearView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid card info, if the user entered it.
        if (!TextUtils.isEmpty(nbcard) && !isCardNBValid(nbcard)) {
            mCardNBView.setError(getString(R.string.error_invalid_card));
            //focusView = mCardNBView;
            cancel = true;
        }

        if (!TextUtils.isEmpty(expmonth) && !isExpiryInfoValid(expmonth)) {
            mExpiryView.setError(getString(R.string.error_invalid_card));
            //focusView = mExpiryView;
            cancel = true;
        }

        if (!TextUtils.isEmpty(expyear) && !isExpiryInfoValid(expyear)) {
            mExpiryView.setError(getString(R.string.error_invalid_card));
            //focusView = mExpiryView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            //focusView.requestFocus();
            mCardNBView.setHintTextColor(Color.RED);
            mExpiryMonthView.setHintTextColor(Color.RED);
            mExpiryYearView.setHintTextColor(Color.RED);
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mCheckCardTask = new CheckCardTask(nbcard, expmonth, expyear);
            mCheckCardTask.execute((Void) null);
        }
    }

    private boolean isCardNBValid(String nbcard) {
        return nbcard.length() == 16;
    }

    private boolean isExpiryInfoValid(String expiryinfo) { return (expiryinfo.length() == 2); }

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

            mCheckCardFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mCheckCardFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mCheckCardFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mCheckCardFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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

        JSONObject JSONcardinfo = new JSONObject();
        String carddata;

        VentraHttpInterface ventraHttpInterface = new VentraHttpInterface();

        CheckCardTask(String nbcard, String expmonth, String expyear) {
            mNBCard = nbcard;
            mExpMonth = expmonth;
            mExpYear = expyear;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // We put the info from the UI views in a JSON object
                JSONcardinfo.put("SerialNumber", mNBCard);
                JSONcardinfo.put("ExpireMonth", mExpMonth);
                JSONcardinfo.put("ExpireYear", mExpYear);

                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()) {
                    // We fetch and store the card data in a string
                    carddata = ventraHttpInterface.getCardData(JSONcardinfo.toString());
                } else {
                    // This needs to run on the UI thread and is an easier solution than using a handler
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "No internet connection available", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mCheckCardTask = null;
            showProgress(false);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);

            if (success) {
                // The card data and card info are put into an Intent to launch the MainActivity
                intent.putExtra(EXTRA_CARD_DATA, carddata);
                intent.putExtra(EXTRA_CARD_INFO, JSONcardinfo.toString());

                setResult(RESULT_OK, intent);
                finish();
                // Starts MainActivity if we don't come from the MainActivity
                if (prev_activity == null)
                    startActivity(intent);
            } else {
                // TODO: Error handling
            }
        }

        @Override
        protected void onCancelled() {
            mCheckCardTask = null;
            showProgress(false);
        }
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        JSONObject carddata = new JSONObject();

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Log.v(TAG, "IsoDep Tag detected");
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

            // If the Ventra card is read, we check its data
            CheckCard();

        }
    }

}



