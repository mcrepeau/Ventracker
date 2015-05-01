package com.mcrepeau.ventracheck;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mcrepeau on 3/16/15.
 */

public class VentraCheckDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "VentraCheckDBHelper";

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ventracheck.db";


    public VentraCheckDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(VentraCheckDBContract.VentraCardInfo.SQL_CREATE_ENTRIES);
        db.execSQL(VentraCheckDBContract.VentraCardData.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Adds a card to the Database
     * @param cardinfo the JSON String containing the card info
     * @return the row at which the card was added
     */
    public long addCardtoDB(String cardinfo){
        SQLiteDatabase mDb = this.getWritableDatabase();
        JSONObject JSONCardInfo;
        long newRowId = -1;

        try{
            JSONCardInfo = new JSONObject(cardinfo);
            // Gets the data repository in write mode
            SQLiteDatabase db = this.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues card_values = new ContentValues();
            card_values.put(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_CARD_NB, JSONCardInfo.getString("SerialNumber"));
            card_values.put(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPMONTH, JSONCardInfo.getString("ExpireMonth"));
            card_values.put(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPYEAR, JSONCardInfo.getString("ExpireYear"));

            // Insert the new row, returning the primary key value of the new row
            newRowId = db.insert(VentraCheckDBContract.VentraCardInfo.TABLE_NAME, null, card_values);
            Log.v(TAG, "Card info added to the DB");
        } catch (Exception e){
            e.printStackTrace();
        }
        mDb.close();

        return newRowId;
    }

    /**
     * Adds card data to the Database
     * @param carddata JSON String containing the card data
     * @return the row at which the card was added
     */
    public long addDatatoDB(String carddata){
        SQLiteDatabase mDb = this.getWritableDatabase();
        JSONObject JSONCardData;
        Calendar c = GregorianCalendar.getInstance();
        Log.v(TAG, "Time of record" + c.getTime().toString());
        long newRowId = -1;

        try{
            JSONCardData = new JSONObject(carddata);
            // Gets the data repository in write mode
            SQLiteDatabase db = this.getWritableDatabase();

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
            Log.v(TAG, "Card data added to the DB");
        } catch (Exception e){
            e.printStackTrace();
        }
        mDb.close();

        return newRowId;
    }

    /**
     * Gets all the card infos present in the DB
     * @return HashMap with the name of the card and the data associated with it
     */
    public Map<String, String> getAllCardsfromDB(){
        SQLiteDatabase mDb = this.getReadableDatabase();
        JSONObject JSONinfo = new JSONObject();
        String cardnb, expmonth, expyear;
        Map<String, String> cardinfo = new HashMap<String, String>();

        // We set an array of columns...
        String[] columns = {    VentraCheckDBContract.VentraCardInfo._ID,
                VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_CARD_NB,
                VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPYEAR,
                VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPMONTH   };

        // ...and use it in our query to know if there are any cards in the DB
        Cursor c = mDb.query(VentraCheckDBContract.VentraCardInfo.TABLE_NAME, columns, null, null, null, null, null);

        if(c.getCount() > 0) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {

                cardnb = c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_CARD_NB));
                expmonth = c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPMONTH));
                expyear = c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_EXPYEAR));

                try {
                    //We format the info in a JSON structure
                    JSONinfo.put("SerialNumber", cardnb);
                    JSONinfo.put("ExpireMonth", expmonth);
                    JSONinfo.put("ExpireYear", expyear);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // We cast the JSON into a string to return it
                String last4 = cardnb == null || cardnb.length() < 4 ?
                        cardnb : cardnb.substring(cardnb.length() - 4);

                cardinfo.put("VentraCard *" + last4, JSONinfo.toString());
                Log.v(TAG, "VentraCard *" + last4 + "/" + JSONinfo.toString());
            }
        }

        return cardinfo;
    }

    /**
     * Fetches the card data for a given card from the Database
     * @param cards the list of cards present in the DB
     * @param position the position of the card we want to fetch the data for
     * @return List of Strings for each and every data update the card has known
     */
    public List<String> getCardDatafromDB(List<String> cards, int position){
        SQLiteDatabase mDb = this.getReadableDatabase();
        JSONObject JSONdata = new JSONObject();
        List<String> carddata = new ArrayList<String>();

        String cardinfo = cards.get(position);

        String last4 = cardinfo == null || cardinfo.length() < 4 ?
                cardinfo : cardinfo.substring(cardinfo.length() - 4);

        Log.v(TAG, "Fetching data of the card ending in " + last4);

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
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                if (last4.equals(c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_CARD_NB)))) {
                    try {
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
                    carddata.add(JSONdata.toString());

                    Log.v(TAG, "VentraCard *" + last4 + "/" + JSONdata.toString());
                }
            }
        }

        return carddata;
    }

    /**
     * Removes a card from the Database and deletes all associated data
     * @param cards the list of cards present in the DB
     * @param position the position of the card we want to delete
     * @return boolean for success or failure
     */
    public boolean removeCardFromDB(List<String> cards, int position){
        SQLiteDatabase mDb = this.getWritableDatabase();
        String cardinfo = cards.get(position);
        String last4 = cardinfo == null || cardinfo.length() < 4 ?
                cardinfo : cardinfo.substring(cardinfo.length() - 4);

        Log.v(TAG, "Deletion of the card ending in " + last4);

        // First delete all the card data entries ?
        mDb.delete(VentraCheckDBContract.VentraCardData.TABLE_NAME, VentraCheckDBContract.VentraCardData.COLUMN_NAME_CARD_NB + " = '" + last4 + "';", null);

        // Then delete in the card info table
        return mDb.delete(VentraCheckDBContract.VentraCardInfo.TABLE_NAME, VentraCheckDBContract.VentraCardInfo.COLUMN_NAME_CARD_NB + " LIKE '%" + last4 + "';", null) > 0;

    }

}
