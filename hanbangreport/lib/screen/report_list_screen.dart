import 'package:flutter/material.dart';
import 'package:hanbangreport/widgets/bottom_nav_bar.dart';

class ReportListScreen extends StatefulWidget {
  const ReportListScreen({super.key});

  @override
  State<ReportListScreen> createState() => _ReportListScreenState();
}

class _ReportListScreenState extends State<ReportListScreen> {
  int _selectedIndex = 1; // ReportList는 탭 1번

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });

    switch (index) {
      case 0:
        Navigator.pushReplacementNamed(context, '/main');
        break;
      case 1:
        // 현재 화면
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
      body: const Center(child: Text('Report List Screen')),
      bottomNavigationBar: CustomBottomNavBar(
        selectedIndex: _selectedIndex,
        onItemTapped: _onItemTapped,
      ),
    );
  }
}
