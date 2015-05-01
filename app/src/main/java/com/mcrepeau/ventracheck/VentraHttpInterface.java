package com.mcrepeau.ventracheck;

import android.util.Log;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
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

                Log.v(TAG, _cookie);

                String responseString = readStream(urlConnection.getInputStream());
                Log.v(TAG, responseString);

                if(_authtoken == ""){
                    //parse the responseString for <input type="hidden" name="hdnRequestVerificationToken" id="hdnRequestVerificationToken" value="[a-zA-Z0-9]+"/>
                    Pattern p = Pattern.compile("<input type=\"hidden\" name=\"hdnRequestVerificationToken\" id=\"hdnRequestVerificationToken\" value=\"(.+?)\" />");
                    Matcher m = p.matcher(responseString);
                    while (m.find()) {
                        _authtoken = m.group(1);
                    }
                }

                Log.v(TAG, _authtoken);

            }else{
                Log.v(TAG, "Response code:" + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(urlConnection != null)
                urlConnection.disconnect();
        }

    }

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

        Log.v(TAG, JSONrequest.toString());

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
            Log.v(TAG, responseCode + " - " + responseString);
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
}
