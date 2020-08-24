package cordova.plugin.bluetooth;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import android.Manifest;
import android.app.Activity;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
/**
 * This class echoes a string called from JavaScript.
 */
public class Bluetooth extends CordovaPlugin {

private final String keyError="error";
    private final String keyMessage="message";
    private final String keyStatus="status";

    private final String statusEnabled="enabled";
    private final String statusDisabled="disabled";

    //Error Messages
//Initialization
    private final String logNotEnabled="Bluetooth not enabled";
    private final String logNotDisabled="Bluetooth not disabled";
    private final String logNotInit="Bluetooth not initialized";
    private final String logOperationUnsupported="Operation unsupported";

    //General CallbackContext
    private CallbackContext initCallbackContext;
    private CallbackContext permissionsCallback;

    private static final int REQUEST_ENABLE_BT = 1;                                //Enable bluetooth request variable
    private static final int REQUEST_DISCOVERABILITY = 1;                  //Make bluetooth discoverable request variable
   private static int REQUEST_ACCESS_FINE_LOCATION= 1;

    private BluetoothAdapter btAdapter; 

    public BluetoothLePlugin() {

    btAdapter = BluetoothAdapter.getDefaultAdapter();


    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
       if (action.equals("initialize")) {
            this.initialize(callbackContext);
            return true;
        }else if(action.equals("showpairedDevice")){
         this.showpairDevice(callbackContext);
            return true
        }
        return false;
    }

  private void initialize(CallbackContext callbackContext) {

        // Request permission for location for android 6+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            JSONObject returnObj = new JSONObject();
            addProperty(returnObj, keyError, "requestPermission");
            addProperty(returnObj, keyMessage, logOperationUnsupported);
            callbackContext.error(returnObj);
            return;
        }
        // cordova.requestPermission(this, REQUEST_ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
       PermissionHelper.requestPermission(this, REQUEST_ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION);
        // permissionsCallback = callbackContext;
        initCallbackContext = callbackContext;
        JSONObject returnObj = new JSONObject();

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter.isEnabled()) {
            addProperty(returnObj, keyStatus, statusEnabled);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
            pluginResult.setKeepCallback(true);
            initCallbackContext.sendPluginResult(pluginResult);

        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cordova.startActivityForResult(this, enableBtIntent, REQUEST_ENABLE_BT);
        }


    }


    // code for show pair devices
    
  private void showpairedDevice(CallbackContext callbackContext) {

       if (!btAdapter.isEnabled()) {                                  //Enable bluetooth if not enables already
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else{                                                          //If already enabled display list of paired devices

                 JSONArray returnArray = new JSONArray();

    Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
    for (BluetoothDevice device : devices) {
    JSONObject returnObj = new JSONObject();

      addDevice(returnObj, device);

      returnArray.put(returnObj);
    }

    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnArray);
    pluginResult.setKeepCallback(true);
    callbackContext.sendPluginResult(pluginResult);
      }

            }

       

    }

      @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == REQUEST_ENABLE_BT) {

            

                if (initCallbackContext != null) {
                    JSONObject returnObj = new JSONObject();

                    addProperty(returnObj, keyStatus, statusEnabled);
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                    pluginResult.setKeepCallback(true);
                    initCallbackContext.sendPluginResult(pluginResult);


                }


                //If user has enabled bluetooth, show list of paired devices

         

        }


    }

        //General Helpers
    private void addProperty(JSONObject obj, String key, Object value) {
        //Believe exception only occurs when adding duplicate keys, so just ignore it
        try {
            if (value == null) {
                obj.put(key, JSONObject.NULL);
            } else {
                obj.put(key, value);
            }
        } catch (JSONException e) {
        }
    }

    private void addDevice(JSONObject returnObj, BluetoothDevice device) {
    addProperty(returnObj, keyAddress, device.getAddress());
    addProperty(returnObj, keyName, device.getName());
  }
     public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        //if (permissionsCallback == null) {
        //  return;
        // }

        //Just call hasPermission again to verify
        //JSONObject returnObj = new JSONObject();

        // addProperty(returnObj, "requestPermission", cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));

        //permissionsCallback.success(returnObj);
    }
}
