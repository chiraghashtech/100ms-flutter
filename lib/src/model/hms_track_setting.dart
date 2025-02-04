import 'package:hmssdk_flutter/hmssdk_flutter.dart';

class HMSTrackSetting {
  final HMSAudioTrackSetting? audioTrackSetting;
  final HMSVideoTrackSetting? videoTrackSetting;

  HMSTrackSetting({this.audioTrackSetting, this.videoTrackSetting});

  factory HMSTrackSetting.fromMap(Map<String, dynamic> map) {
    HMSAudioTrackSetting? audioTrackSetting;
    HMSVideoTrackSetting? videoTrackSetting;
    if (map.containsKey('audio_track_setting')) {
      audioTrackSetting =
          HMSAudioTrackSetting.fromMap(map['audio_track_setting']);
    }
    if (map.containsKey('video_track_setting')) {
      videoTrackSetting =
          HMSVideoTrackSetting.fromMap(map['video_track_setting']);
    }
    return HMSTrackSetting(
      audioTrackSetting: audioTrackSetting,
      videoTrackSetting: videoTrackSetting,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'audioTrackSetting': audioTrackSetting?.toMap() ?? {},
      'videoTrackSetting': this.videoTrackSetting,
    };
  }
}
