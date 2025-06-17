import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:hanbangreport/widgets/bottom_nav_bar.dart';

class SettingScreen extends StatefulWidget {
  const SettingScreen({super.key});

  @override
  State<SettingScreen> createState() => _SettingScreenState();
}

class _SettingScreenState extends State<SettingScreen> {
  int _selectedIndex = 3; // Setting은 탭 3번

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
        Navigator.pushReplacementNamed(context, '/speech_setting');
        break;
      case 3:
        // 현재 화면
        break;
    }
  }

  Future<void> _resetStartDriveDialogPref() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('dont_show_start_drive_dialog');

    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('단속주행 안내 팝업이 초기화되었습니다.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: ElevatedButton(
          onPressed: _resetStartDriveDialogPref,
          child: const Text('단속주행 안내 팝업 초기화'),
        ),
      ),
      bottomNavigationBar: CustomBottomNavBar(
        selectedIndex: _selectedIndex,
        onItemTapped: _onItemTapped,
      ),
    );
  }
}
