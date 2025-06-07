import 'package:flutter/material.dart';
import 'package:toonflix2/models/webtoon.dart';
import 'package:toonflix2/models/webtoon_detail_model.dart';
import 'package:toonflix2/models/webtoon_episode_model.dart';
import 'package:toonflix2/services/api_service.dart';
import 'package:toonflix2/widgets/webtoon_widget.dart'; // Webtoon 위젯 import

class DetailScreen extends StatefulWidget {
  final String id, title, thumb;

  const DetailScreen({
    super.key,
    required this.id,
    required this.title,
    required this.thumb,
  });

  @override
  State<DetailScreen> createState() => _DetailScreenState();
}

class _DetailScreenState extends State<DetailScreen> {
  late Future<WebtoonDetailModel> webtoon;
  late Future<List<WebtoonEpisodeModel>> episodes;

  @override
  void initState() {
    super.initState();
    webtoon = ApiService.getToonById(widget.id);
    episodes = ApiService.getLatesEpisodesById(widget.id);
  }

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
          widget.title,
          style: TextStyle(fontWeight: FontWeight.w600, fontSize: 27),
        ),
      ),
      body: Column(
        children: [
          SizedBox(height: 27),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Webtoon(
                id: widget.id,
                title: widget.title,
                thumb: widget.thumb,
                isDetailScreen: true, // 여기서 detail 여부 전달!
              ),
            ],
          ),
          const SizedBox(height: 17),
          FutureBuilder(
            future: webtoon,
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        snapshot.data!.about,
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w300,
                        ),
                      ),
                      const SizedBox(height: 17),
                      Text(
                        '${snapshot.data!.genre} / '
                        '${snapshot.data!.age}',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                );
              }
              return Text("...");
            },
          ),
        ],
      ),
    );
  }
}
