import 'package:flutter/material.dart';
import 'package:toonflix2/screen/detail_screen.dart';

class Webtoon extends StatelessWidget {
  final String id, title, thumb;
  final bool isDetailScreen;

  const Webtoon({
    super.key,
    required this.id,
    required this.title,
    required this.thumb,
    this.isDetailScreen = false,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        if (isDetailScreen) {
          Navigator.pop(context); // detail이면 뒤로가기
        } else {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) {
                return DetailScreen(id: id, title: title, thumb: thumb);
              },
              fullscreenDialog: true,
            ),
          );
        }
      },
      child: Column(
        children: [
          if (!isDetailScreen)
            Hero(
              tag: id,
              child: Container(
                clipBehavior: Clip.hardEdge,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(27),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withAlpha(177),
                      blurRadius: 7,
                      blurStyle: BlurStyle.inner,
                      spreadRadius: 0.7,
                      offset: Offset(7, 5),
                    ),
                  ],
                ),
                width: 257,

                child: Image.network(
                  thumb,
                  headers: {
                    "User-Agent":
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36",
                  },
                ),
              ),
            ),
          if (isDetailScreen)
            Hero(
              tag: id,
              child: Container(
                clipBehavior: Clip.hardEdge,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(27),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withAlpha(177),
                      blurRadius: 7,
                      blurStyle: BlurStyle.inner,
                      spreadRadius: 0.7,
                      offset: Offset(7, 5),
                    ),
                  ],
                ),
                width: 370,

                child: Image.network(
                  thumb,
                  headers: {
                    "User-Agent":
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36",
                  },
                ),
              ),
            ),
          SizedBox(height: 7),
          if (!isDetailScreen)
            Text(
              title,
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
            ),
        ],
      ),
    );
  }
}
