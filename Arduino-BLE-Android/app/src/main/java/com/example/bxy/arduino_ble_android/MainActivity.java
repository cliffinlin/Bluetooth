package com.example.bxy.arduino_ble_android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.style.UpdateAppearance;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "看我";
    private TextView TvDeviceName;
    private TextView TvDeviceAddress;
    private TextView TvState;
    private TextView TvData;
    private Button BtOpen;
    private Button BtClose;

    private MyExpandableListViewAdapter adapter;
    private ExpandableListView mlistview;
    private Map<String, List<String>> dataset = new HashMap<>();
    private String[] parentList = new String[]{"Service1", "Service2", "Service3", "Service4"};
    ArrayList<ArrayList<String>> childrenList = new ArrayList<ArrayList<String>>();


    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mDeviceAddress = "E9:62:39:D0:53:33";
    private String mDeviceName="";

    private Handler mhandler;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //判断手机是否支持蓝牙4.0
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // 尝试打开蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "打开蓝牙失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        TvDeviceName = (TextView) findViewById(R.id.device_name);
        TvDeviceAddress = (TextView) findViewById(R.id.device_address);
        TvDeviceAddress.setText(mDeviceAddress);
        TvState = (TextView) findViewById(R.id.connection_state);
        TvState.setText("Not Connected");
        TvData = (TextView) findViewById(R.id.data_value);
        TvData.setText("");
        BtClose = (Button) findViewById(R.id.Btclose);
        BtClose.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                if(mBluetoothGatt!=null)
                    mBluetoothGatt.close();
                    TvState.setText("Disconnected");
                    BtClose.setEnabled(false);
                    BtOpen.setEnabled(true);
            }
        });
        BtOpen = (Button) findViewById(R.id.Btopen);
        BtOpen.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                if(mBluetoothGatt==null)
                    initialGatt();
            }
        });


        mlistview = (ExpandableListView) findViewById(R.id.gatt_services_list);
        adapter = new MyExpandableListViewAdapter();
        mlistview.setAdapter(adapter);
        //创建属于主线程的handler
        mhandler=new Handler();
        initialData();
        //initialGatt();

    }

    //初始化ExpandableListView
    private void initialData(){
        Log.i(TAG,"初始化ExpandableListView");
        ArrayList<String> childrenList1 = new ArrayList<>();
        ArrayList<String> childrenList2 = new ArrayList<>();
        ArrayList<String> childrenList3 = new ArrayList<>();
        ArrayList<String> childrenList4 = new ArrayList<>();
        childrenList.add(childrenList1);
        childrenList.add(childrenList2);
        childrenList.add(childrenList3);
        childrenList.add(childrenList4);

        childrenList1.add(parentList[0] + "-" + "first");
        childrenList1.add(parentList[0] + "-" + "second");
        childrenList1.add(parentList[0] + "-" + "third");
        childrenList2.add(parentList[1] + "-" + "first");
        childrenList2.add(parentList[1] + "-" + "second");
        childrenList2.add(parentList[1] + "-" + "third");
        childrenList3.add(parentList[2] + "-" + "first");
        childrenList3.add(parentList[2] + "-" + "second");
        childrenList3.add(parentList[2] + "-" + "third");
        childrenList4.add(parentList[3] + "-" + "first");
        childrenList4.add(parentList[3] + "-" + "second");
        childrenList4.add(parentList[3] + "-" + "third");
        dataset.put(parentList[0], childrenList1);
        dataset.put(parentList[1], childrenList2);
        dataset.put(parentList[2], childrenList3);
        dataset.put(parentList[3], childrenList4);
        adapter.notifyDataSetChanged();
    }
    /*更新ExpandableListView*/
    private void updateData() {

        Log.i(TAG,"更新数据");
        // 构建Runnable对象，在runnable中更新界面
        final Runnable   runnableUi=new  Runnable(){
            @Override
            public void run() {
                TvState.setText("Connected");
                BtClose.setEnabled(true);
                BtOpen.setEnabled(false);
                //childrenList.clear();
                int index=0;
                for(List<String> list : childrenList){
                    dataset.put(parentList[index],list);
                    index++;
                }

                adapter.notifyDataSetChanged();
            }
        };

        new Thread(){
            public void run(){
                mhandler.post(runnableUi);
            }
        }.start();

        Log.i(TAG,"更新完毕"+"Parent.Size="+parentList.length+",Children.size="+childrenList.size());
    }

    /**连接Gatt**/
    private void initialGatt(){
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        TvDeviceName.setText(device.getName());
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                TvState.setText("Disconnected");
                BtClose.setEnabled(false);
                BtOpen.setEnabled(true);
                Log.i(TAG, "Disconnected from GATT server.");

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "发现新的Service.");
                try {
                    Thread.sleep(200);//延迟发送，否则第一次消息会不成功
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i(TAG,String.valueOf(childrenList.size()));
                initCharacteristic();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "读取Characteristic.");
                updateData();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged");
        }
    };

    /*接收到service*/
    private void initCharacteristic(){
        if (mBluetoothGatt == null) throw new NullPointerException();
        List<BluetoothGattService> services = mBluetoothGatt.getServices();
        if(services == null) return;
        String uuid= null;
        String unknownServiceString = "unknown_service";
        String unknownCharaString = "unknown_characteristic";

        int index_parent= 0;
        for(BluetoothGattService service : services){
            uuid = service.getUuid().toString();
            parentList[index_parent]=SampleGattAttributes.lookup(uuid, unknownServiceString);
            Log.i(TAG,parentList[index_parent]);
            List<BluetoothGattCharacteristic> chars = service.getCharacteristics();

            /*先clear原先的数据，再add,要考虑第一次的情况*/
            if(childrenList.get(index_parent)!=null){
                childrenList.get(index_parent).clear();
                childrenList.get(index_parent).addAll(childrenList.get(index_parent));
            }
            /**遍历Characteristic**/
            for(BluetoothGattCharacteristic characterstic : chars){
                byte[] data = characterstic.getValue();
            /*    if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    child_list.add(new String(data) + "\n" + stringBuilder.toString());
                    Log.i(TAG,new String(data) + "\n" + stringBuilder.toString());
                }*/
                childrenList.get(index_parent).add(characterstic.toString());
                Log.i(TAG,characterstic.toString());
            }
            index_parent++;
        }
        updateData();
    }

    private class MyExpandableListViewAdapter extends BaseExpandableListAdapter {

        //  获得某个父项的某个子项
        @Override
        public Object getChild(int parentPos, int childPos) {
            return dataset.get(parentList[parentPos]).get(childPos);
        }

        //  获得父项的数量
        @Override
        public int getGroupCount() {
            return dataset.size();
        }

        //  获得某个父项的子项数目
        @Override
        public int getChildrenCount(int parentPos) {
            if(parentList[parentPos]==null)
                Log.i(TAG,String.valueOf(parentPos));
            return dataset.get(parentList[parentPos]).size();
        }

        //  获得某个父项
        @Override
        public Object getGroup(int parentPos) {
            return dataset.get(parentList[parentPos]);
        }

        //  获得某个父项的id
        @Override
        public long getGroupId(int parentPos) {
            return parentPos;
        }

        //  获得某个父项的某个子项的id
        @Override
        public long getChildId(int parentPos, int childPos) {
            return childPos;
        }

        //  按函数的名字来理解应该是是否具有稳定的id，这个方法目前一直都是返回false，没有去改动过
        @Override
        public boolean hasStableIds() {
            return false;
        }

        //  获得父项显示的view
        @Override
        public View getGroupView(int parentPos, boolean b, View view, ViewGroup viewGroup) {
            Log.i(TAG,"getGroupView被调用,parentPos="+parentPos);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) MainActivity
                        .this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.parent_item, null);
            }
            view.setTag(R.layout.parent_item, parentPos);
            view.setTag(R.layout.child_item, -1);
            TextView text = (TextView) view.findViewById(R.id.parent_title);

            //这句话老出问题 数组越界 但是不知道为什么会越界
            if(parentPos<parentList.length)
                text.setText(parentList[parentPos]);
            else{
                //????????????
                view.setEnabled(false);

            }
            return view;
        }


        //  获得子项显示的view
        @Override
        public View getChildView(final int parentPos, final int childPos, boolean b, View view, ViewGroup viewGroup) {
            Log.i(TAG,"getGroupView被调用,parentPos="+parentPos+",childPos="+childPos);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) MainActivity
                        .this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.child_item, null);
            }
            view.setTag(R.layout.parent_item, parentPos);
            view.setTag(R.layout.child_item, childPos);
            TextView text = (TextView) view.findViewById(R.id.child_title);
            text.setText(dataset.get(parentList[parentPos]).get(childPos));
            text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(MainActivity.this, "点到了parentPos="+parentPos+",childPos="+childPos, Toast.LENGTH_SHORT).show();
                }
            });
            return view;
        }

        //  子项是否可选中，如果需要设置子项的点击事件，需要返回true
        @Override
        public boolean isChildSelectable(int i, int i1) {
            return true;
        }

    }

}
