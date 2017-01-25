package com.eveningoutpost.dexdrip.wearintegration;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import com.google.gson.Gson;//KS
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.eveningoutpost.dexdrip.Models.JoH.ts;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getDexCollectionType;

public class WatchUpdaterService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String ACTION_RESEND = WatchUpdaterService.class.getName().concat(".Resend");
    public static final String ACTION_OPEN_SETTINGS = WatchUpdaterService.class.getName().concat(".OpenSettings");
    public static final String ACTION_SYNC_DB = WatchUpdaterService.class.getName().concat(".SyncDB");//KS
    public static final String ACTION_SYNC_LOGS = WatchUpdaterService.class.getName().concat(".SyncLogs");//KS
    public static final String ACTION_CLEAR_LOGS = WatchUpdaterService.class.getName().concat(".ClearLogs");//KS
    public static final String ACTION_STATUS_COLLECTOR = WatchUpdaterService.class.getName().concat(".StatusCollector");//KS
    public static final String ACTION_START_COLLECTOR = WatchUpdaterService.class.getName().concat(".StartCollector");//KS
    public static final String ACTION_SYNC_SENSOR = WatchUpdaterService.class.getName().concat(".SyncSensor");//KS
    public static final String ACTION_SYNC_CALIBRATION = WatchUpdaterService.class.getName().concat(".SyncCalibration");//KS
    public static final String ACTION_SEND_STATUS = WatchUpdaterService.class.getName().concat(".SendStatus");//KS
    public static final String ACTION_SYNC_ACTIVEBTDEVICE = WatchUpdaterService.class.getName().concat(".SyncActiveBtDevice");//KS
    public final static String ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE
            = "com.eveningoutpost.dexdrip.BLUETOOTH_COLLECTION_SERVICE_UPDATE";
    private static final String SYNC_DB_PATH = "/syncweardb";//KS
    private static final String SYNC_BGS_PATH = "/syncwearbgs";//KS
    private static final String SYNC_LOGS_PATH = "/syncwearlogs";
    private static final String CLEAR_LOGS_PATH = "/clearwearlogs";
    private static final String STATUS_COLLECTOR_PATH = "/statuscollector";
    private static final String START_COLLECTOR_PATH = "/startcollector";
    private static final String WEARABLE_REPLYMSG_PATH = "/nightscout_watch_data_replymsg";
    private static final String WEARABLE_INITDB_PATH = "/nightscout_watch_data_initdb";
    private static final String WEARABLE_INITPREFS_PATH = "/nightscout_watch_data_initprefs";
    private static final String WEARABLE_CALIBRATION_DATA_PATH = "/nightscout_watch_cal_data";//KS
    private static final String WEARABLE_BG_DATA_PATH = "/nightscout_watch_bg_data";//KS
    private static final String WEARABLE_SENSOR_DATA_PATH = "/nightscout_watch_sensor_data";//KS
    private static final String WEARABLE_PREF_DATA_PATH = "/nightscout_watch_pref_data";//KS
    private static final String WEARABLE_ACTIVEBTDEVICE_DATA_PATH = "/nightscout_watch_activebtdevice_data";//KS
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";//KS
    private static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    private static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    private static final String WEARABLE_FIELD_SENDPATH = "field_xdrip_plus_sendpath";
    private static final String WEARABLE_FIELD_PAYLOAD = "field_xdrip_plus_payload";
    public static final String WEARABLE_VOICE_PAYLOAD = "/xdrip_plus_voice_payload";
    public static final String WEARABLE_APPROVE_TREATMENT = "/xdrip_plus_approve_treatment";
    public static final String WEARABLE_CANCEL_TREATMENT = "/xdrip_plus_cancel_treatment";
    private static final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
    private static final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";
    private static final String OPEN_SETTINGS_PATH = "/openwearsettings";
    private static final String NEW_STATUS_PATH = "/sendstatustowear";//KS
    private static final String CAPABILITY_WEAR_APP = "wear_app_sync_bgs";
    private String mWearNodeId = null;
    static final int GET_CAPABILITIES_TIMEOUT_MS = 5000;

    private static final String TAG = "jamorham watchupdater";
    private static GoogleApiClient googleApiClient;
    private static long lastRequest = 0;//KS
    private static final Integer sendCalibrationCount = 3;//KS
    private final static Integer sendBgCount = 4;//KS
    private boolean wear_integration = false;
    private boolean pebble_integration = false;
    private boolean is_using_bt = false;
    private SharedPreferences mPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;

    public static void receivedText(Context context, String text) {
        startHomeWithExtra(context, WEARABLE_VOICE_PAYLOAD, text);
    }

    private static void approveTreatment(Context context, String text) {
        startHomeWithExtra(context, WEARABLE_APPROVE_TREATMENT, text);
    }

    private static void cancelTreatment(Context context, String text) {
        startHomeWithExtra(context, WEARABLE_CANCEL_TREATMENT, text);
    }

    private static void startHomeWithExtra(Context context, String extra, String text) {
        Intent intent = new Intent(context, Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(extra, text);
        context.startActivity(intent);
    }

    private void sendDataReceived(String path, String notification, long timeOfLastEntry, String type) {//KS
        Log.d(TAG, "sendDataReceived timeOfLastEntry=" + JoH.dateTimeText(timeOfLastEntry) + " Path=" + path);
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putLong("timeOfLastEntry", timeOfLastEntry);
            dataMapRequest.getDataMap().putString("type", type);
            dataMapRequest.getDataMap().putString("msg", notification);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "sendDataReceived No connection to wearable available!");
        }
    }

    private void syncFieldData(DataMap dataMap) {
        String dex_txid = dataMap.getString("dex_txid", "");
        byte[] G5_BATTERY_MARKER = dataMap.getByteArray(G5CollectionService.G5_BATTERY_MARKER);
        byte[] G5_FIRMWARE_MARKER = dataMap.getByteArray(G5CollectionService.G5_FIRMWARE_MARKER);
        if (dex_txid != null && dex_txid.equals(mPrefs.getString("dex_txid", "default"))) {
            if (G5_BATTERY_MARKER != null) {
                long watch_last_battery_query = dataMap.getLong(G5CollectionService.G5_BATTERY_FROM_MARKER);
                long phone_last_battery_query = PersistentStore.getLong(G5CollectionService.G5_BATTERY_FROM_MARKER + dex_txid);
                if (watch_last_battery_query > phone_last_battery_query) {
                    G5CollectionService.setStoredBatteryBytes(dex_txid, G5_BATTERY_MARKER);
                    PersistentStore.setLong(G5CollectionService.G5_BATTERY_FROM_MARKER + dex_txid, watch_last_battery_query);
                }
            }
            if (G5_FIRMWARE_MARKER != null) {
                G5CollectionService.setStoredFirmwareBytes(dex_txid, G5_FIRMWARE_MARKER);
            }
        }
    }

    private void syncPrefData(DataMap dataMap) {
        boolean enable_wearG5 = dataMap.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = dataMap.getBoolean("force_wearG5", false);
        String node_wearG5 = dataMap.getString("node_wearG5", "");
        String dex_txid = dataMap.getString("dex_txid", "");
        int bridge_battery = dataMap.getInt("bridge_battery", -1);//Used in DexCollectionService

        boolean change = false;

        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        Log.d(TAG, "syncPrefData enable_wearG5: " + enable_wearG5 + " force_wearG5: " + force_wearG5 + " node_wearG5:" + node_wearG5 + " dex_txid: " + dex_txid);

        if (!node_wearG5.equals(mPrefs.getString("node_wearG5", ""))) {
            change = true;
            prefs.putString("node_wearG5", node_wearG5);
            Log.d(TAG, "syncPrefData node_wearG5:" + node_wearG5);
        }

        if (/*force_wearG5 &&*/ force_wearG5 != mPrefs.getBoolean("force_wearG5", false)) {
            change = true;
            prefs.putBoolean("force_wearG5", force_wearG5);
            Log.d(TAG, "syncPrefData commit force_wearG5:" + force_wearG5);
        }

        if (bridge_battery != mPrefs.getInt("bridge_battery", -1)) {//Used by DexCollectionService
            change = true;
            prefs.putInt("bridge_battery", bridge_battery);
            Log.d(TAG, "syncPrefData commit bridge_battery: " + bridge_battery);
        }

        if (/*enable_wearG5 &&*/ enable_wearG5 != mPrefs.getBoolean("enable_wearG5", false)) {
            change = true;
            prefs.putBoolean("enable_wearG5", enable_wearG5);
            Log.d(TAG, "syncPrefData commit enable_wearG5: " + enable_wearG5);
        }

        if (change) {
            prefs.commit();
        }
        else if (!dex_txid.equals(mPrefs.getString("dex_txid", "default"))) {
            sendPrefSettings();
            processConnect();
        }
    }

    //Assumes Wear is connected to phone
    private void processConnect() {//KS
        Log.d(TAG, "processConnect enter");
        wear_integration = mPrefs.getBoolean("wear_sync", false);
        boolean enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = mPrefs.getBoolean("force_wearG5", false);

        if (wear_integration) {
            if (enable_wearG5) {
                initWearData();
                if (force_wearG5) {
                    Log.d(TAG, "processConnect force_wearG5=true - stopBtService");
                    stopBtService();
                }
                else {
                    Log.d(TAG, "processConnect force_wearG5=false - startBtService");
                    startBtService();
                }
            }
            else {
                Log.d(TAG, "processConnect enable_wearG5=false - startBtService");
                startBtService();
            }
        }
        else {
            Log.d(TAG, "processConnect wear_integration=false - startBtService");
            startBtService();
        }
    }

    private synchronized void syncTransmitterData(DataMap dataMap, boolean bBenchmark) {//KS
        Log.d(TAG, "syncTransmitterData");

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        long timeOfLastBG = 0;
        Log.d(TAG, "syncTransmitterData add BgReading Table" );
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            Log.d(TAG, "syncTransmitterData add BgReading Table entries count=" + entries.size());
            for (DataMap entry : entries) {
                if (entry != null) {
                    //Log.d(TAG, "syncTransmitterData add BgReading Table entry=" + entry);
                    String bgrecord = entry.getString("bgs");
                    if (bgrecord != null) {//for (TransmitterData bgData : bgs) {
                        //Log.d(TAG, "syncTransmitterData add TransmitterData Table bgrecord=" + bgrecord);
                        TransmitterData bgData = gson.fromJson(bgrecord, TransmitterData.class);
                        //TransmitterData bgData = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(bgrecord, TransmitterData.class);
                        TransmitterData exists = TransmitterData.getForTimestamp(bgData.timestamp);
                        TransmitterData uuidexists = TransmitterData.findByUuid(bgData.uuid);
                        timeOfLastBG = bgData.timestamp + 1;
                        if (exists != null || uuidexists != null) {
                            Log.d(TAG, "syncTransmitterData BG already exists for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" + JoH.dateTimeText(bgData.timestamp) + " raw_data=" + bgData.raw_data);
                        } else {
                            Log.d(TAG, "syncTransmitterData add BG; does NOT exist for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" + JoH.dateTimeText(bgData.timestamp) + " raw_data=" + bgData.raw_data);
                            if (!bBenchmark) {
                                bgData.save();

                                //Check
                                if (TransmitterData.findByUuid(bgData.uuid) != null)
                                    Log.d(TAG, "syncTransmitterData: TransmitterData was saved for uuid:" + bgData.uuid);
                                else {
                                    Log.e(TAG, "syncTransmitterData: TransmitterData was NOT saved for uuid:" + bgData.uuid);
                                    return;
                                }

                                //KS the following is from G5CollectionService processNewTransmitterData()
                                Sensor sensor = Sensor.currentSensor();
                                if (sensor == null) {
                                    Log.e(TAG, "syncTransmitterData: No Active Sensor, Data only stored in Transmitter Data");
                                    return;
                                }
                                //TODO : LOG if unfiltered or filtered values are zero
                                Sensor.updateBatteryLevel(sensor, bgData.sensor_battery_level);
                                Log.i(TAG, "syncTransmitterData: BG timestamp create " + Long.toString(bgData.timestamp));//android.util.Log.i
                                BgReading bgExists = BgReading.create(bgData.raw_data, bgData.filtered_data, this, bgData.timestamp);
                                if (bgExists != null)
                                    Log.d(TAG, "syncTransmitterData BG GSON saved BG: " + bgExists.toS());
                                else
                                    Log.e(TAG, "syncTransmitterData BG GSON NOT saved");
                            }
                        }
                    }
                }
            }
            sendDataReceived(DATA_ITEM_RECEIVED_PATH,"DATA_RECEIVED_BGS count=" + entries.size(), timeOfLastBG, bBenchmark?"BM":"BG");
        }
    }

    private synchronized void syncLogData(DataMap dataMap, boolean bBenchmark) {//KS
        Log.d(TAG, "syncLogData");

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        long timeOfLastEntry = 0;
        //Log.d(TAG, "syncLogData add to Table" );
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            Log.d(TAG, "syncLogData add Table entries count=" + entries.size());
            for (DataMap entry : entries) {
                if (entry != null) {
                    //Log.d(TAG, "syncLogData add Table entry=" + entry);
                    String record = entry.getString("entry");
                    if (record != null) {
                        //Log.d(TAG, "syncLogData add Table record=" + record);
                        UserError data = gson.fromJson(record, UserError.class);
                        if (data != null) {
                            timeOfLastEntry = (long) data.timestamp + 1;
                            if (data.shortError != null && !data.shortError.isEmpty()) { //add wear prefix
                                if (!data.shortError.startsWith("wear")) {
                                    data.shortError = mPrefs.getString("wear_logs_prefix", "wear") + data.shortError;
                                    //Log.d(TAG, "syncLogData LISTENER data.shortError=" + data.shortError + " severity=" + data.severity + " timestamp=" + JoH.dateTimeText((long) data.timestamp));
                                }
                            }
                            //Log.d(TAG, "syncLogData add Entry Wear=" + data.toString());
                            //Log.d(TAG, "syncLogData WATCH data.shortError=" + data.shortError + " severity=" + data.severity + " timestamp=" + JoH.dateTimeText((long) data.timestamp));
                            if (!bBenchmark)
                                data.save();
                        }
                    }
                }
            }
            sendDataReceived(DATA_ITEM_RECEIVED_PATH,"DATA_RECEIVED_LOGS count=" + entries.size(), timeOfLastEntry, bBenchmark?"BM":"LOG");
        }
    }

    public static void sendWearToast(String msg, int length)
    {
        if ((googleApiClient != null) && (googleApiClient.isConnected())) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEARABLE_TOAST_NOTIFICATON);
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putInt("length", length);
            dataMapRequest.getDataMap().putString("msg", msg);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "No connection to wearable available for toast! "+msg);
        }
    }

    public static void sendTreatment(double carbs, double insulin, double bloodtest, double timeoffset, String timestring) {
        if ((googleApiClient != null) && (googleApiClient.isConnected())) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEARABLE_TREATMENT_PAYLOAD);
            //unique content
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putDouble("carbs", carbs);
            dataMapRequest.getDataMap().putDouble("insulin", insulin);
            dataMapRequest.getDataMap().putDouble("bloodtest", bloodtest);
            dataMapRequest.getDataMap().putDouble("timeoffset", timeoffset);
            dataMapRequest.getDataMap().putString("timestring", timestring);
            dataMapRequest.getDataMap().putBoolean("ismgdl", doMgdl(PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())));
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "No connection to wearable available for send treatment!");
        }
    }

    private static boolean doMgdl(SharedPreferences sPrefs) {
        String unit = sPrefs.getString("units", "mgdl");
        if (unit.compareTo("mgdl") == 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCreate() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        wear_integration = mPrefs.getBoolean("wear_sync", false);
        //is_using_g5 = (getDexCollectionType() == DexCollectionType.DexcomG5);
        is_using_bt = DexCollectionType.hasBluetooth();
        if (wear_integration) {
            googleApiConnect();
        }
        setSettings();
        listenForChangeInSettings();

    }

    private void listenForChangeInSettings() {
        mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d(TAG, "onSharedPreferenceChanged enter key=" + key);
                pebble_integration = mPrefs.getBoolean("pebble_sync", false);
                if(key.compareTo("bridge_battery") != 0){
                    sendPrefSettings();
                    processConnect();
                }
            }
        };
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesListener);
    }

    private void setSettings() {
        Log.d(TAG, "setSettings enter");
        pebble_integration = mPrefs.getBoolean("pebble_sync", false);
        processConnect();
        if (wear_integration) {
            if (googleApiClient == null) googleApiConnect();
            Log.d(TAG, "setSettings wear_sync changed to True.");
            sendPrefSettings();
        }
    }

    private void googleApiConnect() {
        if (googleApiClient != null && (googleApiClient.isConnected() || googleApiClient.isConnecting())) {
            googleApiClient.disconnect();
        }
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        Wearable.MessageApi.addListener(googleApiClient, this);
        if (googleApiClient.isConnected()) {
            Log.d("WatchUpdater", "API client is connected");
        } else {
            googleApiClient.connect();
        }
    }

    @Override
    public void onPeerConnected(com.google.android.gms.wearable.Node peer) {//KS
        super.onPeerConnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();
        Log.d(TAG, "onPeerConnected peer name & ID: " + name + "|" + id);
        sendPrefSettings();
        if (mPrefs.getBoolean("enable_wearG5", false)) {//watch_integration
            Log.d(TAG, "onPeerConnected call initWearData for node=" + peer.getDisplayName());
            initWearData();
            //Only stop service if Phone will rely on Wear Collection Service
            if (mPrefs.getBoolean("force_wearG5", false)) {
                Log.d(TAG, "onPeerConnected force_wearG5=true Phone stopBtService and continue to use Wear G5 BT Collector");
                stopBtService();
            } else {
                Log.d(TAG, "onPeerConnected onPeerConnected force_wearG5=false Phone startBtService");
                startBtService();
            }
        }
    }

    @Override
    public void onPeerDisconnected(com.google.android.gms.wearable.Node peer) {//KS
        super.onPeerDisconnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();
        Log.d(TAG, "onPeerDisconnected peer name & ID: " + name + "|" + id);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPrefs.getBoolean("watch_integration", false)) {
            Log.d(TAG, "onPeerDisconnected watch_integration=true Phone startBtService");
            startBtService();
        }
    }

    // Custom method to determine whether a service is running
    private boolean isServiceRunning(Class<?> serviceClass){//Class<?> serviceClass
        if (serviceClass != null) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            // Loop through the running services
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                //Log.d(TAG, "isServiceRunning: getClassName=" + service.service.getClassName() + " getShortClassName=" + service.service.getShortClassName());
                if (serviceClass.getName().equals(service.service.getClassName())) return true;
            }
        }
        return false;
    }

    private boolean isCollectorRunning() {
        Class<?> serviceClass = DexCollectionType.getCollectorServiceClass();
        if (serviceClass != null) {
            Log.d(TAG, "DexCollectionType.getCollectorServiceClass(): " + serviceClass.getName());
            return isServiceRunning(serviceClass);
        }
        return false;
    }

    private void startBtService() {//KS
        Log.d(TAG, "startBtService");
        is_using_bt = DexCollectionType.hasBluetooth();//(getDexCollectionType() == DexCollectionType.DexcomG5)
        if (is_using_bt) {
            if (!isCollectorRunning()) {
                CollectionServiceStarter.startBtService(getApplicationContext());
                Log.d(TAG, "startBtService startService");
            }
            else {
                Log.d(TAG, "startBtService collector already running!");
            }
        } else {
            Log.d(TAG, "Not starting any BT Collector service as it is not our data source");
        }
    }

    private void stopBtService() {
        Log.d(TAG, "stopService call stopService");
        CollectionServiceStarter.stopBtService(getApplicationContext());
        Log.d(TAG, "stopBtService should have called onDestroy");
    }

    private void startBtG5Service() {//KS
        Log.d(TAG, "startBtG5Service");
        //is_using_g5 = (getDexCollectionType() == DexCollectionType.DexcomG5);
        is_using_bt = DexCollectionType.hasBluetooth();
        if (is_using_bt) {
            Context myContext = getApplicationContext();
            Log.d(TAG, "startBtG5Service start G5CollectionService");
            myContext.startService(new Intent(myContext, G5CollectionService.class));
            Log.d(TAG, "startBtG5Service AFTER startService G5CollectionService");
        } else {
            Log.d(TAG, "Not starting any G5 service as it is not our data source");
        }
    }

    private void stopBtG5Service() {//KS
        Log.d(TAG, "stopBtG5Service");
        Context myContext = getApplicationContext();
        myContext.stopService(new Intent(myContext, G5CollectionService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("watchupdate-onstart",60000);

        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (wear_integration) {
            is_using_bt = DexCollectionType.hasBluetooth();//(getDexCollectionType() == DexCollectionType.DexcomG5)
            if (googleApiClient != null) {
                if (googleApiClient.isConnected()) {
                    if (ACTION_RESEND.equals(action)) {
                        resendData();
                    } else if (ACTION_OPEN_SETTINGS.equals(action)) {
                        sendNotification(OPEN_SETTINGS_PATH, "openSettings");//KS add args
                    } else if (ACTION_SEND_STATUS.equals(action)) {//KS added for HAPP
                        //https://github.com/StephenBlackWasAlreadyTaken/xDrip-Experimental
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SEND_STATUS + " externalStatusString=" + intent.getStringExtra("externalStatusString"));
                        sendStatus(intent.getStringExtra("externalStatusString"));
                    } else if (ACTION_SYNC_DB.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_DB + " Path=" + SYNC_DB_PATH);
                        sendNotification(SYNC_DB_PATH, "syncDB");
                        initWearData();
                    } else if (ACTION_START_COLLECTOR.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_START_COLLECTOR + " Path=" + START_COLLECTOR_PATH);
                        sendNotification(START_COLLECTOR_PATH, "startCOLLECTOR");
                    } else if (ACTION_STATUS_COLLECTOR.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_STATUS_COLLECTOR + " Path=" + STATUS_COLLECTOR_PATH);
                        sendNotification(STATUS_COLLECTOR_PATH, "statusCOLLECTOR");
                    } else if (ACTION_SYNC_LOGS.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_LOGS + " Path=" + SYNC_LOGS_PATH);
                        sendNotification(SYNC_LOGS_PATH, "syncLOG");
                    } else if (ACTION_CLEAR_LOGS.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_CLEAR_LOGS + " Path=" + CLEAR_LOGS_PATH);
                        sendNotification(CLEAR_LOGS_PATH, "clearLOG");
                    } else if (ACTION_SYNC_SENSOR.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_SENSOR + " Path=" + WEARABLE_SENSOR_DATA_PATH);
                        sendSensorData();
                    } else if (ACTION_SYNC_ACTIVEBTDEVICE.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_ACTIVEBTDEVICE + " Path=" + WEARABLE_ACTIVEBTDEVICE_DATA_PATH);
                        sendActiveBtDeviceData();
                    } else if (ACTION_SYNC_CALIBRATION.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_CALIBRATION + " Path=" + WEARABLE_CALIBRATION_DATA_PATH);

                        sendWearCalibrationData(sendCalibrationCount);
                        final boolean adjustPast = mPrefs.getBoolean("rewrite_history", true);
                        Log.d(TAG, "onStartCommand adjustRecentBgReadings for rewrite_history=" + adjustPast);
                        sendWearBgData(adjustPast ? 30 : 2);//wear may not have all BGs if force_wearG5=false, so send BGs from phone
                        sendData();//ensure BgReading.Last is displayed on watch

                    } else {
                        if (!mPrefs.getBoolean("force_wearG5", false)
                                && mPrefs.getBoolean("enable_wearG5", false)
                                && (is_using_bt)) { //KS only send BGs if using Phone's G5 Collector Server
                            sendWearBgData(1);
                            Log.d(TAG, "onStartCommand Action=" + " Path=" + WEARABLE_BG_DATA_PATH);
                        }
                        sendData();//ensure BgReading.Last is displayed on watch
                    }
                } else {
                    googleApiClient.connect();
                }
            } else {
                Log.wtf(TAG, "GoogleAPI client is null!");
            }
        }

        if (pebble_integration) {
            sendData();
        }

        //if ((!wear_integration)&&(!pebble_integration))
        if (!wear_integration)    // only wear sync starts this service, pebble features are not used?
        {
            Log.i(TAG,"Stopping service");
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }

        JoH.releaseWakeLock(wl);
        return START_STICKY;
    }

    private void updateWearSyncBgsCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        mWearNodeId = pickBestNodeId(connectedNodes);
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected entered");//KS
        CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        updateWearSyncBgsCapability(capabilityInfo);
                        Log.d(TAG, "onConnected onCapabilityChanged mWearNodeID:" + mWearNodeId);
                        new CheckWearableConnected().execute();
                    }
                };

        Wearable.CapabilityApi.addCapabilityListener(
                googleApiClient,
                capabilityListener,
                CAPABILITY_WEAR_APP);
        sendData();
    }

    private class CheckWearableConnected extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (googleApiClient.isConnected()) {
                if (System.currentTimeMillis() - lastRequest > 20 * 1000) { // enforce 20-second debounce period
                    lastRequest = System.currentTimeMillis();
                    //NodeApi.GetConnectedNodesResult nodes =
                    //        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                    NodeApi.GetLocalNodeResult localnodes = Wearable.NodeApi.getLocalNode(googleApiClient).await();
                    Node node = localnodes != null ? localnodes.getNode() : null;
                    String localnode = node != null ?  node.getDisplayName() + "|" + node.getId() : "";
                    Log.d(TAG, "doInBackground.  getLocalNode name=" + localnode);
                    Log.d(TAG, "doInBackground connected.  localnode=" + localnode);//KS
                    CapabilityApi.GetCapabilityResult capabilityResult =
                            Wearable.CapabilityApi.getCapability(
                                    googleApiClient, CAPABILITY_WEAR_APP,
                                    CapabilityApi.FILTER_REACHABLE).await(GET_CAPABILITIES_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    CapabilityInfo nodes;
                    if (!capabilityResult.getStatus().isSuccess()) {
                        Log.e(TAG, "doInBackground Failed to get capabilities, status: " + capabilityResult.getStatus().getStatusMessage());
                        nodes = null;
                    }
                    else {
                        nodes = capabilityResult.getCapability();
                    }
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                    boolean enable_wearG5 = sharedPrefs.getBoolean("enable_wearG5", false);
                    boolean force_wearG5 = sharedPrefs.getBoolean("force_wearG5", false);
                    String node_wearG5 = mPrefs.getString("node_wearG5", "");

                    if (nodes != null && nodes.getNodes().size() > 0) {
                        updateWearSyncBgsCapability(nodes);
                        int count = nodes.getNodes().size();
                        Log.d(TAG, "doInBackground connected.  CapabilityApi.GetCapabilityResult mWearNodeID=" + (mWearNodeId != null ? mWearNodeId : "") + " count=" + count);//KS
                        boolean isConnectedToWearable = false;
                        for (Node peer : nodes.getNodes()) {

                            //onPeerConnected
                            String wearNode = peer.getDisplayName() + "|" + peer.getId();
                            Log.d(TAG, "CheckWearableConnected onPeerConnected peer name & ID: " + wearNode);
                            if (wearNode.equals(node_wearG5)) {
                                isConnectedToWearable = true;
                                sendPrefSettings();
                            }
                            else if (node_wearG5.equals("")) {
                                isConnectedToWearable = true;
                                prefs.putString("node_wearG5", wearNode);
                                prefs.commit();
                            }
                            else
                                sendPrefSettings();
                            if (enable_wearG5) {//watch_integration
                                Log.d(TAG, "CheckWearableConnected onPeerConnected call initWearData for node=" + peer.getDisplayName());
                                initWearData();
                            }
                        }
                        if (enable_wearG5) {
                            //Only stop service if Phone will rely on Wear Collection Service
                            if (force_wearG5 && isConnectedToWearable) {
                                Log.d(TAG, "CheckWearableConnected onPeerConnected force_wearG5=true Phone stopBtService and continue to use Wear BT Collector");
                                stopBtService();
                            } else {
                                Log.d(TAG, "CheckWearableConnected onPeerConnected force_wearG5=false Phone startBtService");
                                startBtService();
                            }
                        }
                    }
                    else {
                        //onPeerDisconnected
                        Log.d(TAG, "CheckWearableConnected onPeerDisconnected");
                        if (sharedPrefs.getBoolean("wear_sync", false)) {
                            Log.d(TAG, "CheckWearableConnected onPeerDisconnected wear_sync=true Phone startBtService");
                            startBtService();
                        }
                    }
                } else {
                    Log.d(TAG, "Debounce limit hit - not sending");
                }
            } else {
                Log.d(TAG, "Not connected for sending");
                googleApiClient.connect();
            }
            return null;
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {//KS does not seem to get triggered; therefore use OnMessageReceived instead

        DataMap dataMap;

        for (DataEvent event : dataEvents) {

            if (event.getType() == DataEvent.TYPE_CHANGED) {

                String path = event.getDataItem().getUri().getPath();

                switch (path) {
                    case WEARABLE_PREF_DATA_PATH:
                        dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                        if (dataMap != null) {
                            Log.d(TAG, "onDataChanged WEARABLE_PREF_DATA_PATH dataMap=" + dataMap);
                            syncPrefData(dataMap);
                        }
                        break;
                    default:
                        Log.d(TAG, "Unknown wearable path: " + path);
                        break;
                }
            }
        }
    }

    // incoming messages from wear device
    @Override
    public void onMessageReceived(MessageEvent event) {
        DataMap dataMap;
        byte[] decomprBytes;
        Log.d(TAG, "onMessageReceived enter");
        if (wear_integration) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("watchupdate-msgrec", 60000);//KS test with 120000
            if (event != null) {
                Log.d(TAG, "wearable event path: " + event.getPath());
                switch (event.getPath()) {
                    case WEARABLE_RESEND_PATH:
                        resendData();
                        break;
                    case WEARABLE_VOICE_PAYLOAD:
                        String eventData = "";
                        try {
                            eventData = new String(event.getData(), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            eventData = "error";
                        }
                        Log.d(TAG, "Received wearable: voice payload: " + eventData);
                        if (eventData.length() > 1)
                            receivedText(getApplicationContext(), eventData);
                        break;
                    case WEARABLE_APPROVE_TREATMENT:
                        approveTreatment(getApplicationContext(), "");
                        break;
                    case WEARABLE_CANCEL_TREATMENT:
                        cancelTreatment(getApplicationContext(), "");
                        break;
                    case SYNC_BGS_PATH + "_BM"://TEST ignore only for benchmark
                    case SYNC_LOGS_PATH + "_BM":
                    case SYNC_BGS_PATH + "_BM_DUP":
                    case SYNC_LOGS_PATH + "_BM_DUP":
                    case SYNC_BGS_PATH + "_BM_RAND":
                    case SYNC_LOGS_PATH + "_BM_RAND":
                        Log.d(TAG, "onMessageReceived Ignore, just for test!");
                        decomprBytes = event.getData();
                        if (decomprBytes != null) {
                            //Log.d(TAG, "Benchmark: " + event.getPath() + "event.getData().length=" + decomprBytes.length);
                        }
                        break;
                    case SYNC_BGS_PATH + "_BM_COMPRESS"://TEST ignore only for benchmark
                    case SYNC_BGS_PATH + "_BM_DUP_COMPRESS":
                        Log.d(TAG, "onMessageReceived Ignore, just for test!");
                        decomprBytes = decompressBytes(event.getPath(), event.getData(), true);//bBenchmark
                        dataMap = DataMap.fromByteArray(decomprBytes);
                        if (dataMap != null) {
                            syncTransmitterData(dataMap, true);//bBenchmark=true
                        }
                        break;
                    case SYNC_LOGS_PATH + "_BM_COMPRESS":
                    case SYNC_LOGS_PATH + "_BM_DUP_COMPRESS":
                        Log.d(TAG, "onMessageReceived Ignore, just for test!");
                        decomprBytes = decompressBytes(event.getPath(), event.getData(), true);
                        dataMap = DataMap.fromByteArray(decomprBytes);
                        if (dataMap != null) {
                            syncLogData(dataMap, true);//bBenchmark=true
                        }
                        break;
                    case SYNC_BGS_PATH + "_BM_RAND_COMPRESS":
                    case SYNC_LOGS_PATH + "_BM_RAND_COMPRESS":
                        Log.d(TAG, "onMessageReceived Ignore, just for test!");
                        decomprBytes = decompressBytes(event.getPath(), event.getData(), true);
                        break;
                    case SYNC_BGS_PATH://KS
                        Log.d(TAG, "onMessageReceived SYNC_BGS_PATH");
                        if (event.getData() != null) {
                            dataMap = DataMap.fromByteArray(event.getData());
                            if (dataMap != null) {
                                Log.d(TAG, "onMessageReceived SYNC_BGS_PATH dataMap=" + dataMap);
                                syncTransmitterData(dataMap, false);
                            }
                        }
                        break;
                    case SYNC_LOGS_PATH:
                        Log.d(TAG, "onMessageReceived SYNC_LOGS_PATH");
                        if (event.getData() != null) {
                            dataMap = DataMap.fromByteArray(event.getData());
                            if (dataMap != null) {
                                Log.d(TAG, "onMessageReceived SYNC_LOGS_PATH dataMap=" + dataMap);
                                syncLogData(dataMap, false);
                            }
                        }
                        break;
                    case WEARABLE_INITDB_PATH:
                        Log.d(TAG, "onMessageReceived WEARABLE_INITDB_PATH");
                        initWearData();
                        break;
                    case WEARABLE_REPLYMSG_PATH:
                        Log.d(TAG, "onMessageReceived WEARABLE_REPLYMSG_PATH");
                        dataMap = DataMap.fromByteArray(event.getData());
                        if (dataMap != null) {
                            Log.d(TAG, "onMessageReceived WEARABLE_REPLYMSG_PATH dataMap=" + dataMap);
                            String msg = dataMap.getString("msg", "");
                            String action_path = dataMap.getString("action_path", "");
                            if (msg != null && !msg.isEmpty() && action_path != null && !action_path.isEmpty()) {
                                switch (action_path) {
                                    case START_COLLECTOR_PATH:
                                        JoH.static_toast_short(msg);
                                        break;
                                    case STATUS_COLLECTOR_PATH:
                                        Log.d(TAG, "onMessageReceived WEARABLE_REPLYMSG_PATH send LocalBroadcastManager ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE=" + ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE);
                                        final Intent intent = new Intent(ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE);
                                        intent.putExtra("data", dataMap.toBundle());//msg
                                        LocalBroadcastManager.getInstance(xdrip.getAppContext()).sendBroadcast(intent);
                                        break;
                                }
                            }
                        }
                        break;
                    case WEARABLE_FIELD_SENDPATH:
                        dataMap = DataMap.fromByteArray(event.getData());
                        if (dataMap != null) {
                            Log.d(TAG, "onMessageReceived WEARABLE_FIELD_SENDPATH dataMap=" + dataMap);
                            syncFieldData(dataMap);
                        }
                        break;
                    case WEARABLE_INITPREFS_PATH:
                        Log.d(TAG, "onMessageReceived WEARABLE_INITPREFS_PATH");
                        sendPrefSettings();
                        break;
                    case WEARABLE_PREF_DATA_PATH:
                        dataMap = DataMap.fromByteArray(event.getData());
                        if (dataMap != null) {
                            Log.d(TAG, "onMessageReceived WEARABLE_PREF_DATA_PATH dataMap=" + dataMap);
                            syncPrefData(dataMap);
                        }
                        break;
                    default:
                        Log.d(TAG, "Unknown wearable path: " + event.getPath());
                        super.onMessageReceived(event);
                }
            }
            JoH.releaseWakeLock(wl);
        } else {
            super.onMessageReceived(event);
        }
    }

    private byte[] decompressBytes(String pathdesc, byte[] bytes, boolean bBenchmark) {
        byte[] decomprBytes;
        if ((bytes.length > 8)
                && (bytes[0] == (byte) 0x1F)
                && (bytes[1] == (byte) 0x8B)
                && (bytes[2] == (byte) 0x08)
                && (bytes[3] == (byte) 0x00)) {
            if (bBenchmark) {
                double benchmark_time = ts();
                JoH.benchmark(null);
                decomprBytes = JoH.decompressBytesToBytes(bytes);
                String msg = pathdesc + " JoH.decompressBytesToBytes from length=" + bytes.length + " to length=" + decomprBytes.length;
                JoH.benchmark(msg);
                msg = msg + " " + (ts() - benchmark_time) + " ms";
                sendDataReceived(DATA_ITEM_RECEIVED_PATH, msg, 1, "BM");//"DATA_RECEIVED"
                return decomprBytes;
            }
            else {
                decomprBytes = JoH.decompressBytesToBytes(bytes);
                Log.d(TAG, pathdesc + " JoH.decompressBytesToBytes from length=" + bytes.length + " to length=" + decomprBytes.length);
                return decomprBytes;
            }
        }
        else {
            Log.d(TAG, "Benchmark: decompressBytesToBytes DataMap is not compressed!  Process as normal. length=" + bytes.length);
            return bytes;
        }
    }

    private void sendData() {
        BgReading bg = BgReading.last();
        if (bg != null) {
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiConnect();
            }
            if (wear_integration) {
                new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap(bg, mPrefs, new BgGraphBuilder(getApplicationContext())));
            }
        }
    }

    private void resendData() {
        Log.d(TAG, "resendData ENTER");
        if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
            googleApiConnect();
        }
        Log.d(TAG, "resendData googleApiClient connected ENTER");
        long startTime = new Date().getTime() - (60000 * 60 * 24);
        BgReading last_bg = BgReading.last();
        if (last_bg != null) {
            List<BgReading> graph_bgs = BgReading.latestForGraph(60, startTime);
            BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(getApplicationContext());
            if (!graph_bgs.isEmpty()) {
                DataMap entries = dataMap(last_bg, mPrefs, bgGraphBuilder);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
                for (BgReading bg : graph_bgs) {
                    dataMaps.add(dataMap(bg, mPrefs, bgGraphBuilder));
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);

                new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
            }
        }
    }

    private void sendNotification(String path, String notification) {//KS add args
        if (googleApiClient.isConnected()) {
            Log.d(TAG, "sendNotification Notification=" + notification + " Path=" + path);
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
            //unique content
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putString(notification, notification);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(notification, "No connection to wearable available!");
        }
    }

    private void sendStatus(String status) {
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(NEW_STATUS_PATH);
            //unique content
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putString("externalStatusString", status);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e("SendStatus", "No connection to wearable available!");
        }
    }


    private DataMap dataMap(BgReading bg, SharedPreferences sPrefs, BgGraphBuilder bgGraphBuilder) {
        Double highMark = Double.parseDouble(sPrefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(sPrefs.getString("lowValue", "70"));
        DataMap dataMap = new DataMap();

        int battery = BgSendQueue.getBatteryLevel(getApplicationContext());

        dataMap.putString("sgvString", bgGraphBuilder.unitized_string(bg.calculated_value));
        dataMap.putString("slopeArrow", bg.slopeArrow());
        dataMap.putDouble("timestamp", bg.timestamp); //TODO: change that to long (was like that in NW)
        dataMap.putString("delta", bgGraphBuilder.unitizedDeltaString(true, true, true));
        dataMap.putString("battery", "" + battery);
        dataMap.putLong("sgvLevel", sgvLevel(bg.calculated_value, sPrefs, bgGraphBuilder));
        dataMap.putInt("batteryLevel", (battery >= 30) ? 1 : 0);
        dataMap.putDouble("sgvDouble", bg.calculated_value);
        dataMap.putDouble("high", inMgdl(highMark, sPrefs));
        dataMap.putDouble("low", inMgdl(lowMark, sPrefs));
        dataMap.putInt("bridge_battery", mPrefs.getInt("bridge_battery", -1));//Used in DexCollectionService

        //TODO: Add raw again
        //dataMap.putString("rawString", threeRaw((prefs.getString("units", "mgdl").equals("mgdl"))));
        return dataMap;
    }

    private void sendPrefSettings() {//KS
        if(googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) { googleApiConnect(); }
        DataMap dataMap = new DataMap();
        String dexCollector = "None";
        boolean enable_wearG5 = false;
        boolean force_wearG5 = false;
        String node_wearG5 = "";
        wear_integration = mPrefs.getBoolean("wear_sync", false);
        if (wear_integration) {
            Log.d(TAG, "sendPrefSettings wear_sync=true");
            dexCollector = mPrefs.getString(DexCollectionType.DEX_COLLECTION_METHOD, "DexcomG5");
            enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
            force_wearG5 = mPrefs.getBoolean("force_wearG5", false);
            node_wearG5 = mPrefs.getString("node_wearG5", "");
            dataMap.putString("dex_collection_method", dexCollector);
            dataMap.putBoolean("rewrite_history", mPrefs.getBoolean("rewrite_history", true));
            dataMap.putBoolean("enable_wearG5", enable_wearG5);
            dataMap.putBoolean("force_wearG5", force_wearG5);
            dataMap.putString("node_wearG5", node_wearG5);
            dataMap.putString("share_key", mPrefs.getString("share_key", "SM00000000"));//Used by DexShareCollectionService
            //Advanced Bluetooth Settings used by G4+xBridge DexCollectionService - temporarily just use the Phone's settings
            dataMap.putBoolean("use_transmiter_pl_bluetooth", mPrefs.getBoolean("use_transmiter_pl_bluetooth", false));
            dataMap.putBoolean("automatically_turn_bluetooth_on", mPrefs.getBoolean("automatically_turn_bluetooth_on", true));
            dataMap.putBoolean("bluetooth_excessive_wakelocks", mPrefs.getBoolean("bluetooth_excessive_wakelocks", true));
            dataMap.putBoolean("close_gatt_on_ble_disconnect", mPrefs.getBoolean("close_gatt_on_ble_disconnect", true));
            dataMap.putBoolean("bluetooth_frequent_reset", mPrefs.getBoolean("bluetooth_frequent_reset", false));
            dataMap.putBoolean("bluetooth_watchdog", mPrefs.getBoolean("bluetooth_watchdog", false));
            dataMap.putInt("bridge_battery", mPrefs.getInt("bridge_battery", -1));
            dataMap.putBoolean("sync_wear_logs", mPrefs.getBoolean("sync_wear_logs", false));
        }
        is_using_bt = DexCollectionType.hasBluetooth();

        Double highMark = Double.parseDouble(mPrefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(mPrefs.getString("lowValue", "70"));
        Log.d(TAG, "sendPrefSettings enable_wearG5: " + enable_wearG5 + " force_wearG5:" + force_wearG5 + " node_wearG5:" + node_wearG5 + " dex_collection_method:" + dexCollector);
        dataMap.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
        dataMap.putString("dex_txid", mPrefs.getString("dex_txid", "ABCDEF"));
        dataMap.putString("units", mPrefs.getString("units", "mgdl"));
        dataMap.putDouble("high", highMark);//inMgdl(highMark, mPrefs));//KS Fix for mmol on graph Y-axis in wear standalone mode
        dataMap.putDouble("low", lowMark);//inMgdl(lowMark, mPrefs));//KS Fix for mmol on graph Y-axis in wear standalone mode
        dataMap.putBoolean("g5_non_raw_method",  mPrefs.getBoolean("g5_non_raw_method", false));
        dataMap.putString("extra_tags_for_logging",  Home.getPreferencesStringDefaultBlank("extra_tags_for_logging"));
        new SendToDataLayerThread(WEARABLE_PREF_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap);
    }

    private void sendSensorData() {//KS
        if (is_using_bt) {
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiConnect();
            }
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                if (wear_integration) {
                    DataMap dataMap = new DataMap();
                    Log.d(TAG, "Sensor sendSensorData uuid=" + sensor.uuid + " started_at=" + sensor.started_at + " active=" + Sensor.isActive() + " battery=" + sensor.latest_battery_level + " location=" + sensor.sensor_location + " stopped_at=" + sensor.stopped_at);
                    String json = sensor.toS();
                    Log.d(TAG, "dataMap sendSensorData GSON: " + json);

                    dataMap.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP

                    dataMap.putString("dex_txid", mPrefs.getString("dex_txid", "ABCDEF"));//KS
                    dataMap.putLong("started_at", sensor.started_at);
                    dataMap.putString("uuid", sensor.uuid);
                    dataMap.putInt("latest_battery_level", sensor.latest_battery_level);
                    dataMap.putString("sensor_location", sensor.sensor_location);

                    new SendToDataLayerThread(WEARABLE_SENSOR_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap);
                }
            }
        } else {
            Log.d(TAG, "Not sending sensor data as we are not using bt");
        }
    }

    private void sendActiveBtDeviceData() {//KS
        if (is_using_bt) {
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiConnect();
            }
            ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
            if (btDevice != null) {
                if (wear_integration) {
                    DataMap dataMap = new DataMap();
                    Log.d(TAG, "sendActiveBtDeviceData name=" + btDevice.name + " address=" + btDevice.address + " connected=" + btDevice.connected);

                    dataMap.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP

                    dataMap.putString("name", btDevice.name);
                    dataMap.putString("address", btDevice.address);
                    dataMap.putBoolean("connected", btDevice.connected);

                    new SendToDataLayerThread(WEARABLE_ACTIVEBTDEVICE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap);
                }
            }
        } else {
            Log.d(TAG, "Not sending activebluetoothdevice data as we are not using bt");
        }
    }

    private void sendWearCalibrationData(Integer count) {//KS
        try {
            if (count == null) return;
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiConnect();
            }
            Log.d(TAG, "sendWearCalibrationData");
            final Sensor sensor = Sensor.currentSensor();
            final Calibration last = Calibration.last();

            List<Calibration> latest;
            BgReading lastBgReading = BgReading.last();
            //From BgReading: if (lastBgReading.calibration_flag == true && ((lastBgReading.timestamp + (60000 * 20)) > bgReading.timestamp) && ((lastBgReading.calibration.timestamp + (60000 * 20)) > bgReading.timestamp))
            //From BgReading:     lastBgReading.calibration.rawValueOverride()
            if (lastBgReading != null && lastBgReading.calibration != null && lastBgReading.calibration_flag == true) {
                Log.d(TAG, "sendWearCalibrationData lastBgReading.calibration_flag=" + lastBgReading.calibration_flag + " lastBgReading.timestamp: " + lastBgReading.timestamp + " lastBgReading.calibration.timestamp: " + lastBgReading.calibration.timestamp);
                latest = Calibration.allForSensor();
            }
            else {
                latest = Calibration.latest(count);
            }

            if ((sensor != null) && (last != null) && (latest != null && !latest.isEmpty())) {
                Log.d(TAG, "sendWearCalibrationData latest count = " + latest.size());
                final DataMap entries = dataMap(last);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(latest.size());
                if (sensor.uuid != null) {
                    for (Calibration bg : latest) {
                        if ((bg != null) && (bg.sensor_uuid != null) && (bg.sensor_uuid.equals(sensor.uuid))) {
                            dataMaps.add(dataMap(bg));
                        }
                    }
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                if (googleApiClient != null)
                    new SendToDataLayerThread(WEARABLE_CALIBRATION_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
            } else
                Log.d(TAG, "sendWearCalibrationData latest count = 0");
        } catch (NullPointerException e) {
            Log.e(TAG, "Nullpointer exception in sendWearBgData: " + e);
        }
    }

    private DataMap dataMap(Calibration bg) {//KS
        DataMap dataMap = new DataMap();
        String json = bg.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("bgs", json);
        return dataMap;
    }

    private void sendWearBgData(Integer count) {//KS
        try {
            if (count == null) return;
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiConnect();
            }
            Log.d(TAG, "sendWearBgData");
            final BgReading last = BgReading.last();
            final List<BgReading> latest = BgReading.latest(count);
            if ((last != null) && (latest != null && !latest.isEmpty())) {
                Log.d(TAG, "sendWearBgData latest count = " + latest.size());
                final DataMap entries = dataMap(last);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(latest.size());
                final Sensor sensor = Sensor.currentSensor();
                if ((sensor != null) && (sensor.uuid != null)) {
                    for (BgReading bg : latest) {
                        if ((bg != null) && (bg.sensor_uuid != null) && (bg.sensor_uuid.equals(sensor.uuid))) {
                            dataMaps.add(dataMap(bg));
                        }
                    }
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                if (googleApiClient != null)
                    new SendToDataLayerThread(WEARABLE_BG_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
            } else
                Log.d(TAG, "sendWearBgData lastest count = 0");
        } catch (NullPointerException e) {
            Log.e(TAG, "Nullpointer exception in sendWearBgData: " + e);
        }
    }

    private DataMap dataMap(BgReading bg) {//KS
        DataMap dataMap = new DataMap();
        //KS Fix for calibration_uuid not being set in Calibration.create which updates bgReading to new calibration ln 497
        //if (bg.calibration_flag == true) {
        //    bg.calibration_uuid = bg.calibration.uuid;
        //}
        dataMap.putString("calibrationUuid", bg.calibration.uuid);

        String json = bg.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("bgs", json);
        return dataMap;
    }

    private void initWearData() {
        if (is_using_bt) {
            Log.d(TAG, "***initWearData***");
            sendSensorData();
            sendActiveBtDeviceData();
            sendWearCalibrationData(sendCalibrationCount);
            sendWearBgData(sendBgCount);
            sendData();//ensure BgReading.Last is displayed on watch
        } else {
            Log.d(TAG, "Not doing initWearData as we are not using G5 as data source");
        }
    }

    private long sgvLevel(double sgv_double, SharedPreferences prefs, BgGraphBuilder bgGB) {
        Double highMark = Double.parseDouble(prefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(prefs.getString("lowValue", "70"));
        if (bgGB.unitized(sgv_double) >= highMark) {
            return 1;
        } else if (bgGB.unitized(sgv_double) >= lowMark) {
            return 0;
        } else {
            return -1;
        }
    }

    private double inMgdl(double value, SharedPreferences sPrefs) {
        if (!doMgdl(sPrefs)) {
            return value * Constants.MMOLL_TO_MGDL;
        } else {
            return value;
        }

    }

    @Override
    public void onDestroy() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        if (mPrefs != null && mPreferencesListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPreferencesListener);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}
