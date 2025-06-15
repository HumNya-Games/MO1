import 'package:flutter/material.dart';
import 'package:hanbangreport/widgets/bottom_nav_bar.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  // 현재 선택된 인덱스 (홈 화면은 0)
  int _selectedIndex = 0;

  // 탭 아이템 클릭 시 호출 함수
  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });

    switch (index) {
      case 0:
        // 현재 화면이므로 이동 없음
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      bottomNavigationBar: BottomNavBar(
        selectedIndex: _selectedIndex,
        onItemTapped: _onItemTapped,
      ),
      body: SafeArea(
        child: Column(
          children: [
            // 상단 영역 (Spacer)
            const Expanded(child: SizedBox()),

            // 운행 시작 버튼
            Center(
              child: GestureDetector(
                onTap: () {
                  // 운행 시작 기능
                },
                child: Image.asset(
                  'assets/images/driving_button.png',
                  width: MediaQuery.of(context).size.width * 0.5,
                ),
              ),
            ),

            // 운행 버튼과 리스트 사이 Spacer
            const Expanded(child: SizedBox()),

            // 신고 현황 안내 바 + 리스트 버튼 + 안내 텍스트
            Column(
              children: [
                // 신고 현황 안내 바
                Container(
                  margin: const EdgeInsets.only(bottom: 11),
                  decoration: BoxDecoration(
                    image: const DecorationImage(
                      image: AssetImage('assets/images/main_bar1.png'),
                      fit: BoxFit.cover,
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

                // 신고 리스트 버튼 3개
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

  // 신고 리스트 버튼 위젯
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
        margin: const EdgeInsets.only(top: 11),
        decoration: BoxDecoration(
          image: DecorationImage(image: AssetImage(bgImage), fit: BoxFit.cover),
          borderRadius: BorderRadius.circular(10),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
        width: MediaQuery.of(context).size.width * 0.9,
        child: Row(
          children: [
            Image.asset(iconImage, width: 30, height: 30),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                title,
                style: const TextStyle(color: Colors.white, fontSize: 16),
              ),
            ),
            Text(
              count,
              style: const TextStyle(color: Colors.white, fontSize: 16),
            ),
          ],
        ),
      ),
    );
  }
}
