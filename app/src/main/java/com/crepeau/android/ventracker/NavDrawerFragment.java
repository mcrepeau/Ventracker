package com.crepeau.android.ventracker;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavDrawerFragment extends Fragment {

    private static final String TAG = "NavDrawerFragment";

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_GROUP_POSITION = "selected_navigation_drawer_group_position";
    private static final String STATE_SELECTED_CHILD_POSITION = "selected_navigation_drawer_child_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ExpandableListView mDrawerMenuExpListView;
    ExpandableListAdapter listAdapter;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    private View mFragmentContainerView;
    private ExpandableListAdapter adapter;

    private int mCurrentSelectedGroupPosition = 0;
    private int mCurrentSelectedChildPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    public static Map<String, String> CARDS;
    public static List<String> cardNames;

    private VentraCheckDBHelper mDbHelper;

    public NavDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);


        if (savedInstanceState != null) {
            mCurrentSelectedGroupPosition = savedInstanceState.getInt(STATE_SELECTED_GROUP_POSITION);
            if (mCurrentSelectedGroupPosition == 0)
                mCurrentSelectedChildPosition = savedInstanceState.getInt(STATE_SELECTED_CHILD_POSITION);
            mFromSavedInstanceState = true;
        }

        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedGroupPosition, mCurrentSelectedChildPosition);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerMenuExpListView = (ExpandableListView) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);

        // Preparing list data
        // adapter.notifyDataSetChanged();
        populateMenu();

        adapter = new ExpandableListAdapter(getActivity(), listDataHeader, listDataChild);
        mDrawerMenuExpListView.setAdapter(adapter);

        // Listview Group click listener
        mDrawerMenuExpListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                // If there are no cards registered, this means we only have the "Check new card" option
                if (CARDS.isEmpty())    selectItem(-1,-1);
                else    selectItem(groupPosition, -1);
                if (groupPosition > 0) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        // Listview Child click listener
        mDrawerMenuExpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                selectItem(groupPosition, childPosition);
                return false;
            }
        });

        return mDrawerMenuExpListView;
    }

    private void populateMenu() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // We instantiate the DB Helper and open the DB
        mDbHelper = new VentraCheckDBHelper(getActivity().getApplicationContext());

        CARDS = mDbHelper.getAllCardsfromDB();
        cardNames = new ArrayList<String>(CARDS.keySet());

        if (CARDS.isEmpty()){
            listDataHeader.add(getString(R.string.title_section3));
        } else {
            // Adding parent data
            listDataHeader.add(getString(R.string.title_section1));
            listDataHeader.add(getString(R.string.title_section2));
            listDataHeader.add(getString(R.string.title_section3));
            listDataChild.put(listDataHeader.get(0), cardNames); // Header, Child data
        }

    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                mDrawerLayout,                        /* host Activity */
                toolbar,                    /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                // We repopulate the menu to refresh the cards when the drawer is opened
                populateMenu();

                ExpandableListAdapter adapter = new ExpandableListAdapter(getActivity(), listDataHeader, listDataChild);
                mDrawerMenuExpListView.setAdapter(adapter);
                mDrawerMenuExpListView.expandGroup(0, true);

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int groupPosition, int childPosition) {

        if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, "selection: group " + groupPosition + ", child " + childPosition);

        if (groupPosition != 0 || childPosition != -1) {

            mCurrentSelectedGroupPosition = groupPosition;
            mCurrentSelectedChildPosition = childPosition;

            if (mDrawerLayout != null) {
                mDrawerLayout.closeDrawer(mFragmentContainerView);
            }
            if (mCallbacks != null) {
                mCallbacks.onNavigationDrawerItemSelected(groupPosition, childPosition);
            }
            if (mDrawerMenuExpListView != null) {
                mDrawerMenuExpListView.setItemChecked(groupPosition, true);
            }
        }

    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_GROUP_POSITION, mCurrentSelectedGroupPosition);
        if (mCurrentSelectedGroupPosition == 0)
            outState.putInt(STATE_SELECTED_CHILD_POSITION, mCurrentSelectedChildPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.action_refresh) {
            if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, "User requested refresh data for card " + mCurrentSelectedChildPosition);
            mCallbacks.onRefreshCardData(mCurrentSelectedChildPosition);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int groupPosition, int childPosition);

        void onRefreshCardData(int position);
    }
}
