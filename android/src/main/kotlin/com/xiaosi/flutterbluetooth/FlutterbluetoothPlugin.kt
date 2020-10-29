package com.xiaosi.flutterbluetooth

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Message
import com.xiaosi.flutterbluetooth.reader.helper.ReaderHelper
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.IOException
import java.util.*


class FlutterbluetoothPlugin: MethodCallHandler {

  var mBluetoothAdapter:BluetoothAdapter? = null;
  var mBtAdapter: BluetoothAdapter? = null
  var mReaderHelper: ReaderHelper? = null

    val myuuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
  companion object {
    var mContext: Context? = null
    var mActivity: Activity? = null
      var mChannel:MethodChannel? = null
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      mContext = registrar.activity().applicationContext
      mActivity = registrar.activity()
        mChannel = MethodChannel(registrar.messenger(), "flutterbluetooth")
        mChannel!!.setMethodCallHandler(FlutterbluetoothPlugin())
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "init") {
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter() as Nothing?;
      if (mBluetoothAdapter == null) {
//        showTextToast("没有发现蓝牙模块,程序中止");
//        finish();
        result.error("no_bluetooth","no_bluetooth","no_bluetooth")
        return;
      }

      // Register for broadcasts when a device is discovered
      var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
      mActivity?.registerReceiver(mReceiver, filter)

      // Register for broadcasts when discovery has finished
      filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
      mActivity?.registerReceiver(mReceiver, filter)

      // Get the local Bluetooth adapter
      mBtAdapter = BluetoothAdapter.getDefaultAdapter();

      result.success("Success")
    } else if (call.method == "connting") {
      mBtAdapter?.cancelDiscovery()
      var macAddress = ""
      if (call.hasArgument("macAddress")) {
        macAddress = call.argument<Any>("macAddress").toString()
      }
      if (macAddress.length < 17) return
      val address = macAddress.substring(macAddress.length - 17)
      // Get the device MAC address, which is the last 17 chars in the View

////            showTextToast("正在尝试连接...");
//      //            showTextToast("正在尝试连接...");
//      showLodingDialog(getmContext(), getString(R.string.dialog_title_tips),
//              "正在尝试连接...", false)
      val message = Message()
      message.what = 0
      message.obj = address
      handler.sendMessage(message)
      result.success("Success")
    } else if (call.method == "discovery") {
        doDiscovery()
    } else if (call.method == "cancelDiscovery") {
        mBtAdapter?.cancelDiscovery()
    }  else {
      result.notImplemented()
    }
  }


    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
//        if (D) Log.d(TAG, "doDiscovery()")
//
//        // Indicate scanning in the title
//        setProgressBarIndeterminateVisibility(true)
//        setTitle(R.string.scanning)
//
//        // Turn on sub-title for new devices
//        findViewById(R.id.title_paired_devices).setVisibility(View.GONE)
//        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE)

        // If we're already discovering, stop it
        if (mBtAdapter!!.isDiscovering) {
            mBtAdapter!!.cancelDiscovery()
        }
//        mNewDevicesArrayAdapter.clear()
//        mNewDevicesArrayAdapter.notifyDataSetChanged()
        // Request discover from BluetoothAdapter
        mBtAdapter!!.startDiscovery()
    }

  @SuppressLint("HandlerLeak")
  var handler: Handler = object : Handler() {
    override fun handleMessage(msg: Message) {
      super.handleMessage(msg)
      if (msg.what === 0) {
        val address:String = msg.obj as String
        Thread(Runnable {
          var newserial: BluetoothSocket? = null
          try {
            val device: BluetoothDevice? = mBluetoothAdapter?.getRemoteDevice(address)
            newserial = device?.createRfcommSocketToServiceRecord(myuuid)
            newserial?.connect()
          } catch (e: Exception) {
            try {
              newserial!!.close()
            } catch (e1: IOException) {
              e1.printStackTrace()
            }
            this.sendEmptyMessage(-1)
            return@Runnable
          }
          if (!newserial!!.isConnected) {
            this.sendEmptyMessage(-1)
            return@Runnable
          }
          try {
            mReaderHelper = ReaderHelper.getDefaultHelper()
            mReaderHelper!!.setReader(newserial.inputStream, newserial.outputStream)

//            val editor: SharedPreferences.Editor = IApplication.getInstance().getPreferences().edit()
//            editor.putString(
//                    SharepreferencesConstant.AppParam.BLUETOOTH_ADDRESS, address)
//            editor.commit()
//            this.sendEmptyMessage(1)
              val message = Message()
              message.what = 1
              message.obj = address
              this.sendMessage(message)
          } catch (e: Exception) {
            e.printStackTrace()
            try {
              newserial.close()
            } catch (e1: IOException) {
              e1.printStackTrace()
            }
            this.sendEmptyMessage(-1)
            return@Runnable
          }
        }).start()
      } else if (msg.what === 1) {
          mChannel!!.invokeMethod("connection_successful",msg.obj)
      } else if (msg.what === -1) {
          mChannel!!.invokeMethod("connection_failed","connection_failed")
      }
    }
  }


  // The BroadcastReceiver that listens for discovered devices and
  // changes the title when discovery is finished
  private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      // When discovery finds a device
      if (BluetoothDevice.ACTION_FOUND == action) {
        // Get the BluetoothDevice object from the Intent
        val device: BluetoothDevice = intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (device.name == null) return
//      if(mNewDevicesArrayAdapter.getPosition(device.getName() + "\n" + device.getAddress()) == -1){
//        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//      }
        // 扫描到的设备是否存在于队列 如不存在则添加到队列
          val mMap = mapOf("name" to device.name, "address" to device.address)
          mChannel!!.invokeMethod("found_result",mMap)

        // When discovery is finished, change the Activity title
      } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
        // 停止扫描蓝牙
          mChannel!!.invokeMethod("found_finish","found_finish")
      }
    }
  }

}

