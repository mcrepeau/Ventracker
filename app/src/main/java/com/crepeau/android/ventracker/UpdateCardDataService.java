package com.crepeau.android.ventracker;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateCardDataService extends IntentService {

    private static final String TAG = "UpdateCardDataService";

    // An ID used to post the notification.
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    private VentraCheckDBHelper mDbHelper;
    public static Map<String, String> CARDS;
    public static List<String> cardInfos;
    public static List<String> cardNames;

    public UpdateCardDataService() {
        super("SchedulingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            // We proceed with checking the card info
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                //If we have connectivity we launch a method fetching the data
                updateCardsData();
            } else {
                getString(R.string.connection_error);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Release the wake lock provided by the BroadcastReceiver.
        UpdateAlarmReceiver.completeWakefulIntent(intent);
    }

    private void updateCardsData(){
        VentraHttpInterface ventraHttpInterface = new VentraHttpInterface();
        String result_info, result_data;
        int i;

        // We instantiate the DB Helper and open the DB
        mDbHelper = new VentraCheckDBHelper(getApplicationContext());
        // We look for cards in the DB
        CARDS = mDbHelper.getAllCardsfromDB();

        if (!CARDS.isEmpty()){
            cardInfos = new ArrayList<String>(CARDS.values());

            //Loop through the cards and execute the following for each of them
            for (i = 0; i < cardInfos.size(); i++){
                result_info = cardInfos.get(i);
                result_data = ventraHttpInterface.getCardData(result_info);
                mDbHelper.addDatatoDB(result_data);
                parseCardData(result_data);
            }
        }

    }

    private void parseCardData(String data){
        JSONObject JSONdata, JSONpasses;
        float remaining_balance;
        int remaining_days;

        DateTime currentDate = new DateTime();
        DateTime passExpiryDate = new DateTime();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean notifications_card_balance = prefs.getBoolean("notifications_card_balance", false);
        boolean notification_card_expiry = prefs.getBoolean("notification_card_expiry", false);
        int balance_threshold = Integer.parseInt(prefs.getString("balance_threshold", "4"));
        int days_threshold = Integer.parseInt(prefs.getString("days_threshold", "2"));

        if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, "Balance check: " + notifications_card_balance +  ", threshold set to: " + balance_threshold);
        if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, "Days check: " + notification_card_expiry + ", threshold set to: " + days_threshold);

        // Parses the card data to find its balance and the date of expiry
        try{
            JSONdata = new JSONObject(data);
            String balance = JSONdata.getString("totalBalanceAndPretaxBalance");
            String card_nb = JSONdata.getString("partialMediaSerialNbr");
            String passes = JSONdata.getString("passes").substring(1, JSONdata.getString("passes").length()-1);
            JSONpasses = new JSONObject(passes);

            remaining_balance = Float.parseFloat(balance.substring(1));
            if (!JSONpasses.getString("endDate").equals("")) {
                remaining_days = Days.daysBetween(currentDate, DateTime.parse(JSONpasses.getString("endDate"))).getDays();
                if (notification_card_expiry == true && (remaining_days < days_threshold && remaining_days > 0)) {
                    sendNotification(getString(R.string.pass_expiry), "Ventra pass" + card_nb + " expires in " + remaining_days + "days");
                    if (BuildConfig.BUILD_TYPE == "debug")  Log.i(TAG, "Ventra Card" + card_nb + " expires in " + remaining_days + "days");
                }
            }

            // Throw a notification depending on the card's data and the pre-set params
            if (notifications_card_balance == true && (remaining_balance < balance_threshold)) {
                sendNotification(getString(R.string.low_balance), "Ventracard " + card_nb + " has a low balance of $" + remaining_balance);
                if (BuildConfig.BUILD_TYPE == "debug")  Log.i(TAG, "Ventra Card " + card_nb + " has a low balance of " + remaining_balance);
            }

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    // Post a notification indicating whether a doodle was found.
    private void sendNotification(String title, String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
