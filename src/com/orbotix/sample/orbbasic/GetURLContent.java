package com.orbotix.sample.orbbasic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;
 
public class GetURLContent {
	public static String getUrlContent(String urlString) {
 
		StringBuilder builder = new StringBuilder();
		try {
			// get URL content
			Log.d("url", "query" + urlString);
			URL url = new URL(urlString);
			Log.d("url", "hitting" + url);
			URLConnection conn = url.openConnection();
 
			// open the stream and put it into BufferedReader
			BufferedReader br = new BufferedReader(
                               new InputStreamReader(conn.getInputStream()));
 
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				builder.append(inputLine + "\n");
			}
			br.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
 
	}
}