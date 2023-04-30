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

public class MainActivity extends UnityPlayerActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public ImageButton help_btn;
    public Button locate_btn;


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
            builder.setTitle("使用帮助");
            builder.setMessage("本应用开启后自动连接树莓派并发送VIO数据，请先开启树莓派的蓝牙确保顺利连接\n" +
                    "\n请手持此设备和树莓派在屋内走动，收集RSSI与VIO信息\n" +
                    "\n如果树莓派发现了新设备，会弹窗通知您（还没做，要改notify），请耐心等待\n" +
                    "\n点击中部按钮，获取树莓派的定位文件，以更新定位设备列表\n" +
                    "\n点击右侧按钮，打开定位设备列表\n" +
                    "\nAR需要根据特征明显的点定位，尽量不要使相机拍摄画面过于单调（例：大片纯色墙壁/地板、遮挡镜头导致画面纯黑）或者发生剧烈晃动，请在光线充足的环境下使用\n");

//            Drawable drawable = new ColorDrawable(Color.TRANSPARENT);
//            drawable.setAlpha(200);

            AlertDialog alertDialog = builder.create();
//            alertDialog.getWindow().setBackgroundDrawable(drawable);
            alertDialog.show();
        });

        // read获取设备定位
        locate_btn = (Button) findViewById(R.id.locate_btn);
        locate_btn.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "获取设备定位中", Toast.LENGTH_SHORT).show();
            UnityPlayerActivity.read(UnityPlayerActivity.device, UnityPlayerActivity.SERVICE_UUID, UnityPlayerActivity.CHARACTERISTIC_UUID);
        });

        // 设备定位展示列表
        Button showListButton = findViewById(R.id.show_list_button);
        showListButton.setOnClickListener(v -> {
            showList();
//            这样会报错，所以还是用俩按钮吧
//            UnityPlayerActivity.read(UnityPlayerActivity.device, UnityPlayerActivity.SERVICE_UUID, UnityPlayerActivity.CHARACTERISTIC_UUID);
//            Timer timer = new Timer();
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    showList();
//                }
//            }, 2000);
        });
    }

    public void showList() {
        // 解析read拿到的json
        String jsonString = UnityPlayerActivity.readStr;
//        String jsonString = "[{ \"devices\": [ { \"mac\":\"ff:ff:ff:ff:ff:ff\", \"type\": \"camera\", \"x\": 0.1541, \"y\": 0.1541, \"z\": -0.1541 }, { \"mac\":\"ff:ff:ff:ff:ff:fe\", \"type\": \"sensor\", \"x\": -3.1541, \"y\": -2.1541, \"z\": 0.1541 } ] }]";
        ArrayList<String> items = new ArrayList<>();
        ArrayList<Integer> images = new ArrayList<>();
        String[] Items = new String[0];
        Integer[] Images = new Integer[0];

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            JSONArray devicesArray = jsonObject.getJSONArray("devices");
            // 遍历每个设备
            for (int i = 0; i < devicesArray.length(); i++) {
                // 获取设备对象
                JSONObject deviceObject = devicesArray.getJSONObject(i);
                DeviceLoc(deviceObject.toString());

                // 获取设备属性
                String mac = deviceObject.getString("mac");
                String type = deviceObject.getString("type");
                double x = deviceObject.getDouble("x");
                double y = deviceObject.getDouble("y");
                double z = deviceObject.getDouble("z");
                Log.d(TAG, "device: " + mac + " " + x + " " + y + " " + z);
                items.add(mac);
                int typeId = this.getResources().getIdentifier(type, "drawable", this.getPackageName());
                images.add(typeId);
            }
            Items = items.toArray(new String[0]);
            Images = images.toArray(new Integer[0]);
//            Log.d(TAG, Items[0]); Log.d(TAG, Images[0].toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        // 设置列表数据源
        String[] finalItems = Items;
        Integer[] finalImages = Images;
//        builder.setItems(finalItems, (dialog, which) -> {
//            // 点击某一列后的响应事件
//            String item = finalItems[which];
//            Toast.makeText(MainActivity.this, "You clicked: " + item, Toast.LENGTH_SHORT).show();
//        });

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
                    Toast.makeText(MainActivity.this, "You clicked: " + item, Toast.LENGTH_SHORT).show();
                    // TODO 唤起unity指路箭头

                    // 想点击后自动关掉，好像关不掉，那点四周空白好了，问题不大
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

