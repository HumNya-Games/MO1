import 'package:flutter/material.dart';
import 'package:hanbangreport/widgets/bottom_nav_bar.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:hanbangreport/widgets/start_drive_dialog.dart';
import 'package:flutter/services.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _selectedIndex = 0;

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });

    switch (index) {
      case 0:
        break;
      case 1:
        Navigator.pushReplacementNamed(context, '/report_list');
        break;
      case 2:
        Navigator.pushReplacementNamed(context, '/speech_setting');
        break;
      case 3:
        Navigator.pushReplacementNamed(context, '/setting');
        break;
    }
  }

  /// 운행 시작 버튼 클릭 처리
  void _onDriveStartButtonPressed() async {
    final prefs = await SharedPreferences.getInstance();
    final dontShow = prefs.getBool('dont_show_start_drive_dialog') ?? false;

    if (dontShow) {
      _startDrivingMode();
    } else {
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (_) => StartDriveDialog(onConfirmed: _startDrivingMode),
      );
    }
  }

  /// 운행 모드 시작 (플로팅볼 서비스 실행 & 앱 백그라운드 이동)
  void _startDrivingMode() {
    const platform = MethodChannel('floating_ball_service');

    platform.invokeMethod('startFloatingBall');

    // 앱을 백그라운드로 이동
    SystemNavigator.pop();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      bottomNavigationBar: CustomBottomNavBar(
        selectedIndex: _selectedIndex,
        onItemTapped: _onItemTapped,
      ),
      body: SafeArea(
        child: Column(
          children: [
            const Expanded(child: SizedBox()),

            // 운행 시작 버튼
            Center(
              child: GestureDetector(
                onTap: _onDriveStartButtonPressed,
                child: Image.asset(
                  'assets/images/driving_button.png',
                  width: MediaQuery.of(context).size.width * 0.5,
                  filterQuality: FilterQuality.high,
                ),
              ),
            ),

            const Expanded(child: SizedBox()),

            // 현재 신고 현황 + 리스트 버튼 영역
            Column(
              children: [
                Container(
                  margin: const EdgeInsets.only(bottom: 11),
                  decoration: BoxDecoration(
                    image: const DecorationImage(
                      image: AssetImage('assets/images/main_bar1.png'),
                      fit: BoxFit.contain,
                      isAntiAlias: true,
                      filterQuality: FilterQuality.high,
                    ),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  padding: const EdgeInsets.all(16),
                  width: MediaQuery.of(context).size.width * 0.9,
                  child: Column(
                    children: const [
                      Text(
                        '현재 신고 현황',
                        style: TextStyle(color: Colors.white, fontSize: 18),
                      ),
                      SizedBox(height: 8),
                      Text(
                        '총 9999건의 신고 처리 완료',
                        style: TextStyle(color: Colors.white, fontSize: 14),
                      ),
                    ],
                  ),
                ),

                _buildReportListButton(
                  context,
                  'assets/images/main_bar2.png',
                  'assets/images/icon_in_progress.png',
                  '접수 대기 중',
                  '9999건',
                  '/report_list',
                ),
                _buildReportListButton(
                  context,
                  'assets/images/main_bar2.png',
                  'assets/images/icon_completed.png',
                  '처리 완료',
                  '9999건',
                  '/report_list',
                ),
                _buildReportListButton(
                  context,
                  'assets/images/main_bar2.png',
                  'assets/images/icon_rejected.png',
                  '반려 신고',
                  '9999건',
                  '/report_list',
                ),

                const SizedBox(height: 20),

                const Text(
                  '자세한 사항을 확인하시려면 리스트를 선택하세요.',
                  style: TextStyle(color: Colors.white, fontSize: 13),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 16),
              ],
            ),
          ],
        ),
      ),
    );
  }

  static Widget _buildReportListButton(
    BuildContext context,
    String bgImage,
    String iconImage,
    String title,
    String count,
    String route,
  ) {
    return GestureDetector(
      onTap: () {
        Navigator.pushNamed(context, route);
      },
      child: Container(
        margin: const EdgeInsets.only(top: 7),
        decoration: BoxDecoration(
          image: DecorationImage(
            image: AssetImage(bgImage),
            fit: BoxFit.contain,
            filterQuality: FilterQuality.high,
          ),
          borderRadius: BorderRadius.circular(10),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 0),
        width: MediaQuery.of(context).size.width * 0.9,
        child: Row(
          children: [
            Transform.translate(
              offset: const Offset(1, 7),
              child: Image.asset(
                iconImage,
                width: 51,
                height: 51,
                fit: BoxFit.contain,
                filterQuality: FilterQuality.high,
              ),
            ),
            const SizedBox(width: 11),
            Expanded(
              child: Text(
                title,
                style: const TextStyle(color: Colors.white, fontSize: 16),
              ),
            ),
            Transform.translate(
              offset: const Offset(-7, 0),
              child: Text(
                count,
                style: const TextStyle(color: Colors.white, fontSize: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
