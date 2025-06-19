import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hanbangreport/screen/logo_screen.dart';
import 'package:hanbangreport/screen/title_screen.dart';
import 'package:hanbangreport/screen/main_screen.dart';
import 'package:hanbangreport/screen/setting_screen.dart';
import 'package:hanbangreport/screen/speech_setting_screen.dart';
import 'package:hanbangreport/screen/report_list_screen.dart';

// navigatorKey 선언 (전역)
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  print('Flutter 앱 시작');

  _setupMethodChannel(); // MethodChannel 세팅 먼저
  runApp(const HanBangReportApp()); // 앱 실행
}

void _setupMethodChannel() {
  const platform = MethodChannel('com.hanbangreport/floating_ball');

  platform.setMethodCallHandler((call) async {
    if (call.method == 'navigateToRoute') {
      final targetRoute = call.arguments as String;
      print('Native 호출: $targetRoute 로 이동');

      try {
        if (navigatorKey.currentState != null) {
          navigatorKey.currentState!.pushNamedAndRemoveUntil(
            targetRoute,
            (route) => false,
          );
        } else {
          print('navigatorKey.currentState가 null입니다.');
        }
      } catch (e) {
        print('네비게이션 중 오류 발생: $e');
      }
    }
  });
}

class HanBangReportApp extends StatelessWidget {
  const HanBangReportApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '한방 신고',
      navigatorKey: navigatorKey, // 전역 navigatorKey 등록
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
