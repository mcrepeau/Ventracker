package com.crepeau.android.ventracker;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by mcrepeau on 3/15/15.
 */
public class VentraCardReader {

    private static final String TAG = "VentraCardReader";

    /**
     * Reads the data on the card to get the card info : number, expiry year and expiry month
     * @param tag
     * @return a JSON Object containing the card info
     */
    public JSONObject readCardData(Tag tag){
        IsoDep iso = IsoDep.get(tag);
        JSONObject carddata = new JSONObject();
        if (iso!=null) {
            try {
                iso.connect();
                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, "Max:" + iso.getMaxTransceiveLength() + " timeout:" + iso.getTimeout() + " connected:" + iso.isConnected());
                iso.setTimeout(2000);
                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, "Max:" + iso.getMaxTransceiveLength() + " timeout:" + iso.getTimeout() + " connected:" + iso.isConnected());

                //First command sent to the card
                byte[] command1 = new byte[]{   (byte) 0x00,
                        (byte) 0xA4,
                        (byte) 0x04,
                        (byte) 0x00,
                        (byte) 0x0E,
                        (byte) 0x32,
                        (byte) 0x50,
                        (byte) 0x41,
                        (byte) 0x59,
                        (byte) 0x2E,
                        (byte) 0x53,
                        (byte) 0x59,
                        (byte) 0x53,
                        (byte) 0x2E,
                        (byte) 0x44,
                        (byte) 0x44,
                        (byte) 0x46,
                        (byte) 0x30,
                        (byte) 0x31,
                        (byte) 0x00};

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, bytesToHex(command1));
                byte[] response1 = iso.transceive(command1);

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, bytesToHex(response1));

                // Second command sent to the card
                byte[] command2 = new byte[]{   (byte) 0x00,
                        (byte) 0xA4,
                        (byte) 0x04,
                        (byte) 0x00,
                        (byte) 0x07,
                        (byte) 0xA0,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x04,
                        (byte) 0x10,
                        (byte) 0x10,
                        (byte) 0x00};

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, bytesToHex(command2));
                byte[] response2 = iso.transceive(command2);

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, bytesToHex(response2));

                // Third command sent to the card
                byte[] command3 = new byte[]{   (byte) 0x80,
                        (byte) 0xA8,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x02,
                        (byte) 0x83,
                        (byte) 0x00,
                        (byte) 0x00};

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, bytesToHex(command3));
                byte[] response3 = iso.transceive(command3);

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, bytesToHex(response3));

                // Fourth and last command sent to the card
                byte[] command4 = new byte[]{   (byte) 0x00,
                        (byte) 0xB2,
                        (byte) 0x01,
                        (byte) 0x0C,
                        (byte) 0x00};

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, bytesToHex(command4));
                byte[] response4 = iso.transceive(command4);

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, bytesToHex(response4));

                // We extract the card number from the last response
                String cardnumber = new String(Arrays.copyOfRange(response4, 10, 26));
                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, cardnumber);
                // We extract the card expiry dates from the last response
                String expyear = new String(Arrays.copyOfRange(response4, 30, 32));
                String expmonth = new String(Arrays.copyOfRange(response4, 32, 34));
                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, expmonth + "/" + expyear);

                // The card info is put into a JSON Object
                try{
                    carddata.put("cardnumber", cardnumber);
                    carddata.put("expmonth", expmonth);
                    carddata.put("expyear", expyear);
                } catch (Exception e){
                    e.printStackTrace();
                }


            } catch (IOException e) {
                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, " " + e.getMessage());
            }
        }

        return carddata;
    }

    private String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
