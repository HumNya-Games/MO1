import 'dart:async';
import 'package:flutter/material.dart' show Alignment, BorderRadius, BoxDecoration, BuildContext, Column, Container, Expanded, Flexible, FontWeight, Icon, IconButton, Icons, MainAxisAlignment, Radius, Row, Scaffold, SizedBox, State, StatefulWidget, Text, TextStyle, Theme, Widget;

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int startSeconds = 1500;
  late int totalSeconds = startSeconds;
  int pomodoros = 0;
  late Timer timer;
  bool isRunning = false;

  String formatSeconds(int seconds) {
    var duration = Duration(seconds: seconds);
    return duration.toString().split(".").first.substring(2, 7);
  }

  void tikTock(Timer timer) {
    setState(() {
      if (totalSeconds == 0) {
        timer.cancel();
        totalSeconds = startSeconds;
        pomodoros = pomodoros + 1;
        isRunning = false;
      } else {
        totalSeconds = totalSeconds - 1;
      }
    });
  }

  void rePlay() {
    setState(() {
        //timer.cancel();
        totalSeconds = startSeconds;
        //isRunning = false;
    });
  }

  void stop() {
    setState(() {
        timer.cancel();
        totalSeconds = startSeconds;
        isRunning = false;
    });
  }

  void onStartCount() {
    timer = Timer.periodic(Duration(seconds: 1), tikTock);
    setState(() {
      isRunning = true;
    });
  }

  void countStop() {
    timer.cancel();
    setState(() {
      isRunning = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: Column(
        children: [
          Flexible(
            flex: 1,
            child: Container(
              alignment: Alignment.topCenter,
              child: Text(
                formatSeconds(totalSeconds),
                style: TextStyle(
                  color: Theme.of(context).cardColor,
                  fontSize: 89,
                ),
              ),
            ),
          ),
          Flexible(
            flex: 3,
            child: Column(
              children: [
                SizedBox(height: 147),
                IconButton(
                  onPressed: isRunning ? countStop : onStartCount,
                  icon: Icon(
                    isRunning
                        ? Icons.pause_circle_filled_sharp
                        : Icons.play_circle_fill_sharp,
                  ),
                  color: Theme.of(context).cardColor,
                  iconSize: 100,
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    IconButton(
                      onPressed: rePlay,
                      icon: Icon(
                        isRunning
                            ? Icons.replay_circle_filled_sharp
                            : null,
                      ),
                      color: Theme.of(context).cardColor,
                      iconSize: 37,
                    ),
                    SizedBox(width: 0),
                    IconButton(
                      onPressed: stop,
                      icon: Icon(
                        isRunning
                            ? Icons.stop_circle_sharp
                            : null,
                      ),
                      color: Theme.of(context).cardColor,
                      iconSize: 37,
                    ),
                  ],
                ),
              ],
            ),
          ),
          Flexible(
            flex: 1,
            child: Row(
              children: [
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(
                      color: Theme.of(context).cardColor,
                      borderRadius: BorderRadius.only(
                        topLeft: Radius.circular(27),
                        topRight: Radius.circular(27),
                      ),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Pomodoros',
                          style: TextStyle(
                            color: Theme.of(
                              context,
                            ).textTheme.titleMedium!.color,
                            fontSize: 20,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Text(
                          '$pomodoros',
                          style: TextStyle(
                            color: Theme.of(
                              context,
                            ).textTheme.titleMedium!.color,
                            fontSize: 57,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
