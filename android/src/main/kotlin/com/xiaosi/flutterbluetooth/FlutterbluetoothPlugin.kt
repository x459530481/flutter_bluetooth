package com.xiaosi.flutterbluetooth

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
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.experimental.and


class FlutterbluetoothPlugin: MethodCallHandler {

  var mBluetoothAdapter:BluetoothAdapter? = null;
//  var mReaderHelper: ReaderHelper? = null
  var newserial: BluetoothSocket? = null
  var mInputStream: InputStream? = null
  var mOutputStream: OutputStream? = null

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
      //初始化
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (mBluetoothAdapter == null) {
        println("没有发现蓝牙模块,程序中止");
        result.error("no_bluetooth","no_bluetooth","no_bluetooth")
        mChannel!!.invokeMethod("no_bluetooth","no_bluetooth")
        return;
      }
      if (!mBluetoothAdapter!!.isEnabled) {
        println("蓝牙功能尚未打开,程序中止");
        result.error("no_enabled_bluetooth","no_enabled_bluetooth","no_enabled_bluetooth")
        mChannel!!.invokeMethod("no_enabled_bluetooth","no_enabled_bluetooth")
        return;
      }

      // Register for broadcasts when a device is discovered
      var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
      mActivity?.registerReceiver(mReceiver, filter)

      // Register for broadcasts when discovery has finished
      filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
      mActivity?.registerReceiver(mReceiver, filter)

      // Get the local Bluetooth adapter
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

      result.success("Success")
    } else if (call.method == "connting") {
      //连接蓝牙
      mBluetoothAdapter?.cancelDiscovery()
      var macAddress = ""
      if (call.hasArgument("macAddress")) {
        macAddress = call.argument<Any>("macAddress").toString()
      }
      if (macAddress.length < 17) return
      val address = macAddress.substring(macAddress.length - 17)
      // Get the device MAC address, which is the last 17 chars in the View

      val message = Message()
      message.what = 0
      message.obj = address
      handler.sendMessage(message)
      result.success("Success")
    } else if (call.method == "disconnect") {
      //断开蓝牙连接
      if(newserial != null && !newserial!!.isConnected){
        newserial!!.close()
      }
      mChannel!!.invokeMethod("disconnect_success","disconnect_success")
      result.success("Success")
    } else if (call.method == "discovery") {
      //搜索蓝牙
        doDiscovery()
    } else if (call.method == "cancelDiscovery") {
      //取消搜索蓝牙
      try{
        if(mBluetoothAdapter == null){
          println("mBluetoothAdapter == null");
        }
        // If we're already discovering, stop it
        if (mBluetoothAdapter!!.isDiscovering) {
          mBluetoothAdapter!!.cancelDiscovery()
        }
      }catch (e:Exception){
        println(e.toString())
        handler.sendEmptyMessage(-1)
      }
    } else if (call.method == "sendData") {
      //发送消息
      var data: ByteArray? = call.argument<ByteArray>("byteArray")
      sendData(data);
    }  else {
      result.notImplemented()
    }
  }

  fun sendData(data: ByteArray?) {
    if (newserial != null && newserial!!.isConnected()) { //判断连接是否有效
      try {
        val outputStream: OutputStream = newserial!!.getOutputStream()
        if (outputStream != null) {
          try {
            outputStream.write(data)
          } catch (e: Exception) {
            println( "outputStream.write null")
            handler.sendEmptyMessage(-1)
          }
        } else {
          println("outputStream is null")
          handler.sendEmptyMessage(-1)
        }
      } catch (e: IOException) {
        println("sendData: $e")
        handler.sendEmptyMessage(-1)
      }
    }else{
      handler.sendEmptyMessage(-1)
    }
  }
    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
      try{
        if(mBluetoothAdapter == null){
          println("mBluetoothAdapter == null");
        }
        // If we're already discovering, stop it
        if (mBluetoothAdapter!!.isDiscovering) {
          mBluetoothAdapter!!.cancelDiscovery()
        }
        mBluetoothAdapter!!.startDiscovery()
      }catch (e:Exception){
        println(e.toString())
        handler.sendEmptyMessage(-1)
      }
    }

  @SuppressLint("HandlerLeak")
  var handler: Handler = object : Handler() {
    override fun handleMessage(msg: Message) {
      super.handleMessage(msg)
      if (msg.what === 0) {
        val address:String = msg.obj as String
        Thread(Runnable {
          
          try {
            if(newserial != null && !newserial!!.isConnected){
              newserial!!.close()
            }
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
          
          val message = Message()
          message.what = 1
          message.obj = address
          this.sendMessage(message)
          
          try {
//            mReaderHelper = ReaderHelper.getDefaultHelper()
//            mReaderHelper!!.setReader(newserial!!.inputStream, newserial!!.outputStream)
//
////            val editor: SharedPreferences.Editor = IApplication.getInstance().getPreferences().edit()
////            editor.putString(
////                    SharepreferencesConstant.AppParam.BLUETOOTH_ADDRESS, address)
////            editor.commit()
////            this.sendEmptyMessage(1)
//              val message = Message()
//              message.what = 1
//              message.obj = address
//              this.sendMessage(message)

            mInputStream = newserial!!.inputStream

            mOutputStream = newserial!!.outputStream

            //启动新线程去处理
            while (true) {
              if (mInputStream != null) {
                try {
                  Thread.sleep(1000)
                  if (mInputStream!!.available() > 0) {
                    val buffer:ByteArray = ByteArray(mInputStream!!.available())
                    
                    mInputStream!!.read(buffer)
                    println(bytesToHexString(buffer))
////                    for (index in 0..buffer.size-1){
////                      println(bytesToHexString(buffer.get(index).to))
////                    }
//                    println("buffer="+buffer)
                    val utf8tzt = String(buffer, Charsets.UTF_8)
                    println("utf8tzt="+utf8tzt)
//
////                    println("hexStr2Str="+hexStr2Str(buffer.toUByteArray().))
////                    //                                    String isotzt = new String(buffer,"ISO-8859-1" );
//////                                    String gb2312tzt = new String(buffer,"GB2312" );
//////                                    String gbktzt = new String(buffer,"GBK" );
//////                                    String utf16tzt = new String(buffer,"UTF-16" );
////                    println(utf8tzt)
////                    //                                    System.out.println(isotzt);
//////                                    System.out.println(gb2312tzt);
//////                                    System.out.println(gbktzt);
//////                                    System.out.println(utf16tzt);
////                    val msg = Message()
////                    msg.what = 999
////                    msg.obj = utf8tzt
////                    sendMessage(msg)

                    val mMap = mapOf("\"origin_bytes\"" to '"' +buffer.toString() +'"'
                            , "\"bytes_to_hex\"" to '"' + bytesToHexString(buffer)!!.split(" ").toString() +'"'
                            , "\"bytes_to_utf8\"" to '"' + utf8tzt +'"')

                    val msg = Message()
                    msg.what = 999
                    msg.obj = mMap
                    sendMessage(msg)
                  }
                  
                } catch (e:Exception) {
                  println("mInputStream read is null")
                }
              }
            }
          } catch (e: Exception) {
            e.printStackTrace()
            try {
              newserial!!.close()
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
      } else if (msg.what === 999) {
        mChannel!!.invokeMethod("received",msg.obj)
      }
    }
  }


  //接收2：将byte【】转为16进制
  fun bytesToHexString(src: ByteArray?): String? {
    val sb = StringBuffer("")
    if (src == null || src.size <= 0) {
      return null
    }
    for (i in src.indices) {
      val v: Byte = src[i] and 0xFF.toByte()
      val hv = Integer.toHexString(v.toInt()).toUpperCase()
      if (hv.length < 2) {
        sb.append(0)
      }
      sb.append(hv)
      if (i != src.size - 1) {
        sb.append(" ")
      }
    }
    return sb.toString()
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

        // 扫描到的设备是否存在于队列 如不存在则添加到队列
          val mMap = mapOf("\"name\"" to '"' +device.name +'"', "\"address\"" to '"' +device.address +'"')
          mChannel!!.invokeMethod("found_result",mMap)

        // When discovery is finished, change the Activity title
      } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
        // 停止扫描蓝牙
          mChannel!!.invokeMethod("found_finish","found_finish")
      }
    }
  }

}

