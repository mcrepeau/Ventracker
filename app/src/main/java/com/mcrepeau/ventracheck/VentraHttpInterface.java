package com.mcrepeau.ventracheck;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by mcrepeau on 3/10/15.
 */
public class VentraHttpInterface {

    private static final String TAG = "VentraHttpInterface";

    private String _cookie = "";
    private String _authtoken = "";

    /**
     * Loads the Ventra website page to get the cookies and the authentication token
     */
    public void loadPage(){
        URL urladdress;
        String url = "https://www.ventrachicago.com/balance/";
        HttpsURLConnection urlConnection = null;

        try {
            urladdress = new URL(url);
            urlConnection = (HttpsURLConnection) urladdress.openConnection();
            int responseCode = urlConnection.getResponseCode();

            if(responseCode == HttpStatus.SC_OK){

                String headerName = null;
                if(_cookie == ""){
                    for(int i = 1; (headerName = urlConnection.getHeaderFieldKey(i)) != null; i++){
                        if(headerName.equalsIgnoreCase("Set-Cookie")){
                            String cookie = urlConnection.getHeaderField(i);
                            _cookie += cookie.substring(0, cookie.indexOf(";")) + "; ";
                        }
                    }
                }

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, _cookie);

                String responseString = readStream(urlConnection.getInputStream());
                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, responseString);

                if(_authtoken == ""){
                    //parse the responseString for <input type="hidden" name="hdnRequestVerificationToken" id="hdnRequestVerificationToken" value="[a-zA-Z0-9]+"/>
                    Pattern p = Pattern.compile("<input type=\"hidden\" name=\"hdnRequestVerificationToken\" id=\"hdnRequestVerificationToken\" value=\"(.+?)\" />");
                    Matcher m = p.matcher(responseString);
                    while (m.find()) {
                        _authtoken = m.group(1);
                    }
                }

                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, _authtoken);

            }else{
                if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, "Response code:" + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(urlConnection != null)
                urlConnection.disconnect();
        }

    }

    /**
     * Reads the stream coming from the HTTP response for parsing
     * @param in
     * @return a String with the HTTP response
     */
    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

    /**
     * Sends a POST request with the card info to get the card data
     * @param cardinfo card info (number, expiry year, expiry month)
     * @return a JSON Object with the card data
     */
    public JSONObject makePostRequest(JSONObject cardinfo) {

        int responseCode;

        URL urladdress;

        HttpsURLConnection urlConnection = null;

        JSONObject JSONrequest = new JSONObject();
        JSONObject JSONresponse = new JSONObject();

        //Formatting POST JSONrequest
        try {
            JSONrequest.put("TransitMediaInfo", cardinfo);
            JSONrequest.put("s", 1);
            JSONrequest.put("IncludePassSupportsTal", true);
        } catch (JSONException e){
            e.printStackTrace();
        }

        if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, JSONrequest.toString());

        try {
            String responseString;
            urladdress = new URL("https://www.ventrachicago.com/ajax/NAM.asmx/CheckAccountBalance");
            urlConnection = (HttpsURLConnection) urladdress.openConnection();

            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("RequestVerificationToken", _authtoken);

            urlConnection.setUseCaches(false);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            // If cookie exists, then send cookie
            if (_cookie != "") {
                urlConnection.setRequestProperty("Cookie", _cookie);
                urlConnection.connect();
            }

            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(JSONrequest.toString());
            wr.flush();
            wr.close();

            responseCode = urlConnection.getResponseCode();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            responseString = readStream(in);
            if (BuildConfig.BUILD_TYPE == "debug")  Log.v(TAG, responseCode + " - " + responseString);
            in.close();
            JSONresponse = new JSONObject(responseString);
        } catch (ClientProtocolException e) {
            // Log exception
            e.printStackTrace();
        } catch (IOException e) {
            // Log exception
            e.printStackTrace();
        } catch(JSONException e){
            e.printStackTrace();
        }

        return JSONresponse;

    }

    /**
     * Function getting the card data from the Ventra website
     * @param cardinfo a JSON-formatted string with the info of the card which data has to be fetched
     */

    public String getCardData(String cardinfo) {
        JSONObject JSONrequestrsp;
        JSONObject JSONcarddata;
        JSONObject JSONcardinfo = null;
        String result = null;

        //Calendar c = GregorianCalendar.getInstance();
        DateTime currentDateTime = new DateTime();

        try {
            JSONcardinfo = new JSONObject(cardinfo);
        } catch(Exception e){
            e.printStackTrace();
        }

        loadPage();
        JSONrequestrsp = makePostRequest(JSONcardinfo);

        //Parse JSONCardInfo and process its output
        try{
            if(JSONrequestrsp.getJSONObject("d").getBoolean("success") == true){
                JSONcarddata = JSONrequestrsp.getJSONObject("d").getJSONObject("result");
                //JSONcarddata.put("timestamp", c.getTime().toString());
                JSONcarddata.put("timestamp", currentDateTime.toString());
                result = JSONcarddata.toString();
            }
            else {
                //TODO Better and more comprehensive error handling
                result = JSONrequestrsp.getJSONObject("d").getString("error");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
