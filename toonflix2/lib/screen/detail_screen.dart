import 'package:flutter/material.dart';
import 'package:toonflix2/widgets/webtoon_widget.dart'; // Webtoon 위젯 import

class DetailScreen extends StatelessWidget {
  final String id, title, thumb;

  const DetailScreen({
    super.key,
    required this.id,
    required this.title,
    required this.thumb,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        centerTitle: true,
        surfaceTintColor: Colors.white,
        shadowColor: Colors.black,
        elevation: 3,
        foregroundColor: Colors.black,
        backgroundColor: Colors.white,
        title: Text(
          title,
          style: TextStyle(fontWeight: FontWeight.w600, fontSize: 27),
        ),
      ),
      body: Column(
        children: [
          SizedBox(height: 77),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Webtoon(
                id: id,
                title: title,
                thumb: thumb,
                isDetailScreen: true, // 여기서 detail 여부 전달!
              ),
            ],
          ),
        ],
      ),
    );
  }
}
