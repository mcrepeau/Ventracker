package com.mcrepeau.ventracheck;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends ActionBarActivity
        implements NavDrawerFragment.NavigationDrawerCallbacks, DisplayCardFragment.OnFragmentInteractionListener, ManageCardsFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";

    /**
     * Async task getting the card data without blocking the UI
     */
    private GetCardDataTask mGetCardDataTask = null;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavDrawerFragment mNavDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private boolean mMenuState;

    /**
     * Database helper and variables to store the card info to sync between functions
     */
    private VentraCheckDBHelper mDbHelper;
    public static Map<String, String> CARDS;
    public static List<String> cardInfos;
    public static List<String> cardNames;

    public int MAX_CARDS = 8;

    /**
     * Actual card info and card data resulting from a scan, an update or a database fetch to
     * display in the DisplayCardFragment or add to the DB
     */
    protected String result_info;
    protected String result_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // We get the intent from the CheckCardActivity and its data
        Intent intent = getIntent();
        result_info = intent.getStringExtra(CheckCardActivity.EXTRA_CARD_INFO);
        result_data = intent.getStringExtra(CheckCardActivity.EXTRA_CARD_DATA);

        FragmentManager fragmentManager = getSupportFragmentManager();

        // We instantiate the DB Helper and open the DB
        mDbHelper = new VentraCheckDBHelper(getApplicationContext());
        // We look for cards in the DB
        CARDS = mDbHelper.getAllCardsfromDB();

        // If we come from the CheckCardActivity and already have info and data to be displayed
        if (result_info != null && result_data != null){
            // TODO: We parse result info to look for the card number
            // TODO: We check if this card number is present in the DB, if so we just refresh its data, otherwise we proceed below
            // We check if the maximum number of cards in the DB has been reached
            if (CARDS.size() > MAX_CARDS - 1){
                // We instantiate the DisplayCardFragment with the provided info and data and set the add card flag to false
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_placeholder, DisplayCardFragment.newInstance(result_info, result_data, false))
                        .commit();
                mMenuState = true;
                invalidateOptionsMenu();
            }
            else {
                // We instantiate the DisplayCardFragment with the provided info and data and set the add card flag to true
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_placeholder, DisplayCardFragment.newInstance(result_info, result_data, true))
                        .commit();
                mMenuState = true;
                invalidateOptionsMenu();
            }

        }
        else {
            if (CARDS.isEmpty()){
                // If there is no card in the DB and we don't come from the CheckCardActivity, we go to the CheckCardActivity
                intent = new Intent(this, CheckCardActivity.class);
                startActivity(intent);
            }
/*
            else {
                // Otherwise, we automatically go to the first element in the menu
                // Hack because onNavigationDrawerItemSelected is initially called before onCreate...
                // and we want to get the result_data displayed
                onNavigationDrawerItemSelected(0, 0);
            }
*/
        }

        // We instantiate the drawer
        mNavDrawerFragment = (NavDrawerFragment)
                fragmentManager.findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // We create the ToolBar/ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set up the drawer.
        mNavDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mDbHelper.close();

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
    }

    @Override
    public void onNavigationDrawerItemSelected(int groupPosition, int childPosition) {
        // Update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        Intent intent;

        mDbHelper = new VentraCheckDBHelper(getApplicationContext());
        CARDS = mDbHelper.getAllCardsfromDB();
        cardInfos = new ArrayList<String>(CARDS.values());
        cardNames = new ArrayList<String>(CARDS.keySet());

        Log.v(TAG, "drawer position: " + groupPosition);

        // We look at the group the user clicked on
        switch (groupPosition) {
            case 0:
                // "My Cards" group : the user selected a card
                // We look in the DB for the latest data regarding that card and instantiate the
                // DisplayCardFragment with that
                if (CARDS.isEmpty()){

                }
                else{
                    result_info = cardInfos.get(childPosition);
                    List<String> carddata = mDbHelper.getCardDatafromDB(cardNames, childPosition);
                    mDbHelper.close();

                    fragmentManager.beginTransaction()
                            .replace(R.id.fragment_placeholder, DisplayCardFragment.newInstance(result_info, carddata.get(carddata.size()-1), false))
                            .commit();
                    mMenuState = true;
                    invalidateOptionsMenu();
                    onSectionAttached(groupPosition);
                }
                break;
            case 1:
                // "Manage cards" group : we instantiate the ManageCardsFragment
                if (CARDS.isEmpty()){
                    Toast.makeText(getApplicationContext(), "No cards present in the DB", Toast.LENGTH_SHORT).show();
                }
                else{
                    fragmentManager.beginTransaction()
                            .replace(R.id.fragment_placeholder, ManageCardsFragment.newInstance())
                            .commit();
                    mMenuState = false;
                    invalidateOptionsMenu();
                    onSectionAttached(groupPosition);
                }
                break;
            case 2:
                // "Check New Card" : we redirect the user to the CheckCardActivity
                intent = new Intent(this, CheckCardActivity.class);
                startActivity(intent);
                break;
        }

    }

    /**
     * Callback function from the NavDrawerFragment when the user pressed the "Refresh" button
     * @param position currently selected card to be refreshed
     */
    @Override
    public void onRefreshCardData(int position){
        //showProgress(true);

        // We get the cards from the DB, select the info of the one we want to refresh and save it in a global variable to be fetched by the GetDataTask afterwards
        CARDS = mDbHelper.getAllCardsfromDB();
        cardInfos = new ArrayList<String>(CARDS.values());
        result_info = cardInfos.get(position);

        // Instantiate the task to fetch new data for this card
        mGetCardDataTask = new GetCardDataTask();
        mGetCardDataTask.execute((Void) null);
        mDbHelper.close();

    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                mTitle = getString(R.string.title_section1);
                break;
            case 1:
                mTitle = getString(R.string.title_section2);
                break;
        }
    }

    public void restoreActionBar(){
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.display_card, menu);
            if (mMenuState == false)
                menu.findItem(R.id.action_refresh).setVisible(false);
            else
                menu.findItem(R.id.action_refresh).setVisible(true);
            restoreActionBar();
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

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.v(TAG, "Inside of onRestoreInstanceState");
        result_data = savedInstanceState.getString("result_data");
        result_info = savedInstanceState.getString("result_info");
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("result_data", result_data);
        savedInstanceState.putString("result_info", result_info);
    }

    @Override
    public void onFragmentInteraction(String string){

    }

    @Override
    public void onFragmentInteraction(Uri uri){

    }

    public class GetCardDataTask extends AsyncTask<Void, Void, Boolean> {

        VentraHttpInterface ventraHttpInterface = new VentraHttpInterface();

        GetCardDataTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // We proceed with checking the card info
                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()) {
                    result_data = ventraHttpInterface.getCardData(result_info);
                } else {
                    Toast.makeText(getApplicationContext(), "No internet connection available", Toast.LENGTH_SHORT);
                }
            } catch (Exception e) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mGetCardDataTask = null;
            //showProgress(false);

            if (success) {
                // Refresh the DisplayCardFragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_placeholder, DisplayCardFragment.newInstance(result_info, result_data, false))
                        .commit();

                mDbHelper.addDatatoDB(result_data);
            } else {
                // TODO: Error handling
            }
        }

        @Override
        protected void onCancelled() {
            mGetCardDataTask = null;
            //showProgress(false);
        }
    }

}
