import 'package:flutter/material.dart';
import 'package:hanbangreport/widgets/bottom_nav_bar.dart';

class SpeechSettingScreen extends StatefulWidget {
  const SpeechSettingScreen({super.key});

  @override
  State<SpeechSettingScreen> createState() => _SpeechSettingScreenState();
}

class _SpeechSettingScreenState extends State<SpeechSettingScreen> {
  int _selectedIndex = 2; // SpeechSetting은 탭 2번

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });

    switch (index) {
      case 0:
        Navigator.pushReplacementNamed(context, '/main');
        break;
      case 1:
        Navigator.pushReplacementNamed(context, '/report_list');
        break;
      case 2:
        // 현재 화면
        break;
      case 3:
        Navigator.pushReplacementNamed(context, '/setting');
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: const Center(child: Text('Speech Setting Screen')),
      bottomNavigationBar: CustomBottomNavBar(
        selectedIndex: _selectedIndex,
        onItemTapped: _onItemTapped,
      ),
    );
  }
}
