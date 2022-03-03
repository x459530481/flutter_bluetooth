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
  var address:String = ""
  var connectState = -1;

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
      // Register for broadcasts when a device is discovered
      var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

      // Register for broadcasts when discovery has finished
      filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

      // Register for broadcasts when discovery has finished
      filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)

      filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

      filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);//指明一个与远程设备建立的低级别（ACL）连接。
      filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);//指明一个来自于远程设备的低级别（ACL）连接的断开
      filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);//指明一个为远程设备提出的低级别（ACL）的断开连接请求，并即将断开连接。

      mActivity?.registerReceiver(mReceiver, filter)

      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (mBluetoothAdapter == null) {
        println("没有发现蓝牙模块,程序中止");
//        result.error("no_bluetooth","no_bluetooth","no_bluetooth")
        mChannel!!.invokeMethod("no_bluetooth","no_bluetooth")
        return;
      }
      if (!mBluetoothAdapter!!.isEnabled) {
        println("蓝牙功能尚未打开,程序中止");
//        result.error("no_enabled_bluetooth","no_enabled_bluetooth","no_enabled_bluetooth")
        mChannel!!.invokeMethod("no_enabled_bluetooth","no_enabled_bluetooth")
        return;
      }

      // Get the local Bluetooth adapter
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      mChannel!!.invokeMethod("init_success","init_success")
      result.success("Success")
    } else if (call.method == "connting") {
      //连接蓝牙
      mBluetoothAdapter?.cancelDiscovery()
      var macAddress = ""
      if (call.hasArgument("macAddress")) {
        macAddress = call.argument<Any>("macAddress").toString()
      }
      if (macAddress.length < 17) return
      address = macAddress.substring(macAddress.length - 17)
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
    }  else if (call.method == "hex2utf8") {
      //转换文字
      var hexStr: String? = call.argument<String>("hexStr")
      var utf8Str = hexStringToString(hexStr)
      if(utf8Str != null){
        result.success(utf8Str)
        val message = Message()
        message.what = 1000
        message.obj = utf8Str
        handler.sendMessage(message)
      }else{
//        result.error("","","")
        handler.sendEmptyMessage(-1000)
      }
    } else if (call.method == "checkConnected") {
      try{
        if (newserial == null || !newserial!!.isConnected) {
          handler.sendEmptyMessage(-1)
        }
      }catch (e:Exception){
        handler.sendEmptyMessage(-1)
      }
      result.success("Success")
    }else {
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
      mChannel!!.invokeMethod("start_discovery","start_discovery")
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
            if(newserial != null && newserial!!.isConnected){
              newserial!!.close()
            }
            val device: BluetoothDevice? = mBluetoothAdapter?.getRemoteDevice(address)
            newserial = device?.createRfcommSocketToServiceRecord(myuuid)
            newserial?.connect()
          } catch (e: Exception) {
            try {
              if(newserial != null && newserial!!.isConnected){
                newserial!!.close()
              }
            } catch (e1: Exception) {
              e1.printStackTrace()
            }
            this.sendEmptyMessage(-1)
            return@Runnable
          }
          if (!newserial!!.isConnected) {
            this.sendEmptyMessage(-1)
            return@Runnable
          }

//          val message = Message()
//          message.what = 1
//          message.obj = address
//          this.sendMessage(message)

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

                    var buffferStrList = buffer.joinToString(separator = ",")
                    println("buffferStrList="+buffferStrList)

                    var bytesToHexStr = bytesToHexString(buffer)
                    println("bytesToHexStr="+bytesToHexStr)

                    var utf8tzt = String(buffer, Charsets.UTF_8).toString().trim()

                    utf8tzt = utf8tzt.replace("\\p{C}","")
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

//                    val mMap = mapOf("\"origin_bytes\"" to '"' +buffferStrList+'"'
//                            , "\"bytes_to_hex\"" to '"' + bytesToHexStr!! +'"'
//                            , "\"bytes_to_utf8\"" to '"' + utf8tzt +'"')

                    val mMap = mapOf("\"origin_bytes\"" to '"' +buffferStrList+'"'
                      , "\"bytes_to_hex\"" to '"' + bytesToHexStr!! +'"')

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
      } else if (msg.what === -11) {
        mChannel!!.invokeMethod("connection_failed_11","connection_failed_11")
//      } else if (msg.what === 11) {
//        mChannel!!.invokeMethod("connection_successful_11","connection_successful_11")
//      } else if (msg.what === -12) {
//        mChannel!!.invokeMethod("connecting","connecting")
      }  else if (msg.what === 999) {
        mChannel!!.invokeMethod("received",msg.obj)
      } else if (msg.what === 1000) {
        mChannel!!.invokeMethod("hex2utf8_successful",msg.obj)
      } else if (msg.what === -1000) {
        mChannel!!.invokeMethod("hex2utf8_error","hex2utf8_error")
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

  /**
   * 16进制字符串转换为字符串
   *
   * @param s
   * @return
   */
  fun hexStringToString(s: String?): String? {
    var s = s
    if (s == null || s == "") {
      return null
    }
    s = s.replace("{","")
    s = s.replace("}","")
    s = s.replace("[","")
    s = s.replace("]","")
    s = s.replace("(","")
    s = s.replace(")","")
    s = s.replace(",","")
    s = s.replace(" ", "")
    val baKeyword = ByteArray(s.length / 2)
    for (i in baKeyword.indices) {
      try {
        baKeyword[i] = (0xff and
                s.substring(i * 2, i * 2 + 2).toInt(16)).toByte()
      } catch (e: java.lang.Exception) {
        e.printStackTrace()
      }
    }
    try {
      s = String(baKeyword, Charsets.UTF_8)
//      s = String(baKeyword, StandardCharsets.UTF_8)
      String()
    } catch (e1: java.lang.Exception) {
      e1.printStackTrace()
    }
    return s
  }

  // The BroadcastReceiver that listens for discovered devices and
  // changes the title when discovery is finished
  private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      mChannel!!.invokeMethod("broadcast_receiver_action",action)
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
      } else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
        // 状态改变的广播
//        intent.getParcelableExtra<>()
        val device: BluetoothDevice = intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (device.address == null) return
        if (device.address.equals(address,true)) {
          connectState = device.getBondState();
          when (connectState) {
            BluetoothDevice.BOND_NONE -> handler.sendEmptyMessage(-11)

            BluetoothDevice.BOND_BONDING -> handler.sendEmptyMessage(-12)

            BluetoothDevice.BOND_BONDED -> handler.sendEmptyMessage(11)
          }
        }
      } else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
//        // 配对状态的广播
////        intent.getParcelableExtra<>()
        val device: BluetoothDevice = intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (device.address == null) return
        if (device.address.equals(address,true)) {
          connectState = device.getBondState();
          when (connectState) {
            BluetoothDevice.BOND_NONE -> {

              mChannel!!.invokeMethod("action_bond_state_changed","bond_none")
            }//删除配对

            BluetoothDevice.BOND_BONDING -> {

              mChannel!!.invokeMethod("action_bond_state_changed","bond_bonding")
            }//正在配对

            BluetoothDevice.BOND_BONDED -> {

              mChannel!!.invokeMethod("action_bond_state_changed","bond_bonded")
            }//配对成功
          }
        }
      }else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
          BluetoothAdapter.ERROR)
        // 蓝牙设备状态的广播
        when (state) {
          BluetoothAdapter.STATE_OFF -> {
            //手机蓝牙关闭
//            val message = Message()
//            message.what = 1000
//            message.obj = "BluetoothAdapter.STATE_OFF"
//            handler.sendMessage(message)
            mChannel!!.invokeMethod("no_enabled_bluetooth","no_enabled_bluetooth")
          }
//          BluetoothAdapter.STATE_TURNING_OFF -> {
//            //手机蓝牙正在关闭
//            val message = Message()
//            message.what = 1000
//            message.obj = "BluetoothAdapter.STATE_TURNING_OFF"
//            handler.sendMessage(message)
//          }
          BluetoothAdapter.STATE_ON -> {
            //手机蓝牙开启
            mChannel!!.invokeMethod("enabled_bluetooth","enabled_bluetooth")
          }
//          BluetoothAdapter.STATE_TURNING_ON -> {
//            //手机蓝牙正在开启
//            val message = Message()
//            message.what = 1000
//            message.obj = "BluetoothAdapter.STATE_TURNING_ON"
//            handler.sendMessage(message)
//          }
        }
      }else if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
        //指明一个与远程设备建立的低级别（ACL）连接。
        val message = Message()
        message.what = 1
        message.obj = address
        handler.sendMessage(message)
      }else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
        //指明一个来自于远程设备的低级别（ACL）连接的断开
        handler.sendEmptyMessage(-11)
      }
//      else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)){
//        //指明一个为远程设备提出的低级别（ACL）的断开连接请求，并即将断开连接。
//        handler.sendEmptyMessage(-12)
//      }

    }
  }

}

