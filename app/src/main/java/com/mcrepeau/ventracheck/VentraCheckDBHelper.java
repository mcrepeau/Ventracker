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
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        //db.execSQL(VentraCheckDBContract.VentraCardInfo.SQL_DELETE_ENTRIES);
        //onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //onUpgrade(db, oldVersion, newVersion);
    }

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
            Log.v("Ventra DB", "Card info added to the DB");
        } catch (Exception e){
            e.printStackTrace();
        }
        mDb.close();

        return newRowId;
    }

    public long addDatatoDB(String carddata){
        SQLiteDatabase mDb = this.getWritableDatabase();
        JSONObject JSONCardData;
        Calendar c = GregorianCalendar.getInstance();
        Log.v("Ventra time", "Time of record" + c.getTime().toString());
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
            Log.v("Ventra DB", "Card data added to the DB");
        } catch (Exception e){
            e.printStackTrace();
        }
        mDb.close();

        return newRowId;
    }

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
                Log.v("Ventra DB", "VentraCard *" + last4 + "/" + JSONinfo.toString());
            }
        }

        return cardinfo;
    }

    public List<String> getCardDatafromDB(String last4cardnb){
        SQLiteDatabase mDb = this.getReadableDatabase();
        JSONObject JSONdata = new JSONObject();
        List<String> carddata = new ArrayList<String>();

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
                if (last4cardnb == c.getString(c.getColumnIndex(VentraCheckDBContract.VentraCardData.COLUMN_NAME_CARD_NB))) {
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
                }
            }

        }

        return carddata;
    }

}