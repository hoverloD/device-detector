package com.unity3d.player;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.FrameLayout.LayoutParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends UnityPlayerActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public ImageButton help_btn, locate_btn, tips_btn;
    public ImageButton start_btn, end_btn, send_btn;
    public int tip_count, tip_size;
    Set<String> macs = new HashSet<>();
    public ArrayList<String> items = new ArrayList<>();
    public ArrayList<String> devices = new ArrayList<>();
    public ArrayList<Integer> images = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUnityPlayer = new UnityPlayer(this, this);

        setContentView(R.layout.activity_main);
        FrameLayout layout = (FrameLayout) findViewById(R.id.unity_fragment);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.addView(mUnityPlayer.getView(), 0, layoutParams);

        tip_count = 0;
        String[] Tips = {
                getString(R.string.tip1),
                getString(R.string.tip2),
                getString(R.string.tip3),
                getString(R.string.tip4),
                getString(R.string.tip5),
                getString(R.string.tip6),
                getString(R.string.tip7),
                getString(R.string.tip8)
        };
        tip_size = Tips.length;

        help_btn = (ImageButton) findViewById(R.id.help_button);
        help_btn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.help_desc));
            builder.setMessage(getString(R.string.help_info));

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });

        locate_btn = (ImageButton) findViewById(R.id.occlusion_btn);
        locate_btn.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, getString(R.string.mode_change), Toast.LENGTH_SHORT).show();
            Occlusion();
        });

        tips_btn = (ImageButton) findViewById(R.id.tips_btn);
        tips_btn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

//            textView没学会
//            TextView textView = new TextView(this);
//            textView.setText(Tips[(tip_count++) % tip_size]);
//            builder.setMessage(textView.getText().toString());

            builder.setTitle(getString(R.string.tips_desc));
            // 本来想随机展示一条tip，整个列表全部展示过一轮后，开启下一轮随机。
            // 但是tips前后连贯性有点强，就直接顺序展示了。
            builder.setMessage(Tips[(tip_count++) % tip_size])
                .setPositiveButton(getString(R.string.one_more_tip), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if((tip_count + 1) % tip_size == 0) {
                            // 接下来模拟点击，展示最后一条（更新计划）
                            Toast.makeText(MainActivity.this, getString(R.string.round), Toast.LENGTH_SHORT).show();
                        }
                        tips_btn.performClick();
                    }
                });

            final AlertDialog alertDialog = builder.create();
//            设置透明度不管用。给mainActivity加了个theme： android:theme="@style/TransTheme"，
//            把UI搞没了orz。但是原来UI的对话框不支持半透明
//            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
//                @Override
//                public void onShow(DialogInterface dialogInterface) {
//                    Window window = alertDialog.getWindow();
//                    if (window != null) {
//                        window.setDimAmount(0.7f);
//                    }
//                }
//            });
            alertDialog.show();
        });


        start_btn = (ImageButton) findViewById(R.id.start_btn);
        start_btn.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "moStart", Toast.LENGTH_SHORT).show();
            byte[] byteArray = "moStart".getBytes();
            UnityPlayerActivity.write(device, SERVICE_UUID,CHARACTERISTIC_UUID, byteArray, false);
        });

//        end_btn = (ImageButton) findViewById(R.id.end_btn);
//        end_btn.setOnClickListener(v -> {
//            Toast.makeText(MainActivity.this, "关闭监听", Toast.LENGTH_SHORT).show();
//            byte[] byteArray = "moEnd".getBytes();
//            UnityPlayerActivity.write(device, SERVICE_UUID,CHARACTERISTIC_UUID, byteArray, false);
//        });

        send_btn = (ImageButton) findViewById(R.id.send_btn);
        send_btn.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "moSend", Toast.LENGTH_SHORT).show();
            byte[] byteArray = "moSend".getBytes();
            UnityPlayerActivity.write(device, SERVICE_UUID,CHARACTERISTIC_UUID, byteArray, false);
        });



        // 设备定位展示列表
        ImageButton showListButton = findViewById(R.id.list_fab);
        showListButton.setOnClickListener(v -> {
            showList();
        });

        // 初始列表项。不在showList调用后更新了，因为设置初始readStr可能被刷掉
        items.add(getString(R.string.initial_list));
        devices.add("{ \"mac\":\"" + "initial" + "\", \"type\": \"initial\", \"x\": 0, \"y\": 0, \"z\": 0 }");
        int typeId = this.getResources().getIdentifier("initial", "drawable", this.getPackageName());
        images.add(typeId);
    }

    private String getDeviceType(String type) {
        String type_;
        switch(type) {
            case "camera":
                type_ = getString(R.string.type_camera);
                break;
            case "scamera":
                type_ = getString(R.string.type_scamera);
                break;
            case "plug":
                type_ = getString(R.string.type_plug);
                break;
            case "doorbell":
                type_ = getString(R.string.type_doorbell);
                break;
            default:
                type_ = getString(R.string.type_unknown);
        }
        return type_;
    }

    public void showList() {
        // 解析read拿到的json
        boolean changed = false;
        String jsonString = UnityPlayerActivity.readStr;
//        String jsonString = "[{ \"devices\": [ { \"mac\":\"ff:ff:ff:ff:ff:ff\", \"type\": \"camera\", \"x\": 0.35, \"y\": 0, \"z\": 0.1541 }, { \"mac\":\"ff:ff:ff:ff:ff:fe\", \"type\": \"scamera\", \"x\": -0.541, \"y\": 0, \"z\": -0.1 } ] }]";
        String[] Items = new String[0];
        String[] Devices = new String[0];
        Integer[] Images = new Integer[0];

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            JSONArray devicesArray = jsonObject.getJSONArray("devices");
            // 遍历每个设备
            for (int i = 0; i < devicesArray.length(); i++) {
                // 获取设备对象
                JSONObject deviceObject = devicesArray.getJSONObject(i);
                String jsonStr = deviceObject.toString();

                // 获取设备属性
                String mac = deviceObject.getString("mac");
                String type = deviceObject.getString("type");
                double x = deviceObject.getDouble("x");
                double y = deviceObject.getDouble("y");
                double z = deviceObject.getDouble("z");
                Log.d(TAG, "device: " + mac + " " + x + " " + y + " " + z);
                // 新mac代表新设备，在列表中新增，在unity部署预制件
                if(!macs.contains(mac)) {
                    DeviceLoc(jsonStr);
                    macs.add(mac);
                    changed = true; // 列表里加了设备，标记为true

                    String type_ = getDeviceType(type);
                    items.add("MAC:\t" + mac + "\t\t" + type_ + "\n" + getString(R.string.coordinate) + "\t" + x + ", " + y + ", " + z);
                    devices.add(jsonStr);
                    int typeId = this.getResources().getIdentifier(type, "drawable", this.getPackageName());
                    images.add(typeId);
                }
                // 暂无更新功能（主要是Unity那还没做，这里把macs从集合换成列表就可以）
//                else { // 内容更新
//                    DeviceLoc_Re(jsonStr);
//                    changedID = macs.indexOf(mac);
//                    String type_ = getDeviceType(type);
//
//                    items.set(changedID, "MAC:\t" + mac + "\t\t" + type_ + "\n" + getString(R.string.coordinate) + "\t" + x + ", " + y + ", " + z);
//                    devices.set(changedID, jsonStr);
//                    int typeId = this.getResources().getIdentifier(type, "drawable", this.getPackageName());
//                    images.set(changedID, typeId);
//                }
            }
            if (changed){
                // 说明不是初始列表了，修改初始项文字
                items.set(0, getString(R.string.back_to_start));
            }
            Items = items.toArray(new String[0]);
            Devices = devices.toArray(new String[0]);
            Images = images.toArray(new Integer[0]);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        // 设置列表数据源
        String[] finalItems = Items;
        Integer[] finalImages = Images;
        String[] finalDevices = Devices;

        // 设置列表每一列的布局和内容
        builder.setAdapter(new ArrayAdapter<String>(this, R.layout.device_list, finalItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_list, parent, false);
                }

                // 设置每一列的文本和图片
                TextView text = (TextView) convertView.findViewById(android.R.id.text1);
                text.setText(finalItems[position]);
                ImageView image = (ImageView) convertView.findViewById(android.R.id.icon);
                image.setImageResource(finalImages[position]);

                // 为每个列表项设置点击事件
                convertView.setOnClickListener(v -> {
                    String item = finalItems[position];
                    // getString(R.string.initial_list)
                    Toast.makeText(MainActivity.this, getString(R.string.you_chose) + item, Toast.LENGTH_SHORT).show();
                    // 点击某一列后的响应事件，Aimer修改目标
                    Aimer(finalDevices[position]);
                    // 点击后没法自动关掉，那点四周空白也可以，问题不大
//                    AlertDialog dialog = builder.create();
//                    dialog.dismiss();
                });

                return convertView;
            }
        }, null);

        // 创建并显示AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

