package com.orbotix.sample.orbbasic;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListViewLoader extends ListActivity {
	 private class GetJson extends AsyncTask<String, Integer, HashMap<String, String>> {
	     protected HashMap<String, String> doInBackground(String... urls) {
	         JSONObject json;
	         HashMap<String, String> map = new HashMap<String, String>();
			try {
				json = JSONFunctions.getJSONfromURL(urls[0]);
			} catch (IOException e1) {
				e1.printStackTrace();
				return map;
			} catch (JSONException e1) {
				e1.printStackTrace();
				return map;
			}
	         JSONArray names = json.names();
	         
	         for (int i = 0; i < names.length(); i++) {
	        	 try {
					map.put(names.getString(i), json.getString(names.getString(i)));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	         }
	         Log.d("list", map.toString());
	         return map;
	     }

	     protected void onProgressUpdate(Integer... progress) {
	     }

	     protected void onPostExecute(HashMap<String, String> result) {
	         Log.d("list", "done!!");
	         
	         setPrograms(result);
	     }
	 }

	// These are the Contacts rows that we will retrieve
	static final String[] PROJECTION = new String[] {
			ContactsContract.Data._ID, ContactsContract.Data.DISPLAY_NAME };
	public HashMap<String, String> programMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String username = getIntent().getStringExtra("username");
		Log.d("list", "got username: " + username);
		new GetJson().execute("http://orb-wed.meteor.com/program_ids/" + username);
		String[] myNames = {"Loading..."};
		

		ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, 
		        android.R.layout.simple_list_item_1, myNames);
		setListAdapter(mAdapter);
		getListView().setBackgroundColor(Color.BLACK);
		
	}
	
	public void setPrograms(HashMap<String, String> map) {
		programMap = map;
		Log.d("setting programs", programMap.toString());
		String[] names = programMap.keySet().toArray(new String[0]);
		ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, 
		        android.R.layout.simple_list_item_1, names);
		setListAdapter(mAdapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		String name = (String) getListView().getItemAtPosition(position);
		Log.d("list", "click on" + name);
		Log.d("list", "value is" + programMap.get(name));
		 Intent returnIntent = new Intent();
		 returnIntent.putExtra("program_id",programMap.get(name));
		 setResult(RESULT_OK,returnIntent);     
		 finish();
		
		// Do something when a list item is clicked
	}
}