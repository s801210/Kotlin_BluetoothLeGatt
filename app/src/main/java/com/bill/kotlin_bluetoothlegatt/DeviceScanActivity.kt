

package com.bill.kotlin_bluetoothlegatt

import android.Manifest
import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
class DeviceScanActivity : ListActivity() {
    private var mLeDeviceListAdapter: LeDeviceListAdapter? = null

    private var mScanning: Boolean = false
    private var mHandler: Handler? = null

    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar!!.setTitle(R.string.title_devices)
        mHandler = Handler()
        

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_COARSE_LOCATION)
            }
        }

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mLeDeviceListAdapter = LeDeviceListAdapter()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).isVisible = false
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        } else {
            menu.findItem(R.id.menu_stop).isVisible = true
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                mLeDeviceListAdapter!!.clear()
                scanLeDevice(true)
            }
            R.id.menu_stop -> scanLeDevice(false)
        }

        return true
    }


    //Permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(ContentValues.TAG, "coarse location permission granted")
                } else {

                }
                return
            }
        }
    }

    // 導入系統藍牙頁面後返回
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_ENABLE_BT ->{
                when (resultCode) {
                    AppCompatActivity.RESULT_OK ->{
                        mBluetoothLeScanner = mBluetoothManager!!.adapter.bluetoothLeScanner
                        scanLeDevice(true)
                    }
                    AppCompatActivity.RESULT_CANCELED ->{

                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



    override fun onResume() {
        super.onResume()

        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        listAdapter = mLeDeviceListAdapter

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }else{
            mBluetoothLeScanner = mBluetoothManager!!.adapter.bluetoothLeScanner
            scanLeDevice(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (mBluetoothAdapter!!.isEnabled) {
            scanLeDevice(false)
            mLeDeviceListAdapter!!.clear()
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val device = mLeDeviceListAdapter!!.getDevice(position) ?: return
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.address)
        if (mScanning) {
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
            mScanning = false
        }
        startActivity(intent)
    }

    private fun scanLeDevice(enable: Boolean) {


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { // 5.0以下
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                mHandler!!.postDelayed({
                    mScanning = false
                    mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                    invalidateOptionsMenu()
                }, SCAN_PERIOD)

                mScanning = true
                mBluetoothAdapter!!.startLeScan(mLeScanCallback)

            } else {
                mScanning = false
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
            }
        }else{ // 5.0以上
            when(enable){
                true -> {
                    mHandler!!.postDelayed({
                        mScanning = false
                        mBluetoothLeScanner!!.stopScan(mScanCallback)
                        invalidateOptionsMenu()
                    }, SCAN_PERIOD)

                    mScanning = true
                    mBluetoothLeScanner!!.startScan(mScanCallback)
                }
                false -> {
                    mScanning = false
                    mBluetoothLeScanner!!.stopScan(mScanCallback)
                }
            }
        }

        invalidateOptionsMenu()
    }

    // Android 5.0 以上
    private val mScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            val deviceName = result?.device?.name
            if (deviceName != null && deviceName.isNotEmpty()){
                //   if(deviceName.contains("ESP")) {
                Log.d(TAG,"onScanResult: ${result.scanRecord}")
                mLeDeviceListAdapter!!.addDevice(result.device!!)
                mLeDeviceListAdapter!!.notifyDataSetChanged()
                //  }

            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG,"onBatchScanResults:${results.toString()}")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanFailed: $errorCode")
        }
    }

    // Device scan callback. android 5.0以下
    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {

            //            System.out.println("##### " + device.address)
            val name = device.name
            if(name != null){
                mLeDeviceListAdapter!!.addDevice(device)
                mLeDeviceListAdapter!!.notifyDataSetChanged()
            }

        }
    }




    // Adapter for holding devices found through scanning.
    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice>
        private val mInflator: LayoutInflater

        init {
            mLeDevices = ArrayList<BluetoothDevice>()
            mInflator = this@DeviceScanActivity.layoutInflater
        }

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice? {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            var view = view
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view!!.findViewById(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById(R.id.device_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }

            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.length > 0)
                viewHolder.deviceName!!.text = deviceName
            else
                viewHolder.deviceName!!.setText(R.string.unknown_device)
            viewHolder.deviceAddress!!.text = device.address

            return view
        }
    }


    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }

    companion object {
        private val TAG = BluetoothLeService::class.java.simpleName

        const val REQUEST_ENABLE_BT = 1 // Ble開啟回傳
        const val PERMISSION_REQUEST_COARSE_LOCATION = 1 // 權限

        // Stops scanning after 10 seconds.
        private val SCAN_PERIOD: Long = 10000
    }
}