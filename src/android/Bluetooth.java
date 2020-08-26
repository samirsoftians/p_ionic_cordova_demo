package cordova.plugin.bluetooth;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import android.Manifest;
import android.app.Activity;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

import android.content.IntentFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.content.BroadcastReceiver;
import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import java.lang.reflect.Method;

/**
 * This class echoes a string called from JavaScript.
 */
public class Bluetooth extends CordovaPlugin {

    private final String keyError = "error";
    private final String keyMessage = "message";
    private final String keyStatus = "status";

    private final String statusEnabled = "enabled";
    private final String statusDisabled = "disabled";
    private final String pairStatus = "paired";
    private final String unpairStatus = "unpair";

    private final String keyName = "name";
    private final String keyAddress = "address";

    //Error Messages
//Initialization
    private final String logNotEnabled = "Bluetooth not enabled";
    private final String logNotDisabled = "Bluetooth not disabled";
    private final String logNotInit = "Bluetooth not initialized";
    private final String logOperationUnsupported = "Operation unsupported";

    private final String errorArguments = "arguments";
    private final String errorConnect = "connect";
    private final String logNoArgObj = "Argument object not found";
    private final String logNoAddress = "No device address";

    private final String divdisconnect = "Disconnected";


    //General CallbackContext
    private CallbackContext initCallbackContext;
    private CallbackContext permissionsCallback;
    private CallbackContext broadcastCallback;
    private CallbackContext bondBroadcastCallback;

    private static final int REQUEST_ENABLE_BT = 1;                                //Enable bluetooth request variable
    private static final int REQUEST_DISCOVERABILITY = 1;                  //Make bluetooth discoverable request variable
    private static int REQUEST_ACCESS_FINE_LOCATION = 1;

    private BluetoothAdapter btAdapter;

    public Bluetooth() {

        btAdapter = BluetoothAdapter.getDefaultAdapter();


    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("initialize")) {
            this.initialize(callbackContext);
            return true;
        } else if (action.equals("showpairedDevice")) {
            this.showpairedDevice(callbackContext);
            return true;
        } else if (action.equals("findBluetoothDevice")) {
            this.findBluetoothDevice(callbackContext);
            return true;
        } else if (action.equals("pairDevice")) {
            this.pairDeviceAction(args, callbackContext);
            return true;
        }
        
        else if (action.equals("disconnectBle")) {
            this.disconnectBle(callbackContext);
            return true;
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
            cordova.startActivityForResult(this, enableBtIntent, REQUEST_ENABLE_BT);
        } else {                                                          //If already enabled display list of paired devices

            JSONArray returnArray = new JSONArray();

            Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
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

    private void findBluetoothDevice(CallbackContext callbackContext) {
        broadcastCallback = callbackContext;
        discoverOn();
        Activity activity = cordova.getActivity();


        if (btAdapter.isDiscovering()) {
            //Bluetooth is already in mode discovery mode, we cancel to restart it again
            btAdapter.cancelDiscovery();
        }

        btAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(receiver, filter);


    }

    //for pairing device
    private void pairDeviceAction(JSONArray args, CallbackContext callbackContext) {
        bondBroadcastCallback = callbackContext;
        JSONObject obj = getArgsObject(args);
        if (isNotArgsObject(obj, callbackContext)) {
            return;
        }

        String address = getAddress(obj);
        if (isNotAddress(address, callbackContext)) {
            return;
        }
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            unpairDevice(device);
        } else {


            pairDevice(device);
        }

        cordova.getActivity().registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));


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

    private void discoverOn() {
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        cordova.startActivityForResult(this, discoverableIntent, REQUEST_DISCOVERABILITY);

    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//---------------------------------UNCKECK BLE--------------------------------------------------
    private void disconnectBle(CallbackContext callbackContext) {




        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }


        JSONObject returnObj = new JSONObject();

        addProperty(returnObj, keyStatus,divdisconnect);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);





    }

    //----------------------------------------------------------------

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        //if (permissionsCallback == null) {
        //  return;
        // }

        //Just call hasPermission again to verify
        //JSONObject returnObj = new JSONObject();

        // addProperty(returnObj, "requestPermission", cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));

        //permissionsCallback.success(returnObj);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Discovery has found a device. Get the BluetoothDevice
                //object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


                BluetoothDevice devicedata = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


                JSONObject returnObj = new JSONObject();

                addDevice(returnObj, devicedata);

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                pluginResult.setKeepCallback(true);
                broadcastCallback.sendPluginResult(pluginResult);

                //  Log.i("findnewdevice44",""+device.getName());

            }
        }
    };

    //extract json data
    private JSONObject getArgsObject(JSONArray args) {
        if (args.length() == 1) {
            try {
                return args.getJSONObject(0);
            } catch (JSONException ex) {
            }
        }

        return null;
    }

    private boolean isNotArgsObject(JSONObject obj, CallbackContext callbackContext) {
        if (obj != null) {
            return false;
        }

        JSONObject returnObj = new JSONObject();

        addProperty(returnObj, keyError, errorArguments);
        addProperty(returnObj, keyMessage, logNoArgObj);

        callbackContext.error(returnObj);

        return true;
    }

    private boolean isNotAddress(String address, CallbackContext callbackContext) {
        if (address == null) {
            JSONObject returnObj = new JSONObject();

            addProperty(returnObj, keyError, errorConnect);
            addProperty(returnObj, keyMessage, logNoAddress);

            callbackContext.error(returnObj);
            return true;
        }

        return false;
    }

    private String getAddress(JSONObject obj) {
        //Get the address string from arguments
        String address = obj.optString(keyAddress, null);

        if (address == null) {
            return null;
        }

        //Validate address format
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return null;
        }

        return address;
    }


    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    if (broadcastCallback != null) {
                        JSONObject returnObj = new JSONObject();

                        addProperty(returnObj, keyStatus, pairStatus);
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                        pluginResult.setKeepCallback(true);
                        broadcastCallback.sendPluginResult(pluginResult);


                    }
                    // showToast("Paired");
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    //showToast("Unpaired");
                    JSONObject errorObj = new JSONObject();


                    addProperty(errorObj, keyMessage, unpairStatus);

                    broadcastCallback.error(errorObj);
                }


            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        cordova.getActivity().unregisterReceiver(receiver);

    }
}
