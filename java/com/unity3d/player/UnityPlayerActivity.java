package com.unity3d.player;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.*;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class UnityPlayerActivity extends Activity implements IUnityPlayerLifecycleEvents {
    protected UnityPlayer mUnityPlayer; // don't change the name of this variable; referenced from native code

    private static final String TAG = UnityPlayerActivity.class.getSimpleName();
    private static final String MAC = "B8:27:EB:4F:E9:EC";
    static final String SERVICE_UUID = "ffffffff-ffff-ffff-ffff-fffffffffff0";
    static final String CHARACTERISTIC_UUID = "0000FFF1-0000-1000-8000-00805F9B34FB";
    static String readStr;
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    static BleDevice device;

    // Override this in your custom UnityPlayerActivity to tweak the command line arguments passed to the Unity Android Player
    // The command line arguments are passed as a string, separated by spaces
    // UnityPlayerActivity calls this from 'onCreate'
    // Supported: -force-gles20, -force-gles30, -force-gles31, -force-gles31aep, -force-gles32, -force-gles, -force-vulkan
    // See https://docs.unity3d.com/Manual/CommandLineArguments.html
    // @param cmdLine the current command line arguments, may be null
    // @return the modified command line string or null
    protected String updateUnityCommandLineArguments(String cmdLine) {
        return cmdLine;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String cmdLine = updateUnityCommandLineArguments(getIntent().getStringExtra("unity"));
        getIntent().putExtra("unity", cmdLine);

        mUnityPlayer = new UnityPlayer(this, this);
        setContentView(mUnityPlayer);
        mUnityPlayer.requestFocus();
        // 检查权限，动态请求
        checkPermissions();

        // 只需初始化一次，在使用库中的方法前调用，并非必须在Application中调用。
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setSplitWriteNum(20)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MultiWindowSupport.getAllowResizableWindow(this) && !MultiWindowSupport.isMultiWindowModeChangedToTrue(this))
            return;

        mUnityPlayer.resume();
        connect(MAC);
    }


    // 动态权限获取
    public void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
//            不enable，不就又引用空对象虚函数了？？但是enable后还是闪退，直接log都没了
//            bluetoothAdapter.enable();
//            Log.d(TAG, "等待用户开启蓝牙...");
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "ACCESS_FINE_LOCATION: PERMISSION_GRANTED...");
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            Toast.makeText(UnityPlayerActivity.this, "等待同意权限...", Toast.LENGTH_SHORT).show();
            String[] deniedPermissions = permissionDeniedList.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }


// 在扫描设备之前，可以配置扫描规则，筛选出与程序匹配的设备
// 不配置的话均为默认参数
// 在2.1.2版本及之前，必须先配置过滤规则再扫描；在2.1.3版本之后可以无需配置，开启默认过滤规则的扫描。
    private void setScanRule() {
        String[] names = {"Raspberrypi77"};

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
//                .setServiceUuids(SERVICE_UUID)      // 只扫描指定的服务的设备，可选
//                .setDeviceName(true, names)   // 只扫描指定广播名的设备，可选
                .setDeviceMac(MAC)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(false)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                // 开始扫描（主线程）
                Log.d(TAG, "开始扫描");
//                Toast.makeText(UnityPlayerActivity.this, "开始扫描", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                // 扫描到一个符合扫描规则的BLE设备（主线程）
                Log.d(TAG, "扫描到一个符合扫描规则的BLE设备");
//                Toast.makeText(UnityPlayerActivity.this, "扫描到一个符合扫描规则的BLE设备", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                // 扫描结束，列出所有扫描到的符合扫描规则的BLE设备（主线程）
                Log.d(TAG, "Scan list: " + scanResultList.toString());
            }
        });
    }


    // https://github.com/Jasonchenlijian/FastBle/wiki/%E6%89%AB%E6%8F%8F%E5%8F%8A%E8%BF%9E%E6%8E%A5#%E9%80%9A%E8%BF%87mac%E8%BF%9E%E6%8E%A5
    // 通过已知设备Mac直接连接。此方法可以不经过扫描，尝试直接连接周围复合该Mac的BLE设备。
    // 在很多使用场景，我建议APP保存用户惯用设备的Mac，然后使用该方法进行连接可以大大提高连接效率。
    private void connect(final String mac) {
        BleManager.getInstance().connect(mac, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                Log.d(TAG, "连接中...");
                Toast.makeText(UnityPlayerActivity.this, "蓝牙连接中...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Toast.makeText(UnityPlayerActivity.this, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Toast.makeText(UnityPlayerActivity.this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                device = bleDevice;

                setMTU(bleDevice, 256); // 树莓派（Bleno？）最高支持256,写更高返回也是256

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // 开了notify，read也还是读不全
                        UnityPlayerActivity.notify(device, SERVICE_UUID, CHARACTERISTIC_UUID, false);
                    }
                }, 1000); // 延迟 1000 毫秒（1秒）
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Toast.makeText(UnityPlayerActivity.this, "蓝牙断开连接", Toast.LENGTH_SHORT).show();
                stopNotify(device, SERVICE_UUID, CHARACTERISTIC_UUID);
            }
        });
    }


    private void onPermissionGranted(String permission) {
        if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
            if (!checkGPSIsOpen()) {
                new AlertDialog.Builder(this)
                    .setTitle(R.string.notifyTitle)
                    .setMessage(R.string.gpsNotifyMsg)
                    .setNegativeButton(R.string.cancel,
                            (dialog, which) -> finish())
                    .setPositiveButton(R.string.setting,
                            (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                            })
                    .setCancelable(false)
                    .show();
            }
        }
    }

    public static void notify(BleDevice bleDevice, String uuid_service, String uuid_notify, boolean useCharacteristicDescriptor) {
        BleManager.getInstance().notify(
            bleDevice,
            uuid_service,
            uuid_notify,
            new BleNotifyCallback() {
                @Override
                public void onNotifySuccess() {
                    Log.d(TAG, "打开通知操作成功");
                }

                @Override
                public void onNotifyFailure(BleException exception) {
                    Log.d(TAG, "打开通知操作失败");
                }

                @Override
                public void onCharacteristicChanged(byte[] data) {
                    // 打开通知后，设备发过来的数据将在这里出现
                    String str = new String(data);
                    Log.d(TAG, "notify: " + str);
                    // TODO 改notify，发现新设备提示
                }
            });
    }

    public static void stopNotify(BleDevice bleDevice,
                                  String uuid_service,
                                  String uuid_notify) {
        BleManager.getInstance().stopNotify(bleDevice, uuid_service, uuid_notify);
    }


    // 通过notify获取新设备定位不是更好？
    // 发现bleno的notify没有offset参数用于分包传送，read、write有，或许代表无法发送长信息？换回read...
    // 一包最多发253字符（MTU是256，减去包头包尾3）
    // 这个read也有点迷。。一次为什么只打印593个字符
    public static void read(BleDevice bleDevice,
                            String uuid_service,
                            String uuid_read) {
        BleManager.getInstance().read(
            bleDevice,
            uuid_service,
            uuid_read,
            new BleReadCallback() {
                @Override
                public void onReadSuccess(byte[] data) {
                    // 读特征值数据成功
                    // node的string to buffer只支持utf8，不用写后面
                    readStr = new String(data, StandardCharsets.UTF_8);
                    Log.d("BLERead", readStr);

                    // 我以为Log字数有限制就写了下文件，真就这么长
//                        FileWriter writer;
//                        try {
//                            writer = new FileWriter("/storage/emulated/0/Download/test.txt");
//                            writer.write("");//清空原文件内容
//                            writer.write(str);
//                            writer.flush();
//                            writer.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                }

                @Override
                public void onReadFailure(BleException exception) {
                    // 读特征值数据失败
                    Log.d("BLERead", "读特征值数据失败");
                }
            });
    }


    public static void write(BleDevice bleDevice,
                             String uuid_service,
                             String uuid_write,
                             byte[] data,
                             boolean split) {
        BleManager.getInstance().write(
            bleDevice,
            uuid_service,
            uuid_write,
            data,
            split,
            new BleWriteCallback() {
                @Override
                public void onWriteSuccess(int current, int total, byte[] justWrite) {
//                        Log.d(TAG, "发送数据到设备成功");
                }

                @Override
                public void onWriteFailure(BleException exception) {
//                        Log.d(TAG, "发送数据到设备失败");
                }
            });
    }
//    boolean sendNextWhenLastSuccess,
//    long intervalBetweenTwoPackage
//    - 在没有扩大MTU及扩大MTU无效的情况下，当遇到超过20字节的长数据需要发送的时候，需要进行分包。参数`boolean split`表示是否使用分包发送；无`boolean split`参数的`write`方法默认对超过20字节的数据进行分包发送。
//    - 关于`onWriteSuccess`回调方法: `current`表示当前发送第几包数据，`total`表示本次总共多少包数据，`justWrite`表示刚刚发送成功的数据包。
//    - 对于分包发送的辅助策略，可以选择发送上一包数据成功之后发送下一包，或直接发送下一包，参数`sendNextWhenLastSuccess`表示是否待收到`onWriteSuccess`之后再进行下一包的发送。默认true。
//    - 参数`intervalBetweenTwoPackage`表示延时多长时间发送下一包，单位ms，默认0。


    public static void setMTU(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure (BleException exception){
                //
                Log.d(TAG, "设置MTU失败");
            }

            @Override
            public void onMtuChanged (int mtu){
                // 设置MTU成功，并获得当前设备传输支持的MTU值
                Log.d(TAG, "设置MTU成功，并获得当前设备传输支持的MTU值" + mtu);
            }
        });
    }

    public void DeviceLoc(String location) {
        Log.i("Unity", "获得设备定位坐标  " + location);

        // gameobject, method, 对应方法的参数
        UnityPlayer.UnitySendMessage("SimpleAR", "Locator", location);
    }

    public void SendVIO(String json) {
        Log.v("Unity", "相机定位" + json);
        byte[] byteArray = json.getBytes();
        UnityPlayerActivity.write(device, SERVICE_UUID,CHARACTERISTIC_UUID, byteArray, false);
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_LOCATION) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        onPermissionGranted(permissions[i]);
                    }
                }
            }
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    // 啥意思
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                Log.d(TAG, "onActivityResult");
            }
        }
    }

    private void setMtu(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }


    // When Unity player unloaded move task to background
    @Override
    public void onUnityPlayerUnloaded() {
        moveTaskToBack(true);
    }

    // Callback before Unity player process is killed
    @Override
    public void onUnityPlayerQuitted() {
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        mUnityPlayer.newIntent(intent);
    }

    // Low Memory Unity
    @Override public void onLowMemory()
    {
        super.onLowMemory();
        mUnityPlayer.lowMemory();
    }

    // Trim Memory Unity
    @Override public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL)
        {
            mUnityPlayer.lowMemory();
        }
    }

    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    @Override
    protected void onDestroy() {
        mUnityPlayer.destroy();
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    // If the activity is in multi window mode or resizing the activity is allowed we will use
    // onStart/onStop (the visibility callbacks) to determine when to pause/resume.
    // Otherwise it will be done in onPause/onResume as Unity has done historically to preserve
    // existing behavior.
    @Override
    protected void onStop() {
        super.onStop();

        if (!MultiWindowSupport.getAllowResizableWindow(this))
            return;

        mUnityPlayer.pause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!MultiWindowSupport.getAllowResizableWindow(this))
            return;

        mUnityPlayer.resume();
    }

    // Pause Unity
    @Override
    protected void onPause() {
        super.onPause();

        MultiWindowSupport.saveMultiWindowMode(this);

        if (MultiWindowSupport.getAllowResizableWindow(this))
            return;

        mUnityPlayer.pause();
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }


    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
    /*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }
}
