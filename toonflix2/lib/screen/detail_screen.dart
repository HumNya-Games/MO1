import 'package:flutter/material.dart';

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
                  width: 270,

                  child: Image.network(
                    thumb,
                    headers: {
                      "User-Agent":
                          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36",
                    },
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
