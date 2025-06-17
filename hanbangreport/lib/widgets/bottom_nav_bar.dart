import 'package:flutter/material.dart';

class CustomBottomNavBar extends StatelessWidget {
  final int selectedIndex;
  final Function(int) onItemTapped;

  const CustomBottomNavBar({
    super.key,
    required this.selectedIndex,
    required this.onItemTapped,
  });

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;

    double iconSizeSelected = (screenWidth * 0.17).clamp(50, 52);
    double iconSizeUnselected = (screenWidth * 0.07).clamp(35, 41);
    double labelFontSize = (screenWidth * 0.025).clamp(9, 12);

    return SizedBox(
      width: screenWidth,
      height: 61,
      child: Stack(
        children: [
          // 배경 이미지
          Positioned.fill(
            child: Image.asset(
              'assets/images/main_bottom_bar_bg.png',
              fit: BoxFit.fill,
              filterQuality: FilterQuality.high,
            ),
          ),
          // 아이콘 버튼들
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _buildNavItem(
                context,
                index: 0,
                iconPath: 'assets/images/icon_home.png',
                activeIconPath: 'assets/images/icon_home_active.png',
                label: '홈',
                isSelected: selectedIndex == 0,
                iconSizeSelected: iconSizeSelected,
                iconSizeUnselected: iconSizeUnselected,
                labelFontSize: labelFontSize,
              ),
              _buildNavItem(
                context,
                index: 1,
                iconPath: 'assets/images/icon_report.png',
                activeIconPath: 'assets/images/icon_report_active.png',
                label: '신고 목록',
                isSelected: selectedIndex == 1,
                iconSizeSelected: iconSizeSelected * 1, // 선택 시 크기 15% 줄임
                iconSizeUnselected: iconSizeUnselected * 0.70, // 비선택 크기 15% 줄임
                labelFontSize: labelFontSize,
              ),
              _buildNavItem(
                context,
                index: 2,
                iconPath: 'assets/images/icon_speech.png',
                activeIconPath: 'assets/images/icon_speech_active.png',
                label: '음성 세팅',
                isSelected: selectedIndex == 2,
                iconSizeSelected: iconSizeSelected,
                iconSizeUnselected: iconSizeUnselected,
                labelFontSize: labelFontSize,
              ),
              _buildNavItem(
                context,
                index: 3,
                iconPath: 'assets/images/icon_setting.png',
                activeIconPath: 'assets/images/icon_setting_active.png',
                label: '설정',
                isSelected: selectedIndex == 3,
                iconSizeSelected: iconSizeSelected,
                iconSizeUnselected: iconSizeUnselected,
                labelFontSize: labelFontSize,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildNavItem(
    BuildContext context, {
    required int index,
    required String iconPath,
    required String activeIconPath,
    required String label,
    required bool isSelected,
    required double iconSizeSelected,
    required double iconSizeUnselected,
    required double labelFontSize,
  }) {
    return GestureDetector(
      onTap: () => onItemTapped(index),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            height: 37, // NavItem의 고정 높이값
            alignment: Alignment.center,
            child: Image.asset(
              isSelected ? activeIconPath : iconPath,
              width: isSelected ? iconSizeSelected : iconSizeUnselected,
              height: isSelected ? iconSizeSelected : iconSizeUnselected,
              fit: BoxFit.contain,
              filterQuality: FilterQuality.high,
            ),
          ),
          const SizedBox(height: 1),
          if (!isSelected)
            Text(
              label,
              style: TextStyle(
                color: Colors.white70,
                fontSize: labelFontSize + 3,
                fontWeight: FontWeight.w500,
              ),
            ),
        ],
      ),
    );
  }
}
