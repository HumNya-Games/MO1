import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class StartDriveDialog extends StatefulWidget {
  final VoidCallback onConfirmed;

  const StartDriveDialog({super.key, required this.onConfirmed});

  @override
  State<StartDriveDialog> createState() => _StartDriveDialogState();
}

class _StartDriveDialogState extends State<StartDriveDialog> {
  bool _dontShowAgain = false;

  static const _prefKey = 'dont_show_start_drive_dialog';

  @override
  void initState() {
    super.initState();
  }

  Future<void> _savePref() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_prefKey, _dontShowAgain);
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () async => false, // 뒤로가기 방지
      child: Stack(
        children: [
          // 뒤 화면을 어둡게 가리기
          ModalBarrier(color: Colors.black, dismissible: false),

          // 다이얼로그와 바깥 텍스트, 버튼 배치
          Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 7),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // 팝업 바깥 위쪽 텍스트 '단속 주행 안내'
                  const Text(
                    '단속 주행 안내',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 22,
                      fontWeight: FontWeight.w900,
                      decoration: TextDecoration.none,
                    ),
                  ),

                  const SizedBox(height: 17),

                  // 다이얼로그 박스 → Dialog 제거하고 Container + BoxConstraints 적용
                  Container(
                    constraints: const BoxConstraints(
                      minWidth: 270,
                      maxWidth: 400,
                    ),
                    padding: const EdgeInsets.fromLTRB(27, 24, 27, 16),
                    decoration: BoxDecoration(
                      color: const Color(0xFF514C4C),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Text(
                          '확인 버튼을 누르면 단속 주행이 시작됩니다.\n앱은 플로팅 위젯으로 화면에 출력됩니다.',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 17,
                            decoration: TextDecoration.none,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          '실행 중 “신고”라고 말하시면 즉시 해당\n 위반 내역이 저장 또는 신고 처리됩니다.',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 17,
                            decoration: TextDecoration.none,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 20),

                        // 다시 보지 않기 버튼
                        GestureDetector(
                          onTap: () {
                            setState(() {
                              _dontShowAgain = !_dontShowAgain;
                            });
                          },
                          child: Container(
                            decoration: BoxDecoration(
                              color: _dontShowAgain
                                  ? Colors.green
                                  : Colors.black.withAlpha(80),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 8,
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Container(
                                  width: 20,
                                  height: 20,
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    border: Border.all(
                                      color: Colors.white,
                                      width: 2,
                                    ),
                                    color: _dontShowAgain
                                        ? Colors.white
                                        : Colors.transparent,
                                  ),
                                  child: _dontShowAgain
                                      ? const Icon(
                                          Icons.check,
                                          size: 16,
                                          color: Colors.green,
                                        )
                                      : null,
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  '다음부터 안내문 보지 않기',
                                  style: TextStyle(
                                    fontWeight: _dontShowAgain
                                        ? FontWeight.w900
                                        : FontWeight.w500,
                                    color: _dontShowAgain
                                        ? Colors.black
                                        : Colors.white,
                                    fontSize: 14,
                                    decoration: TextDecoration.none,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),

                  // 팝업 바깥 아래쪽 확인 버튼
                  ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF1849D6),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 24,
                        vertical: 12,
                      ),
                      minimumSize: Size.zero,
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                    onPressed: () async {
                      await _savePref();
                      widget.onConfirmed();
                      if (mounted) {
                        Navigator.of(context).pop();
                      }
                    },
                    child: const Text(
                      '확인',
                      style: TextStyle(
                        fontSize: 17,
                        color: Colors.white,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
