package live.hms.hmssdk_flutter.views

import android.content.Context
import android.util.Log
import android.view.View
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformViewFactory
import live.hms.hmssdk_flutter.HmssdkFlutterPlugin
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.video.sdk.models.HMSPeer
import org.webrtc.SurfaceViewRenderer

class HMSVideoViewWidget(context: Context, id: Int, creationParams: Map<String?, Any?>?,val peer:HMSPeer?,val trackId:String,val  isAux:Boolean) : PlatformView {

    private val hmsVideoView: HMSVideoView by lazy {
        HMSVideoView(context)
    }

    init {
    }

    override fun onFlutterViewAttached(flutterView: View) {
        super.onFlutterViewAttached(flutterView)
        renderVideo()
    }

    override fun onFlutterViewDetached() {
        super.onFlutterViewDetached()
    }

    private fun renderVideo() {

        if (peer == null) return

        val tracks = peer.auxiliaryTracks
        if (tracks.isNotEmpty() && isAux){
            val track = tracks.first {
                it.trackId == trackId
            }

            if (track != null) {
                hmsVideoView.setVideoTrack((track as HMSVideoTrack))
                return
            }
        }
        else {
            peer.videoTrack.let {
                if (it?.trackId == trackId || peer.isLocal) {
                    hmsVideoView.setVideoTrack(it)
                }
            }
        }
    }

    override fun getView(): View {
        return hmsVideoView
    }

    override fun dispose() {
        release()
    }

    private fun release() {
        peer?.videoTrack.let {
            if (it?.trackId == trackId || peer?.isLocal == true) {
                it?.removeSink(hmsVideoView.surfaceViewRenderer)
            }
        }
        hmsVideoView.surfaceViewRenderer.release()
    }
}

class HMSVideoViewFactory(val plugin: HmssdkFlutterPlugin) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {

        val creationParams = args as Map<String?, Any?>?
        val id=args!!["peer_id"] as? String
        val isLocal=args!!["is_local"] as? Boolean
        val trackId=args!!["track_id"] as? String
        val isAuxiliary = args!!["is_aux"] as? Boolean
        val peer = if(isLocal==null || isLocal!!) plugin.getLocalPeer()
        else plugin.getPeerById(id!!)!!
        return HMSVideoViewWidget(context, viewId, creationParams,peer,trackId!!,isAuxiliary!!)
    }
}
