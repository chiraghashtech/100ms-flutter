package live.hms.hmssdk_flutter

import android.app.Activity
import androidx.annotation.NonNull
import io.flutter.Log

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.hms.hmssdk_flutter.views.HMSVideoView
import live.hms.hmssdk_flutter.views.HMSVideoViewFactory
import live.hms.hmssdk_flutter.views.HMSVideoViewWidget
import live.hms.video.connection.HMSDataChannel
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSTrack
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.HMSUpdateListener
import live.hms.video.sdk.models.*
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRoomUpdate
import live.hms.video.sdk.models.enums.HMSTrackUpdate
import kotlin.isInitialized

/** HmssdkFlutterPlugin */
class HmssdkFlutterPlugin: FlutterPlugin, MethodCallHandler, HMSUpdateListener,ActivityAware,EventChannel.StreamHandler {
  private lateinit var channel : MethodChannel
  private lateinit var meetingEventChannel: EventChannel
  private var eventSink: EventChannel.EventSink? = null
  private lateinit var activity: Activity
  lateinit var hmssdk: HMSSDK
  private lateinit var hmsVideoFactory:HMSVideoViewFactory


  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    this.channel = MethodChannel(flutterPluginBinding.binaryMessenger, "hmssdk_flutter")
    this.meetingEventChannel= EventChannel(flutterPluginBinding.binaryMessenger,"meeting_event_channel")
    this.hmsVideoFactory= HMSVideoViewFactory(this)
    flutterPluginBinding.platformViewRegistry.registerViewFactory("HMSVideoView",hmsVideoFactory)

    this.meetingEventChannel.setStreamHandler(this)
    this.channel.setMethodCallHandler(this)


  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    Log.i("onMethodCall","reached")
    when(call.method){
      "getPlatformVersion"->{
        result.success("Android ${android.os.Build.VERSION.RELEASE}")
      }
      "join_meeting"->{
        joinMeeting(call)
        result.success("joining meeting in android")
      }
      "leave_meeting"->{
        leaveMeeting()
        result.success("Leaving meeting")
      }
      "switch_audio"->{
        switchAudio(call,result)
      }
      "switch_video"->{
        switchVideo(call,result)
      }
      "switch_camera"->{
        switchCamera()
        result.success("switch_camera")
      }
      "is_video_mute"->{
        result.success(isVideoMute(call))
      }
      "is_audio_mute"->{
        result.success(isAudioMute(call))
      }
      "stop_capturing"->{
        stopCapturing()
        result.success("stop_capturing")
      }
      "start_capturing"->{
        startCapturing()
        result.success("start_capturing")
      }
      "send_message"->{
        sendMessage(call)
      }
      else->{
        result.notImplemented()
      }
    }


  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    meetingEventChannel.setStreamHandler(null)
  }



  override fun onError(error: HMSException) {

    val args=HashMap<String,Any>()
    args.put("event_name","on_error")
    args.put("data", HMSExceptionExtension.toDictionary(error)!!)
    Log.i("onError",args.get("data").toString())
    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }
  }

  override fun onJoin(room: HMSRoom) {
    //Log.i("OnJoin",room.toString()+"HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHAAA")
    val args=HashMap<String,Any>()
    args.put("event_name","on_join_room")
    args.put("data", HMSRoomExtension.toDictionary(room)!!)
    Log.i("onJoin",args.get("data").toString())
    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }

  }

  override fun onMessageReceived(message: HMSMessage) {

    val args=HashMap<String,Any>()
    args.put("event_name","on_message")
    args.put("data",HMSMessageExtension.toDictionary(message))
    Log.i("onMessageReceived",args.get("data").toString())
    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }
  }

  override fun onPeerUpdate(type: HMSPeerUpdate, peer: HMSPeer) {
    peer.auxiliaryTracks
    val args=HashMap<String,Any>()
    args.put("event_name","on_peer_update")
    Log.i("onPeerUpdate",type.toString())
    args.put("data",HMSPeerUpdateExtension.toDictionary(peer,type))
    Log.i("onPeerUpdate",args.get("data").toString())
//    val hmsVideoViewWidget =HMSVideoView(hmsVideoFactory.context)
//    if(peer.videoTrack!=null){
//      peer!!.videoTrack!!.addSink(hmsVideoViewWidget.surfaceViewRenderer)
//    }
    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }
  }

  override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
    val args=HashMap<String,Any>()
    args.put("event_name","on_update_room")
    args.put("data",hmsRoom.name)

    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }
  }

  override fun onTrackUpdate(type: HMSTrackUpdate, track: HMSTrack, peer: HMSPeer) {

    val args=HashMap<String,Any>()
    args.put("event_name","on_track_update")


    args.put("data",HMSTrackUpdateExtension.toDictionary(peer,track,type))
    Log.i("onTrackUpdate",peer.toString())


 //   val hmsVideoViewWidget=hmsVideoFactory.hmsVideoViewWidget

//    if(peer.videoTrack!=null && hmsVideoViewWidget!=null){
//
//      Log.i("onTrackUpdate",hmsVideoFactory.hmsVideoViewWidget!!.hmsVideoView.surfaceViewRenderer.toString())
//      val renderer=hmsVideoFactory.hmsVideoViewWidget!!.hmsVideoView.surfaceViewRenderer
//      peer.videoTrack!!.addSink(renderer)
//      Log.i("onTrackUpdate","Track Found")
//    }
//    else{
//      Log.i("onTrackUpdate","Track Not Found")
//    }
    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }


  }

  override fun onReconnected() {
    super.onReconnected()
    val args=HashMap<String,Any>()
    args.put("event_name","on_re_connected")
    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }
  }

  override fun onReconnecting(error: HMSException) {
    super.onReconnecting(error)

    val args=HashMap<String,Any>()
    args.put("event_name","on_re_connecting")
    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }
  }

  override fun onRoleChangeRequest(request: HMSRoleChangeRequest) {
    val args=HashMap<String,Any>()
    args.put("event_name","on_role_change_request")
    args.put("data",HMSRoleChangedExtension.toDictionary(request))
    CoroutineScope(Dispatchers.Main).launch {
      eventSink!!.success(args)
    }
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    this.activity=binding.activity
    this.hmssdk=HMSSDK.Builder(this.activity).build()
  }

  override fun onDetachedFromActivityForConfigChanges() {

  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    this.activity=binding.activity
  }

  override fun onDetachedFromActivity() {

  }

  fun joinMeeting(@NonNull call: MethodCall){
    val userName=call.argument<String>("user_name")
    val authToken= call.argument<String>("auth_token")
    val shouldSkipPiiEvents=call.argument<Boolean>("should_skip_pii_events")
    Log.i("userName",authToken!!)
    val hmsConfig= HMSConfig(userName = userName!!,authtoken = authToken!!)
    hmssdk.join(hmsConfig,this)
    meetingEventChannel.setStreamHandler(this)

  }

  fun leaveMeeting(){
    if (hmssdk!=null)
      hmssdk.leave()
    else
      Log.e("error","not initialized")
  }

  fun switchAudio(call: MethodCall,result: Result){
    val argsIsOn=call.argument<Boolean>("is_on")
    val peer=hmssdk.getLocalPeer()
    val audioTrack=peer?.audioTrack
    audioTrack!!.setMute(argsIsOn!!)
    result.success("audio_changed")
  }

  fun switchVideo(call: MethodCall,result: Result){
    val argsIsOn=call.argument<Boolean>("is_on")
    val peer=hmssdk.getLocalPeer()
    val videoTrack=peer?.videoTrack
    videoTrack!!.setMute(argsIsOn!!)
    result.success("video_changed")
  }

  fun stopCapturing(){
    val peer=hmssdk.getLocalPeer()
    val videoTrack=peer?.videoTrack
    videoTrack!!.setMute(true)
  }

  fun startCapturing(){
    val peer=hmssdk.getLocalPeer()
    val videoTrack=peer?.videoTrack
    videoTrack!!.setMute(false)

  }

  fun switchCamera(){
    val peer=hmssdk.getLocalPeer()
    val videoTrack=peer?.videoTrack
    CoroutineScope(Dispatchers.Default).launch{
      videoTrack!!.switchCamera()
    }
  }

  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    this.eventSink=events
  }

  override fun onCancel(arguments: Any?) {
    this.eventSink=null
  }

  fun getPeerById(id:String ,isLocal:Boolean):HMSPeer?{
    if(isLocal){
      val peer=hmssdk.getLocalPeer()
      return peer
    }
    else{
      val peers=hmssdk.getRemotePeers()
      peers.forEach {
        if(it.peerID==id) return it
      }
    }
    return  null
  }

  fun isVideoMute(call: MethodCall):Boolean{
    val peerId=call.argument<String>("peer_id")
    val isLocal=call.argument<Boolean>("is_local")
    val peer= getPeerById(peerId!!,isLocal!!) ?: return false
    return peer!!.videoTrack!!.isMute
  }

  fun isAudioMute(call: MethodCall):Boolean{
    val peerId=call.argument<String>("peer_id")
    val isLocal=call.argument<Boolean>("is_local")
    val peer= getPeerById(peerId!!,isLocal!!) ?: return false
    return peer!!.audioTrack!!.isMute
  }

  fun sendMessage(call: MethodCall){
    val message=call.argument<String>("message")
    hmssdk!!.sendMessage("chat",message!!)
  }
}