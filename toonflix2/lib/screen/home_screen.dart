import 'package:flutter/material.dart';
import 'package:toonflix2/models/webtoon.dart';
import 'package:toonflix2/services/api_service.dart';

class HomeScreen extends StatelessWidget {
  HomeScreen({super.key});

  final Future<List<WebtoonModel>> webtoons = ApiService.getTodaysToons();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        centerTitle: true,
        surfaceTintColor: Colors.white,
        shadowColor: Colors.black,
        elevation: 3,
        foregroundColor: Colors.green,
        backgroundColor: Colors.white,
        title: const Text(
          "Today's Toons",
          style: TextStyle(fontWeight: FontWeight.w600, fontSize: 27),
        ),
      ),
      body: FutureBuilder(
        future: webtoons,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            return Column(
              children: [
                SizedBox(height: 50),
                Expanded(child: makeList(snapshot)),
              ],
            );
          }
          return const Center(child: CircularProgressIndicator());
        },
      ),
    );
  }

  ListView makeList(AsyncSnapshot<List<WebtoonModel>> snapshot) {
    return ListView.separated(
      itemBuilder: (context, index) {
        print(index);
        var webtoon = snapshot.data![index];
        return Text(webtoon.title, style: TextStyle(fontSize: 17));
      },
      separatorBuilder: (context, index) {
        return SizedBox(width: 17);
      },
      scrollDirection: Axis.horizontal,
      itemCount: snapshot.data!.length,
    );
  }
}
