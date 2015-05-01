package com.mcrepeau.ventracheck;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManageCardsFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final String TAG = "ManageCardsFragment";

    private VentraCheckDBHelper mDbHelper;
    public static Map<String, String> CARDS;
    public static List<String> cardNames;

    private OnFragmentInteractionListener mListener;

    /**
     * UI References
     */
    private Button mRemoveCardButton;
    private int mItemSelected;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    public static ManageCardsFragment newInstance() {
        ManageCardsFragment fragment = new ManageCardsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ManageCardsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We instantiate the DB Helper and open the DB
        mDbHelper = new VentraCheckDBHelper(getActivity().getApplicationContext());

        CARDS = mDbHelper.getAllCardsfromDB();
        cardNames = new ArrayList<String>(CARDS.keySet());

        mAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.cards_list_item, android.R.id.text1, cardNames);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_managecards, container, false);

        mDbHelper = new VentraCheckDBHelper(getActivity().getApplicationContext());

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        mRemoveCardButton = (Button) view.findViewById(R.id.remove_card_button);

        mRemoveCardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDbHelper.removeCardFromDB(cardNames, mItemSelected);
                // Reload list
                rePopulateCardsList();
            }
        });

        return view;
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


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListView.setItemChecked(position, true);
            mListener.onFragmentInteraction(CARDS.get(position));
            mItemSelected = position;
            Log.v(TAG, "card selected " + position);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    public void rePopulateCardsList(){

        mDbHelper = new VentraCheckDBHelper(getActivity().getApplicationContext());
        CARDS = mDbHelper.getAllCardsfromDB();
        cardNames = new ArrayList<String>(CARDS.keySet());

        mAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.cards_list_item, android.R.id.text1, new ArrayList<String>(CARDS.keySet()));

        mListView.setAdapter(mAdapter);

        mDbHelper.close();
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
        public void onFragmentInteraction(String id);
    }

}
