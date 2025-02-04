import 'dart:async';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:hmssdk_flutter_example/common/constant.dart';
import 'package:hmssdk_flutter_example/common/ui/organisms/user_name_dialog_organism.dart';
import 'package:hmssdk_flutter_example/enum/meeting_flow.dart';
import 'package:hmssdk_flutter_example/preview/preview_page.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:wakelock/wakelock.dart';
import 'package:input_history_text_field/input_history_text_field.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  FirebaseCrashlytics.instance.setCrashlyticsCollectionEnabled(true);
  Wakelock.enable();
  runZonedGuarded(
      () => runApp(HMSExampleApp()), FirebaseCrashlytics.instance.recordError);
}

class HMSExampleApp extends StatelessWidget {
  const HMSExampleApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      scaffoldMessengerKey: GlobalKey<ScaffoldMessengerState>(),
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  TextEditingController roomIdController =
      TextEditingController(text: Constant.defaultRoomID);

  void getPermissions() async {
    await Permission.camera.request();
    await Permission.microphone.request();

    while ((await Permission.camera.isDenied)) {
      await Permission.camera.request();
    }
    while ((await Permission.microphone.isDenied)) {
      await Permission.microphone.request();
    }
  }

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    getPermissions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('100ms'),
        actions: [
          IconButton(onPressed: () {}, icon: Icon(CupertinoIcons.settings))
        ],
      ),
      body: Center(
        child: Container(
          padding: EdgeInsets.all(8),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Join a Meeting',
                    style: TextStyle(
                        height: 1, fontSize: 24, fontWeight: FontWeight.bold)),
                SizedBox(
                  height: 8,
                ),
                InputHistoryTextField(
                  historyKey: "key-01",
                  textEditingController: roomIdController,
                  enableOpacityGradient: true,
                  autofocus: true,
                  decoration: InputDecoration(
                      hintText: 'Enter Room URL',
                      border: OutlineInputBorder(
                          borderRadius: BorderRadius.all(Radius.circular(16)))),
                ),
                SizedBox(
                  height: 8,
                ),
                ElevatedButton(
                    style: ButtonStyle(
                        shape:
                            MaterialStateProperty.all<RoundedRectangleBorder>(
                                RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16.0),
                    ))),
                    onPressed: () async {
                      String user = await showDialog(
                          context: context,
                          builder: (_) => UserNameDialogOrganism());
                      if (user.isNotEmpty)
                        Navigator.of(context).push(MaterialPageRoute(
                            builder: (_) => PreviewPage(
                                  roomId: roomIdController.text,
                                  user: user,
                                  flow: MeetingFlow.join,
                                )));
                    },
                    child: Container(
                      padding: const EdgeInsets.all(4.0),
                      decoration: BoxDecoration(
                          borderRadius: BorderRadius.all(Radius.circular(16))),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.video_call_outlined, size: 48),
                          SizedBox(
                            width: 8,
                          ),
                          Text('Join Meeting',
                              style: TextStyle(height: 1, fontSize: 24))
                        ],
                      ),
                    ))
              ],
            ),
          ),
        ),
      ),
    );
  }
}
