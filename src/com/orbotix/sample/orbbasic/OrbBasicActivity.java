package com.orbotix.sample.orbbasic;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import orbotix.robot.base.*;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.view.connection.SpheroConnectionView.OnRobotConnectionEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class OrbBasicActivity extends Activity {
	/**
	 * Sphero Connection View
	 */
	private SpheroConnectionView mSpheroConnectionView;

	/**
	 * Robot to from which we are running OrbBasic programs on
	 */
	private Robot mRobot = null;

	/** OrbBasic Program */
	private OrbBasicProgram mOrbBasicProgram;
	private int mOrbBasicProgramResource;

	/** UI */
	private TextView mTxtStatus;
	
	// Other
	private String url;
	private final String TAG = "orb";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        setupStatusText();
        showUrl();
        startSpheroConnectionView();
	}

    private void startSpheroConnectionView() {
        mSpheroConnectionView = (SpheroConnectionView)findViewById(R.id.sphero_connection_view);
        mSpheroConnectionView.setOnRobotConnectionEventListener(new OnRobotConnectionEventListener() {
            @Override
            public void onRobotConnectionFailed(Robot arg0) {}
            @Override
            public void onNonePaired() {}

            @Override
            public void onRobotConnected(Robot arg0) {
                mRobot = arg0;
                mSpheroConnectionView.setVisibility(View.GONE);

                // Set the AsyncDataListener that will process print and error messages
                DeviceMessenger.getInstance().addAsyncDataListener(mRobot, mDataListener);
            }
            @Override
            public void onBluetoothNotEnabled() {
                // See UISample Sample on how to show BT settings screen, for now just notify user
                Toast.makeText(OrbBasicActivity.this, "Bluetooth Not Enabled", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupStatusText() {
        mTxtStatus = (TextView)findViewById(R.id.txt_status);
        // Auto scrolls to new data
        mTxtStatus.setMovementMethod(new ScrollingMovementMethod());
    }

    private void showUrl() {
        url = getEditOrbUrl();
        final String urlString = "http://orb-code.meteor.com/edit/" + url;
        ((TextView)findViewById(R.id.url_text)).setText(urlString);
        mTxtStatus.append("Edit Code At: "+urlString+"\n");
    }

    /**
     * Called when the user comes back to this app
     */
    @Override
    protected void onResume() {
    	super.onResume();
        // Refresh list of Spheros
        mSpheroConnectionView.showSpheros();
    }
    
    /**
     * Called when the user presses the back or home button
     */
    @Override
    protected void onPause() {
    	super.onPause();
        // register the async data listener
        DeviceMessenger.getInstance().removeAsyncDataListener(mRobot, mDataListener);
    	// Disconnect Robot properly
    	RobotProvider.getDefaultProvider().disconnectControlledRobots();
    }

    /**
     * AsyncDataListener that will be assigned to the DeviceMessager, listen for print messages and errors
     */
    private DeviceMessenger.AsyncDataListener mDataListener = new DeviceMessenger.AsyncDataListener() {
        @Override
        public void onDataReceived(DeviceAsyncData data) {

            if(data instanceof OrbBasicPrintMessageAsyncData){
                OrbBasicPrintMessageAsyncData printMessage = (OrbBasicPrintMessageAsyncData)data;
                addMessageToStatus(printMessage.getMessage());
            }
            else if(data instanceof OrbBasicErrorASCIIAsyncData ){
                OrbBasicErrorASCIIAsyncData errorMessageASCII = (OrbBasicErrorASCIIAsyncData)data;
                addMessageToStatus("Error:" + errorMessageASCII.getErrorASCII());
            }
            else if(data instanceof OrbBasicErrorBinaryAsyncData ) {
                //OrbBasicErrorBinaryAsyncData errorMessageBinary = (OrbBasicErrorBinaryAsyncData)data;
            }
        }
    };
    
    /**
     * Magic Button Pressed
     */
    public void runCodePressed(View v) {
    	Log.d("Orb", "runCodePressed");
        addMessageToStatus("Running Code");

        Log.d("orb", "try getting contents");
        String getCodeUrl = "http://orb-code.meteor.com/show/" + url;
        byte[] program = getUrlContent(getCodeUrl).getBytes();
        Log.d("orb", "program is: " + program);

        // Create the OrbBasic Program object
        mOrbBasicProgram = new OrbBasicProgram(program);

        if(program.length != 0){
            mOrbBasicProgram.setRobot(mRobot);

            // Set the listener for the OrbBasic Program Events
            mOrbBasicProgram.setOrbBasicProgramEventListener(new OrbBasicProgram.OrbBasicProgramEventListener() {
                @Override
                public void onEraseCompleted(boolean success) {
                    String successStr = (success) ? "Success!!":"Failure";
                    addMessageToStatus("Done Erasing: " + successStr);

                    mOrbBasicProgram.loadProgram();
                }

                @Override
                public void onLoadProgramComplete(boolean success) {
                    Log.d("orb", "callback onLoadProgramk");
                    if (success) {
                        addMessageToStatus("Done Loading: Success; now executing");
                        mOrbBasicProgram.executeProgram();
                    } else {
                        addMessageToStatus("Done Loading: Failure");
                    }
                }
            });
            mOrbBasicProgram.eraseStorage();
        }else {
            addMessageToStatus("Program is empty.");
        }

    }

    public String getEditOrbUrl() {
    	SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String currentUrl = settings.getString("editOrbUrl", null);
        if (currentUrl == null) {
        	// get a url from my computer
        	currentUrl = getUrlContent("http://orb-code.meteor.com/add");
        	Log.d(TAG, "saving the editor to be " + currentUrl);
        	editor.putString("editOrbUrl", currentUrl);
        	editor.commit();
        }
        return currentUrl;
    }

    /**
     * Function to append a string to a TextView as a new line
     * @param msg to append
     */
    private void addMessageToStatus(String msg){
        // append the new string
        mTxtStatus.append(msg+"\n");
        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        final int scrollAmount = mTxtStatus.getLayout().getLineTop(mTxtStatus.getLineCount())
                -mTxtStatus.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if(scrollAmount>0)
            mTxtStatus.scrollTo(0, scrollAmount);
        else
            mTxtStatus.scrollTo(0,0);
    }

    private String getUrlContent(String urlString){
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
