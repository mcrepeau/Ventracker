package com.mcrepeau.ventracheck;

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

    public JSONObject readCardData(Tag tag){
        IsoDep iso = IsoDep.get(tag);
        JSONObject carddata = new JSONObject();
        if (iso!=null) {
            try {
                iso.connect();
                // txMessage is a TextView object used for debugging purpose
                Log.v("Ventra NFC Info", "Max:" + iso.getMaxTransceiveLength() + " timeout:" + iso.getTimeout() + " connected:" + iso.isConnected());
                iso.setTimeout(2000);
                Log.v("Ventra NFC Info", "Max:" + iso.getMaxTransceiveLength() + " timeout:" + iso.getTimeout() + " connected:" + iso.isConnected());

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

                Log.v("Ventra NFC Cmd", bytesToHex(command1));
                byte[] response1 = iso.transceive(command1);

                Log.v("Ventra NFC Rsp", bytesToHex(response1));

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

                Log.v("Ventra NFC Cmd", bytesToHex(command2));
                byte[] response2 = iso.transceive(command2);

                Log.v("Ventra NFC Rsp", bytesToHex(response2));

                byte[] command3 = new byte[]{   (byte) 0x80,
                        (byte) 0xA8,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x02,
                        (byte) 0x83,
                        (byte) 0x00,
                        (byte) 0x00};

                Log.v("Ventra NFC Cmd", bytesToHex(command3));
                byte[] response3 = iso.transceive(command3);

                Log.v("Ventra NFC Rsp", bytesToHex(response3));

                byte[] command4 = new byte[]{   (byte) 0x00,
                        (byte) 0xB2,
                        (byte) 0x01,
                        (byte) 0x0C,
                        (byte) 0x00};

                Log.v("Ventra NFC Cmd", bytesToHex(command4));
                byte[] response4 = iso.transceive(command4);

                Log.v("Ventra NFC Rsp", bytesToHex(response4));

                String cardnumber = new String(Arrays.copyOfRange(response4, 10, 26));
                Log.v("Ventra Card number", cardnumber);

                String expyear = new String(Arrays.copyOfRange(response4, 30, 32));
                String expmonth = new String(Arrays.copyOfRange(response4, 32, 34));
                Log.v("Ventra Expiration info", expmonth + "/" + expyear);

                try{
                    carddata.put("cardnumber", cardnumber);
                    carddata.put("expmonth", expmonth);
                    carddata.put("expyear", expyear);
                } catch (Exception e){
                    e.printStackTrace();
                }


            } catch (IOException e) {
                Log.v("Ventra NFC", e.getMessage());
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
