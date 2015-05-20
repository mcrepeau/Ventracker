package com.mcrepeau.ventracheck;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

public class DisplayCardFragment extends Fragment {

    private static final String TAG = "DisplayCardFragment";

    /**
     * UI References
     */
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
    private boolean new_card_can_be_added;
    private int position;

    private double BUS_RIDE_COST = 2.00;
    private double TRAIN_RIDE_COST = 2.25;

    private OnFragmentInteractionListener mListener;

    public final static String EXTRA_CARD_INFO = "com.mcrepeau.ventracheck.CARD_INFO";
    public final static String EXTRA_CARD_DATA = "com.mcrepeau.ventracheck.CARD_DATA";
    public final static String EXTRA_CARD_NB = "com.mcrepeau.ventracheck.CARD_NB";
    public final static String EXTRA_NEW_CARD = "com.mcrepeau.ventracheck.NEW_CARD";

    /**
     * Instantiates the Fragment
     * @param cardinfo JSON String containing the card info
     * @param carddata JSON String containing the card data
     * @param newcard boolean to know if the card is new or not
     * @return
     */
    public static DisplayCardFragment newInstance(String cardinfo, String carddata, boolean newcard) {
        DisplayCardFragment fragment = new DisplayCardFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_CARD_INFO, cardinfo);
        args.putString(EXTRA_CARD_DATA, carddata);
        args.putBoolean(EXTRA_NEW_CARD, newcard);
        fragment.setArguments(args);
        return fragment;
    }

    public DisplayCardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            result_info = getArguments().getString(EXTRA_CARD_INFO);
            result_data = getArguments().getString(EXTRA_CARD_DATA);
            new_card_can_be_added = getArguments().getBoolean(EXTRA_NEW_CARD);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        mDbHelper = new VentraCheckDBHelper(getActivity().getApplicationContext());

        populateInfo(result_data);

        if(new_card_can_be_added)
            mAddCardButton.setVisibility(View.VISIBLE);
        else mAddCardButton.setVisibility(View.GONE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_display_card, container, false);
        //View rootView = inflater.inflate(R.layout.fragment_display_card, null);
        // We instantiate the UI elements
        mAddCardButton = (Button) rootView.findViewById(R.id.add_card_button);

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
                mDbHelper.addCardtoDB(result_info);
                mDbHelper.addDatatoDB(result_data);
                mAddCardButton.setVisibility(View.GONE);
            }
        });

        return rootView;
    }

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

    /**
     * Populates all the UI elements in the View for display
     * @param data JSON String containing the data to be displayed
     */
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

        //TODO: Catch and handle potential errors
        float balance = Float.parseFloat(mBalance.getText().toString().substring(1));

        nbbusridesremaining = (int) Math.floor(balance/BUS_RIDE_COST);
        nbtrainridesremaining = (int) Math.floor(balance/TRAIN_RIDE_COST);
        //passtimeremaining = 1;
        mRemainingRides.setText("That's " + nbbusridesremaining + " bus rides OR " + nbtrainridesremaining + " train rides remaining"  );
        //mRemainingRides.setText("You have unlimited rides until " + passtimeremaining);


    }



}
