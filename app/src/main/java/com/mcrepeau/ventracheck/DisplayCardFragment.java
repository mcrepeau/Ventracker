package com.mcrepeau.ventracheck;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.GregorianCalendar;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DisplayCardFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DisplayCardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DisplayCardFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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

    private OnFragmentInteractionListener mListener;

    public final static String EXTRA_CARD_INFO = "com.mcrepeau.ventracheck.CARD_INFO";
    public final static String EXTRA_CARD_DATA = "com.mcrepeau.ventracheck.CARD_DATA";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DisplayCardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DisplayCardFragment newInstance(String cardinfo, String carddata) {
        DisplayCardFragment fragment = new DisplayCardFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_CARD_INFO, cardinfo);
        args.putString(EXTRA_CARD_DATA, carddata);
        fragment.setArguments(args);
        return fragment;
    }

    public DisplayCardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            result_info = getArguments().getString(EXTRA_CARD_INFO);
            result_data = getArguments().getString(EXTRA_CARD_DATA);
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        // We instantiate the DB Helper and open the DB
        mDbHelper = new VentraCheckDBHelper(getActivity().getApplicationContext());

        String cardinfo = getCardfromDB();

        // If we don't come from the MainActivity and no cards are in the DB we go to the CheckCardActivity
        // If there is a card in the DB we fetch its data and display it
        // Otherwise we just display the data from the card scanned
        if (result_data == null){
            Log.v("Ventra", "No card scanned");
            if(cardinfo == null){
                Log.v("Ventra DB", "No cards in the database");
                startActivity(new Intent(getActivity(), CheckCardActivity.class));
            }
            else{
                Log.v("Ventra DB", "One or more cards are present in the database");
                // Check and load info
                String carddata = getDatafromDB();
                //populateInfo(carddata);
                // TODO: Maybe add a condition to limit the number of cards in the DB
                //mAddCardButton.setVisibility(View.GONE);
            }
        }
        else {
            populateInfo(result_data);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_display_card, container, false);
        //View rootView = inflater.inflate(R.layout.fragment_display_card, null);
        // We instantiate the UI elements
        mAddCardButton = (Button) rootView.findViewById(R.id.check_card_button);

        //mMediaNickname = (TextView) findViewById(R.id.mediaNicknameValue);
        mPartialMediaSerialNbr = (TextView) rootView.findViewById(R.id.partialMediaSerialNbr);
        //mTransitAccountId = (TextView) findViewById(R.id.transitAccountIdValue);
        mAccountStatus = (TextView) rootView.findViewById(R.id.accountStatus);
        mBalance = (TextView) rootView.findViewById(R.id.totalBalanceAddPretaxBalanceValue);
        mPasses = (TextView) rootView.findViewById(R.id.passes);
        mRiderClassDescription = (TextView) rootView.findViewById(R.id.riderClassDescription);
        mRemainingRides = (TextView) rootView.findViewById(R.id.remainingRides);

        mAddCardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addCardtoDB(result_info);
                addDatatoDB(result_data);
                mAddCardButton.setVisibility(View.GONE);
            }
        });

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
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
