import 'package:flutter/material.dart';

class BottomNavBar extends StatelessWidget {
  final int selectedIndex;
  final Function(int) onItemTapped;

  const BottomNavBar({
    super.key,
    required this.selectedIndex,
    required this.onItemTapped,
  });

  @override
  Widget build(BuildContext context) {
    return BottomNavigationBar(
      backgroundColor: Colors.black,
      items: const [
        BottomNavigationBarItem(
          icon: ImageIcon(AssetImage('assets/images/icon_home.png')),
          activeIcon: ImageIcon(
            AssetImage('assets/images/icon_home_active.png'),
          ),
          label: '홈',
        ),
        BottomNavigationBarItem(
          icon: ImageIcon(AssetImage('assets/images/icon_report.png')),
          activeIcon: ImageIcon(
            AssetImage('assets/images/icon_report_active.png'),
          ),
          label: '리스트',
        ),
        BottomNavigationBarItem(
          icon: ImageIcon(AssetImage('assets/images/icon_speech.png')),
          activeIcon: ImageIcon(
            AssetImage('assets/images/icon_speech_active.png'),
          ),
          label: '음성',
        ),
        BottomNavigationBarItem(
          icon: ImageIcon(AssetImage('assets/images/icon_setting.png')),
          activeIcon: ImageIcon(
            AssetImage('assets/images/icon_setting_active.png'),
          ),
          label: '설정',
        ),
      ],
      currentIndex: selectedIndex,
      selectedItemColor: Colors.blue,
      unselectedItemColor: Colors.white,
      onTap: (index) => onItemTapped(index),
      type: BottomNavigationBarType.fixed,
    );
  }
}
