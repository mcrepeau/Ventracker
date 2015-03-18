package com.mcrepeau.ventracheck;

import android.provider.BaseColumns;

/**
 * Created by mcrepeau on 3/16/15.
 */
public class VentraCheckDBContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public VentraCheckDBContract() {}

    /* Inner class that defines the table contents */
    public static abstract class VentraCardInfo implements BaseColumns {
        public static final String TABLE_NAME = "card_info";
        public static final String COLUMN_NAME_CARD_NB = "card_nb";
        public static final String COLUMN_NAME_EXPMONTH = "expmonth";
        public static final String COLUMN_NAME_EXPYEAR = "expyear";

        public static final String TEXT_TYPE = " TEXT";
        public static final String COMMA_SEP = ",";
        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_CARD_NB + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_EXPMONTH + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_EXPYEAR + TEXT_TYPE + " )";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

    }

    public static abstract class VentraCardData implements BaseColumns {
        public static final String TABLE_NAME = "card_data";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_USER_ID = "user_id"; // Unique ID token randomly generated on first use of the app
        public static final String COLUMN_NAME_H_CARD_NB = "card_nb"; // Card number (hashed by user token? or by custom token to be able to retrieve single card scanned by multiple users) to protect privacy
        public static final String COLUMN_NAME_BALANCE = "balance";
        public static final String COLUMN_NAME_PASSES = "passes";

        public static final String TEXT_TYPE = " TEXT";
        public static final String COMMA_SEP = ",";
        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_USER_ID + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_H_CARD_NB + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_BALANCE + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_PASSES + TEXT_TYPE + " )";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

    }


}
