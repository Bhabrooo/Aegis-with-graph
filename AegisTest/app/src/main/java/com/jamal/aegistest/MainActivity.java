package com.jamal.aegistest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


// line chart wali classes hain ye
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    public double value_bluetooth_double[];
    public float value_bluetooth_float[];
    //variables to store the received BLE data
    public Vector<Double> ecg_raw= new Vector<Double>(0);
    public Vector<Double> acc_x_raw= new Vector<Double>(0);
    public Vector<Double> acc_y_raw= new Vector<Double>(0);
    public Vector<Double> acc_z_raw= new Vector<Double>(0);

    //new Chart Variables
    LineData line_chart_data;
    LineDataSet line_chart_data_set;
    LineChart line_chart;
    ArrayList<String> labels = new ArrayList<String>();
    public Vector<Float> chart_vec = new Vector<Float>(0);
    public Vector<Float> chart_buffer = new Vector<Float>(0);

    private int state;

    private boolean scanStarted;
    private boolean scanning;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;

    private Button enableBluetoothButton;
    private Button disableBluetoothButton;
    private TextView scanStatusText;
    private Button scanButton;
    private TextView deviceInfoText;
    private TextView connectionStatusText;
    private TextView Data_received;

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            scanStarted &= scanning;
            updateUi();
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                    upgradeState(STATE_CONNECTING);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };

    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                downgradeState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Data_received = (TextView) findViewById(R.id.data_received);

        //Line Chart Stuff
        line_chart = (LineChart) findViewById(R.id.line_chart);
        line_chart.setDescription("");
        YAxis leftAxis = line_chart.getAxisLeft();
        YAxis rightAxis = line_chart.getAxisRight();
        leftAxis.setAxisMaxValue(10f);
        leftAxis.setAxisMinValue(-10f);
        rightAxis.setAxisMaxValue(10f);
        rightAxis.setAxisMinValue(-10f);
        line_chart.setData(SeedData());
        line_chart.animateX(2500);
        line_chart.setDrawGridBackground(false);
        line_chart.invalidate();


        connectionStatusText = (TextView) findViewById(R.id.connectionStatus);
        scanStatusText = (TextView) findViewById(R.id.scanStatus);
        deviceInfoText = (TextView) findViewById(R.id.deviceInfo);

        // Enable Bluetooth
        enableBluetoothButton = (Button) findViewById(R.id.enableBluetooth);
        enableBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBluetoothButton.setEnabled(false);
                enableBluetoothButton.setText(
                        bluetoothAdapter.enable() ? "Enabling bluetooth..." : "Enable failed!");
            }
        });

        // Enable Bluetooth
        disableBluetoothButton = (Button) findViewById(R.id.disableBluetooth);
        disableBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBluetoothButton.setEnabled(false);
                disableBluetoothButton.setText(
                        bluetoothAdapter.disable() ? "Disabling bluetooth..." : "Disable failed!");
                if(!bluetoothAdapter.isEnabled()){
                    enableBluetoothButton.setEnabled(true);
                }
            }
        });

        // Find Device and connect to it
        scanStatusText = (TextView) findViewById(R.id.scanStatus);
        scanButton = (Button) findViewById(R.id.scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanStarted = true;
                scanStatusText.setText("Scan Started");
                scanButton.setText("Scanning...");
                bluetoothAdapter.startLeScan( new UUID[]{ RFduinoService.UUID_SERVICE }, MainActivity.this);
            }
        });

        // Send
        /*valueEdit = (EditData) findViewById(R.id.value);
        valueEdit.setImeOptions(EditorInfo.IME_ACTION_SEND);
        valueEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendValueButton.callOnClick();
                    return true;
                }
                return false;
            }
        });

        sendZeroButton = (Button) findViewById(R.id.sendZero);
        sendZeroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rfduinoService.send(new byte[]{0});
            }
        });

        sendValueButton = (Button) findViewById(R.id.sendValue);
        sendValueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rfduinoService.send(valueEdit.getData());
            }
        });*/
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
    }

    @Override
    protected void onStop() {
        super.onStop();

        bluetoothAdapter.stopLeScan(this);

        unregisterReceiver(scanModeReceiver);
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(rfduinoReceiver);
    }

    private void upgradeState(int newState) {
        if (newState > state) {
            updateState(newState);
        }
    }

    private void downgradeState(int newState) {
        if (newState < state) {
            updateState(newState);
        }
    }

    private void updateState(int newState) {
        state = newState;
        updateUi();
    }

    private void updateUi() {
        // Enable Bluetooth
        boolean on = state > STATE_BLUETOOTH_OFF;
        enableBluetoothButton.setEnabled(!on);
        disableBluetoothButton.setEnabled(on);
        enableBluetoothButton.setText(on ? "Bluetooth enabled" : "Enable Bluetooth");
        disableBluetoothButton.setText(!on ? "Bluetooth disabled" : "Disable Bluetooth");
        scanButton.setEnabled(on);

        // Scan
        if (scanStarted && scanning) {
            //scanStatusText.setText("Scanning...");
            //scanButton.setText("Stop Scan");
            scanButton.setEnabled(true);
        } else if (scanStarted) {
            //scanStatusText.setText("Scan started...");
            scanButton.setEnabled(false);
        } else {
            //scanStatusText.setText("");
            //scanButton.setText("Scan");
            scanButton.setEnabled(true);
        }

        // Connect
        boolean connected = false;
        String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
            connected = true;
            connectionText = "Connected";
        }
        connectionStatusText.setText(connectionText);

        // Send
    }

    private void addData(byte[] data) {

        value_bluetooth_double = HexAsciiHelper.bytestodouble(data);
        Data_received.setText("Data Received: " + String.valueOf(value_bluetooth_double[0]));

        ecg_raw.addElement(value_bluetooth_double[0]);
        acc_x_raw.addElement(value_bluetooth_double[1]);
        acc_y_raw.addElement(value_bluetooth_double[2]);
        //acc_z_raw.addElement(value_bluetooth_double[3]);

        value_bluetooth_float = HexAsciiHelper.bytestofloat(data);
        chart_vec.addElement(value_bluetooth_float[0]);



        if(chart_vec.size()==200){
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateLineChart(chart_vec);
                    chart_vec.clear();
                }
            });
        }

        /*View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, dataLayout, false);

        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        text1.setText(HexAsciiHelper.bytesToHex(data));

        String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
        if (ascii != null) {
            TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            text2.setText(ascii);
        }

        dataLayout.addView(
                view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);*/
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;
        scanning=true;

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceInfoText.setText(BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord));
                updateUi();
            }
        });
        if( !(bluetoothDevice.getName().equals("AEGIS"))){
            bluetoothAdapter.startLeScan(new UUID[]{RFduinoService.UUID_SERVICE}, MainActivity.this);
        }
        else{
            Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
            bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
        }
    }

    //Chart wala stuff
    protected LineData SeedData() {

        ArrayList<Entry> entries = new ArrayList<>();

        int k=0;
        for(int i=0;i<200 ;i++){
            chart_buffer.addElement((float)k);
            entries.add(new Entry(chart_buffer.elementAt(i), i));
            labels.add(String.valueOf(i));
            k++;
            if(k==10){
                k=0;
            }
        }
        line_chart_data_set = new LineDataSet(entries, "ECG Reading");

        line_chart_data_set.setLineWidth(2.5f);
        line_chart_data_set.setDrawCircles(false);
        line_chart_data_set.setColor(ColorTemplate.VORDIPLOM_COLORS[2]);
        line_chart_data = new LineData(labels, line_chart_data_set);
        return line_chart_data;
    }

    protected void updateLineChart(Vector<Float> data) {
        line_chart_data_set.clear();
        if(data.size() == 200 ){
            for(int i=0; i<200;i++){
                chart_buffer.remove(0);
                chart_buffer.addElement(data.elementAt(i));
            }
            for(int i=0; i<200;i++){
                line_chart_data_set.addEntry(new Entry(chart_buffer.elementAt(i),i));
            }
        }
        int random_colour = (int) (Math.random()*5);
        line_chart_data_set.setColor(ColorTemplate.VORDIPLOM_COLORS[random_colour]);
        line_chart.notifyDataSetChanged();  // notification of update
        line_chart.invalidate(); // refresh graph
    }
}