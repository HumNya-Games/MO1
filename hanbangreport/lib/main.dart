import 'package:flutter/material.dart';
import 'package:hanbangreport/screen/logo_screen.dart';
import 'package:hanbangreport/screen/title_screen.dart';
import 'package:hanbangreport/screen/main_screen.dart';
import 'package:hanbangreport/screen/setting_screen.dart';
import 'package:hanbangreport/screen/speech_setting_screen.dart';
import 'package:hanbangreport/screen/report_list_screen.dart';

void main() {
  print('Flutter 앱 시작');
  runApp(const HanBangReportApp());
}

class HanBangReportApp extends StatelessWidget {
  const HanBangReportApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '한방 신고',
      theme: ThemeData(primarySwatch: Colors.blue),
      initialRoute: '/logo',
      routes: {
        '/logo': (context) => const LogoScreen(),
        '/title': (context) => const TitleScreen(),
        '/main': (context) => const MainScreen(),
        '/setting': (context) => const SettingScreen(),
        '/speech_setting': (context) => const SpeechSettingScreen(),
        '/report_list': (context) => const ReportListScreen(),
      },
    );
  }
}
