package com.orbotix.sample.orbbasic;

import orbotix.robot.base.DeviceAsyncData;
import orbotix.robot.base.DeviceMessenger;
import orbotix.robot.base.OrbBasicErrorASCIIAsyncData;
import orbotix.robot.base.OrbBasicErrorBinaryAsyncData;
import orbotix.robot.base.OrbBasicPrintMessageAsyncData;
import orbotix.robot.base.OrbBasicProgram;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.view.connection.SpheroConnectionView.OnRobotConnectionEventListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
	private SharedPreferences settings;
	private static final int PICK_PROGRAM_REQUEST = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        setupStatusText();
        
        settings = getSharedPreferences("list_name", 0);
        String listName = settings.getString("list_name", null);
        EditText username_input = (EditText)findViewById(R.id.username_input);
        username_input.setText(listName);
        
        Button runProgramButton = (Button)findViewById(R.id.button_run_code);
        runProgramButton.setEnabled(false);
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
        mTxtStatus.append("Code Output Appears Here\n"); // TODO why can I not call the method for this? (set status text or whatever)
    }

    /**
     * Called when the user comes back to this app
     */
    @Override
    protected void onResume() {
    	super.onResume();
        // Refresh list of Spheros

    }
    
    @Override
    protected void onStart() {
    	super.onStart();
        mSpheroConnectionView.showSpheros();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();

        // register the async data listener
        DeviceMessenger.getInstance().removeAsyncDataListener(mRobot, mDataListener);
    	// Disconnect Robot properly
    	RobotProvider.getDefaultProvider().disconnectControlledRobots();
    	
    	savePreferences();
    }
    
    private void savePreferences() {
        SharedPreferences.Editor editor = settings.edit();
        EditText username_input = (EditText)findViewById(R.id.username_input);
        editor.putString("list_name", username_input.getText().toString());
        editor.commit();
    }
    

    /**
     * Called when the user presses the back or home button
     */
    @Override
    protected void onPause() {
    	super.onPause();
    }

    /**
     * AsyncDataListener that will be assigned to the DeviceMessager, listen for print messages and errors
     */
    private DeviceMessenger.AsyncDataListener mDataListener = new DeviceMessenger.AsyncDataListener() {
        @Override
        public void onDataReceived(DeviceAsyncData data) {

            if(data instanceof OrbBasicPrintMessageAsyncData){
                OrbBasicPrintMessageAsyncData printMessage = (OrbBasicPrintMessageAsyncData)data;
                addMessageToStatusRaw(printMessage.getMessage());
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
        addMessageToStatus("Loading Code...");

        Log.d("orb", "try getting contents");
        String getCodeUrl = "http://orb-wed.meteor.com/show/" + url;
        new GetCode().execute(getCodeUrl);
    }
    
    
    
    private class GetCode extends AsyncTask<String, Integer, byte[]> {
	     protected byte[] doInBackground(String... urls) {
	    	 byte[] program = HttpGetter.getUrlContent(urls[0]).getBytes();
	    	 return program;
	     }

	     protected void onProgressUpdate(Integer... progress) {
	     }

	     protected void onPostExecute(byte[] program) {
	         Log.d("list", "done!!");
	         runCodeCallback(program);
	     }
	 }
    
    
    public void runCodeCallback(byte[] program) {
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
                    Log.d(TAG, "Done Erasing: " + successStr);
                    mOrbBasicProgram.loadProgram();
                }

                @Override
                public void onLoadProgramComplete(boolean success) {
                    Log.d("orb", "callback onLoadProgramk");
                    if (success) {
                        addMessageToStatus("Program Loaded Successfully. Executing:");
                        addMessageToStatus("=========");
                        mOrbBasicProgram.executeProgram();
                    } else {
                        addMessageToStatus("Error in loading the program.");
                    }
                }
            });
            mOrbBasicProgram.eraseStorage();
        }else {
            addMessageToStatus("Program is empty.");
        }

    }
    
    public void chooseProgramPressed(View v) {
    	Log.d(TAG, "chooseProgramPressed");
        Intent intent = new Intent(this, ListViewLoader.class);
        EditText username_input = (EditText)findViewById(R.id.username_input);
        Log.d(TAG, "setting username" + username_input.getText());
        intent.putExtra("username", username_input.getText().toString());
        startActivityForResult(intent, PICK_PROGRAM_REQUEST);
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == PICK_PROGRAM_REQUEST) {
            if (resultCode == RESULT_OK) {
                // A contact was picked.  Here we will just display it
                // to the user.
            	url = data.getStringExtra("program_id");
            	String name = data.getStringExtra("program_name");
            	addMessageToStatus("Picked " + name);
            	Log.d(TAG, "set id to" + url);
            	Button runProgramButton = (Button)findViewById(R.id.button_run_code);
                runProgramButton.setEnabled(true);
            }
        }
    }

    public String getEditOrbUrl() {
    	SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String currentUrl = settings.getString("editOrbUrl", null);
        if (currentUrl == null) {
        	// get a url from my computer
        	currentUrl = HttpGetter.getUrlContent("http://orb-wed.meteor.com/add");
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
    	addMessageToStatusRaw(msg + "\n");
    }
    
    private void addMessageToStatusRaw(String msg) {
        // append the new string
        mTxtStatus.append(msg);
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


}
