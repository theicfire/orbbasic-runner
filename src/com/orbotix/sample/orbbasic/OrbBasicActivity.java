package com.orbotix.sample.orbbasic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import orbotix.robot.base.DeviceAsyncData;
import orbotix.robot.base.DeviceMessenger;
import orbotix.robot.base.OrbBasicAbortProgramCommand;
import orbotix.robot.base.OrbBasicErrorASCIIAsyncData;
import orbotix.robot.base.OrbBasicErrorBinaryAsyncData;
import orbotix.robot.base.OrbBasicPrintMessageAsyncData;
import orbotix.robot.base.OrbBasicProgram;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.view.connection.SpheroConnectionView.OnRobotConnectionEventListener;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class OrbBasicActivity extends ListActivity
{
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
	private OrbBasicProgramListAdapter mListAdapter;
	
	// Other
	private String url;
	private final String TAG = "orb";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		
		mTxtStatus = (TextView)findViewById(R.id.txt_status);
		// Auto scrolls to new data
		mTxtStatus.setMovementMethod(new ScrollingMovementMethod());
		loadRawResourcesIntoList();
		
		// get and display url
		
		// TODO remove..
//		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = settings.edit();
//    	editor.putString("editOrbUrl", null);
//    	editor.commit();
        	
		// TODO why can I not use addMessageToStatus?
    	url = getEditOrbUrl();
		mTxtStatus.append("Edit Code At: http://orb-code.meteor.com/edit/" + url + "\n");
//		addMessageToStatus("Edit Code At: 192.168.1.53/edit/");

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
     * Loads the contents of the raw res folder and puts it into the ListView adapter
     */
    private void loadRawResourcesIntoList() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        ArrayList<String> listStr = new ArrayList<String>();
        Field[] fields = R.raw.class.getFields();
        for(Field f : fields)
            try {
                listStr.add(f.getName());
                list.add(f.getInt(null));
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) { }
        mListAdapter = new OrbBasicProgramListAdapter();
        mListAdapter.setOrbBasicProgramNames(listStr);
        mListAdapter.setProgramResources(list);
        getListView().setAdapter(mListAdapter);

//        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                mOrbBasicProgramResource = mListAdapter.getOrbBasicPrograms().get(i).intValue();
//                addMessageToStatus(mListAdapter.getOrbBasicProgramNames().get(i) + " now selected");
//
//                try {
//                    // Retrieve byte array from file
//                    Resources res = getResources();
//
//                    InputStream in_s = res.openRawResource(mOrbBasicProgramResource);
//                    byte[] program = new byte[in_s.available()];
//                    in_s.read(program);
//
//                    // Create the OrbBasic Program object
//                    mOrbBasicProgram = new OrbBasicProgram(program);
//                    mOrbBasicProgram.setRobot(mRobot);
//
//                    // Set the listener for the OrbBasic Program Events
//                    mOrbBasicProgram.setOrbBasicProgramEventListener(new OrbBasicProgram.OrbBasicProgramEventListener() {
//                        @Override
//                        public void onEraseCompleted(boolean success) {
//                            String successStr = (success) ? "Success":"Failure";
//                            addMessageToStatus("Done Erasing: " + successStr);
//                        }
//
//                        @Override
//                        public void onLoadProgramComplete(boolean success) {
//                            String successStr = (success) ? "Success":"Failure";
//                            addMessageToStatus("Done Loading: " + successStr);
//                        }
//                    });
//
//                } catch (Exception e) {
//                    addMessageToStatus("Error Decoding Resource");
//                }
//            }
//        });
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
     * Append Button Pressed
     */
    public void loadPressed(View v) {
        addMessageToStatus("Loading OrbBasic Program...");
        mOrbBasicProgram.loadProgram();
    }

    /**
     * Abort Button Pressed
     */
    public void abortPressed(View v) {
        addMessageToStatus("Aborting OrbBasic Program");
        OrbBasicAbortProgramCommand.sendCommand(mRobot);
    }
    
    /**
     * Magic Button Pressed
     */
    public void runCodePressed(View v) {
    	Log.d("Orb", "runCodePressed");
        addMessageToStatus("Running Code");
        
        try {
        	Log.d("orb", "try getting contents");
        	String getCodeUrl = "http://orb-code.meteor.com/show/" + url;
        	byte[] program = GetURLContent.getUrlContent(getCodeUrl).getBytes();
        	Log.d("orb", "program is: " + program);

            // Create the OrbBasic Program object
            mOrbBasicProgram = new OrbBasicProgram(program);
            mOrbBasicProgram.setRobot(mRobot);
            

            // Set the listener for the OrbBasic Program Events
            mOrbBasicProgram.setOrbBasicProgramEventListener(new OrbBasicProgram.OrbBasicProgramEventListener() {
                @Override
                public void onEraseCompleted(boolean success) {
                    String successStr = (success) ? "Success!!":"Failure";
                    addMessageToStatus("Done Erasing: " + successStr);
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
            
            Log.d("orb", "loading program now");
            mOrbBasicProgram.loadProgram();
            Log.d("orb", "after call to loading program");
        } catch (Exception e) {
            addMessageToStatus("Error Decoding Resource");
        }
        
    }


    /**
     * Execute Button Pressed
     */
    public void executePressed(View v) {
        addMessageToStatus("Executing OrbBasic Program");
        mOrbBasicProgram.executeProgram();
    }

    /**
     * Erase Button Pressed
     */
    public void erasePressed(View v) {
        addMessageToStatus("Erasing OrbBasic Program...");
        if (mOrbBasicProgram != null) {
        	mOrbBasicProgram.eraseStorage();
        }
        
    }
    
    public String getEditOrbUrl() {
    	SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String currentUrl = settings.getString("editOrbUrl", null);
        if (currentUrl == null) {
        	// get a url from my computer
        	currentUrl = GetURLContent.getUrlContent("http://orb-code.meteor.com/add");
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

    /**
     * A BaseAdapter that keeps track of the OrbBasic Programs in the raw folder
     */
    private class OrbBasicProgramListAdapter extends BaseAdapter {

        private List<Integer> mOrbBasicPrograms = new ArrayList<Integer>();
        private List<String> mOrbBasicProgramNames = new ArrayList<String>();

        public void setProgramResources(List<Integer> programs){
            mOrbBasicPrograms = programs;
            notifyDataSetChanged();
        }

        public void setOrbBasicProgramNames(List<String> programNames){
            mOrbBasicProgramNames = programNames;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mOrbBasicPrograms.size();
        }

        @Override
        public String getItem(int i) {
            return mOrbBasicProgramNames.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        public List<Integer> getOrbBasicPrograms() {
            return mOrbBasicPrograms;
        }

        public List<String> getOrbBasicProgramNames() {
            return mOrbBasicProgramNames;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            if(view == null){
                view = new OrbBasicProgramListItemView(viewGroup.getContext());
            }

            String program = getItem(i);
            OrbBasicProgramListItemView list_item = (OrbBasicProgramListItemView)view;

            // Display OrbBasic Player Name
            list_item.setText(program);

            return view;
        }
    }
}
