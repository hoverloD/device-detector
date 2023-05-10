package com.unity3d.player;

import android.app.AlertDialog;
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

    public ImageButton help_btn;
    public ImageButton locate_btn;
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

        help_btn = (ImageButton) findViewById(R.id.help_button);
        help_btn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.help_desc));
            builder.setMessage(getString(R.string.help_info));

//            Drawable drawable = new ColorDrawable(Color.TRANSPARENT);
//            drawable.setAlpha(200);

            AlertDialog alertDialog = builder.create();
//            alertDialog.getWindow().setBackgroundDrawable(drawable);
            alertDialog.show();
        });

        locate_btn = (ImageButton) findViewById(R.id.occlusion_btn);
        locate_btn.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, getString(R.string.mode_change), Toast.LENGTH_SHORT).show();
            Occlusion();
        });

        // 设备定位展示列表
        ImageButton showListButton = findViewById(R.id.list_fab);
        showListButton.setOnClickListener(v -> {
            showList();
        });


    }

    public void showList() {
        // 解析read拿到的json
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
                // 新mac代表新设备，在列表中新增，在unity部署预制件。否则对此条不做处理
                if(!macs.contains(mac)) {
                    DeviceLoc(jsonStr);
                    macs.add(mac);
                    items.add(mac);
                    devices.add(jsonStr);
                    int typeId = this.getResources().getIdentifier(type, "drawable", this.getPackageName());
                    images.add(typeId);
                }
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
                    Toast.makeText(MainActivity.this, getString(R.string.you_chose) +
                            ((item).equals(getString(R.string.initial_list)) ? getString(R.string.starting_point) : item), Toast.LENGTH_SHORT).show();
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

