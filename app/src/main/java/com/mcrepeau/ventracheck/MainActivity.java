package com.mcrepeau.ventracheck;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.GregorianCalendar;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    //private TextView mMediaNickname;
    private TextView mPartialMediaSerialNbr;
    private TextView mTransitAccountId;
    private TextView mAccountStatus;
    private TextView mBalance;
    private TextView mPasses;
    private TextView mRiderClassDescription;
    private TextView mRemainingRides;
    private Button mAddCardButton;

    private VentraCheckDBHelper mDbHelper;

    private String result_info;
    private String result_data;

    private double BUS_RIDE_COST = 2.00;
    private double TRAIN_RIDE_COST = 2.25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getApplicationContext();

        // We get the intent from the CheckCardActivity and its data
        Intent intent = getIntent();
        result_info = intent.getStringExtra(CheckCardActivity.EXTRA_CARD_INFO);
        result_data = intent.getStringExtra(CheckCardActivity.EXTRA_CARD_DATA);

        // We instantiate the UI elements
        mAddCardButton = (Button) findViewById(R.id.check_card_button);

        //mMediaNickname = (TextView) findViewById(R.id.mediaNicknameValue);
        mPartialMediaSerialNbr = (TextView) findViewById(R.id.partialMediaSerialNbr);
        //mTransitAccountId = (TextView) findViewById(R.id.transitAccountIdValue);
        mAccountStatus = (TextView) findViewById(R.id.accountStatus);
        mBalance = (TextView) findViewById(R.id.totalBalanceAddPretaxBalanceValue);
        mPasses = (TextView) findViewById(R.id.passes);
        mRiderClassDescription = (TextView) findViewById(R.id.riderClassDescription);
        mRemainingRides = (TextView) findViewById(R.id.remainingRides);

        // We instantiate the DB Helper and open the DB
        mDbHelper = new VentraCheckDBHelper(context);

        String cardinfo = getCardfromDB();

        // If we don't come from the MainActivity and no cards are in the DB we go to the CheckCardActivity
        // If there is a card in the DB we fetch its data and display it
        // Otherwise we just display the data from the card scanned
        if (result_data == null){
            Log.v("Ventra", "No card scanned");
            if(cardinfo == null){
                Log.v("Ventra DB", "No cards in the database");
                startActivity(new Intent(this, CheckCardActivity.class));
            }
            else{
                Log.v("Ventra DB", "One or more cards are present in the database");
                // Check and load info
                String carddata = getDatafromDB();
                populateInfo(carddata);
                // TODO: Maybe add a condition to limit the number of cards in the DB
                mAddCardButton.setVisibility(View.GONE);
            }
        }
        else {
            populateInfo(result_data);
        }

        mAddCardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addCardtoDB(result_info);
                addDatatoDB(result_data);
                mAddCardButton.setVisibility(View.GONE);
            }
        });

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }
/*
    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        Toolbar toolbar = getActionBar();
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }
*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.display_card, menu);
            //restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = new Intent(this, SettingsActivity.class);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

    }

    public void populateInfo(String data){
        JSONObject JSONdata;
        int nbbusridesremaining, nbtrainridesremaining, passtimeremaining;

        mAddCardButton.setVisibility(View.VISIBLE);

        try{

            JSONdata = new JSONObject(data);

            //mMediaNickname.setText(JSONinfo.getString("mediaNickname"));
            mPartialMediaSerialNbr.setText("Card ending in " + JSONdata.getString("partialMediaSerialNbr"));
            //mTransitAccountId.setText(JSONdata.getString("transitAccountId"));
            mAccountStatus.setText("Account " + JSONdata.getString("accountStatus"));
            mBalance.setText(JSONdata.getString("totalBalanceAndPretaxBalance"));
            if (JSONdata.getString("passes") == "[]"){
                mPasses.setText("No pass is active on this card");
            }
            else{
                mPasses.setText(JSONdata.getString("passes") + "is active on this card");
            }

            mRiderClassDescription.setText(JSONdata.getString("riderClassDescription"));

        } catch (Exception e){
            e.printStackTrace();
        }

        double balance = Double.parseDouble(mBalance.getText().toString().substring(1));

        nbbusridesremaining = (int) Math.floor(balance/BUS_RIDE_COST);
        nbtrainridesremaining = (int) Math.floor(balance/TRAIN_RIDE_COST);
        //passtimeremaining = 1;
        mRemainingRides.setText("That's " + nbbusridesremaining + " bus rides OR " + nbtrainridesremaining + " train rides remaining"  );
        //mRemainingRides.setText("You have unlimited rides until " + passtimeremaining);


    }

    public long addCardtoDB(String cardinfo){
        SQLiteDatabase mDb = mDbHelper.getWritableDatabase();
        JSONObject JSONCardInfo;
        long newRowId = -1;

        try{
            JSONCardInfo = new JSONObject(cardinfo);
            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues card_values = new ContentValues();
            card_values.put(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_CARD_NB, JSONCardInfo.getString("SerialNumber"));
            card_values.put(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPMONTH, JSONCardInfo.getString("ExpireMonth"));
            card_values.put(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPYEAR, JSONCardInfo.getString("ExpireYear"));

            // Insert the new row, returning the primary key value of the new row
            newRowId = db.insert(VentraCheckDBContract.VentraCardInfo.TABLE_NAME, null, card_values);
            Log.v("Ventra DB", "Card info added to the DB");
        } catch (Exception e){
            e.printStackTrace();
        }
        mDb.close();

        return newRowId;
    }

    public long addDatatoDB(String carddata){
        SQLiteDatabase mDb = mDbHelper.getWritableDatabase();
        JSONObject JSONCardData;
        Calendar c = GregorianCalendar.getInstance();
        Log.v("Ventra time", "Time of record" + c.getTime().toString());
        long newRowId = -1;

        try{
            JSONCardData = new JSONObject(carddata);
            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues card_values = new ContentValues();

            card_values.put(VentraCheckDBContract.VentraCardData.COLUMN_NAME_MEDIA_NICK, JSONCardData.getString("mediaNickname"));
            card_values.put(VentraCheckDBContract.VentraCardData.COLUMN_NAME_CARD_NB, JSONCardData.getString("partialMediaSerialNbr"));
            card_values.put(VentraCheckDBContract.VentraCardData.COLUMN_NAME_ACCOUNT_ID, JSONCardData.getString("transitAccountId"));
            card_values.put(VentraCheckDBContract.VentraCardData.COLUMN_NAME_ACCOUNT_STATUS, JSONCardData.getString("accountStatus"));
            card_values.put(VentraCheckDBContract.VentraCardData.COLUMN_NAME_BALANCE, JSONCardData.getString("totalBalanceAndPretaxBalance"));
            card_values.put(VentraCheckDBContract.VentraCardData.COLUMN_NAME_PASSES, JSONCardData.getString("passes"));
            card_values.put(VentraCheckDBContract.VentraCardData.COLUMN_NAME_RIDER_CLASS, JSONCardData.getString("riderClassDescription"));

            // Insert the new row, returning the primary key value of the new row
            newRowId = db.insert(VentraCheckDBContract.VentraCardData.TABLE_NAME, null, card_values);
            Log.v("Ventra DB", "Card data added to the DB");
        } catch (Exception e){
            e.printStackTrace();
        }
        mDb.close();

        return newRowId;
    }

    public String getCardfromDB(){
        SQLiteDatabase mDb = mDbHelper.getReadableDatabase();
        JSONObject JSONinfo = new JSONObject();
        String info = null;

        // We set an array of columns...
        String[] columns = {    VentraCheckDBContract.VentraCardInfo._ID,
                VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_CARD_NB,
                VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPYEAR,
                VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPMONTH   };

        // ...and use it in our query to know if there are any cards in the DB
        Cursor c = mDb.query(VentraCheckDBContract.VentraCardInfo.TABLE_NAME, columns, null, null, null, null, null);

        if(c.getCount() > 0) {
            try {
                c.moveToFirst();
                //We format the info in a JSON structure
                JSONinfo.put("SerialNumber", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_CARD_NB)));
                JSONinfo.put("ExpireMonth", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPMONTH)));
                JSONinfo.put("ExpireYear", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPYEAR)));

            } catch (Exception e) {
                e.printStackTrace();
            }

            // We cast the JSON into a string to return it
            info = JSONinfo.toString();
        }

        return info;
    }

    public String getDatafromDB(){
        SQLiteDatabase mDb = mDbHelper.getReadableDatabase();
        JSONObject JSONdata = new JSONObject();
        String data = null;

        // We set an array of columns...
        String[] columns = {    VentraCheckDBContract.VentraCardData._ID,
                                VentraCheckDBContract.VentraCardData.COLUMN_NAME_MEDIA_NICK,
                                VentraCheckDBContract.VentraCardData.COLUMN_NAME_CARD_NB,
                                VentraCheckDBContract.VentraCardData.COLUMN_NAME_ACCOUNT_ID,
                                VentraCheckDBContract.VentraCardData.COLUMN_NAME_ACCOUNT_STATUS,
                                VentraCheckDBContract.VentraCardData.COLUMN_NAME_BALANCE,
                                VentraCheckDBContract.VentraCardData.COLUMN_NAME_PASSES,
                                VentraCheckDBContract.VentraCardData.COLUMN_NAME_RIDER_CLASS   };

        // ...and use it in our query to know if there are any cards in the DB
        Cursor c = mDb.query(VentraCheckDBContract.VentraCardData.TABLE_NAME, columns, null, null, null, null, null);

        if(c.getCount() > 0) {
            try {
                c.moveToLast();
                JSONdata.put("mediaNickname", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_MEDIA_NICK)));
                JSONdata.put("partialMediaSerialNbr", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_CARD_NB)));
                JSONdata.put("transitAccountId", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_ACCOUNT_ID)));
                JSONdata.put("accountStatus", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_ACCOUNT_STATUS)));
                JSONdata.put("totalBalanceAndPretaxBalance", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_BALANCE)));
                JSONdata.put("passes", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_PASSES)));
                JSONdata.put("riderClassDescription", c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_RIDER_CLASS)));

            } catch (Exception e) {
                e.printStackTrace();
            }

            // We have to cast the JSON structure into a string to return it
            data = JSONdata.toString();

        }


        return data;
    }

}
