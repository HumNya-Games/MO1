import 'package:flutter/material.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
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
                '25:00',
                style: TextStyle(
                  color: Theme.of(context).cardColor,
                  fontSize: 89,
                ),
              ),
            ),
          ),
          Flexible(
            flex: 3,
            child: Center(
              child: IconButton(
                onPressed: () {},
                icon: Icon(Icons.play_circle_fill_sharp),
                color: Theme.of(context).cardColor,
                iconSize: 100,
              ),
            ),
          ),
          Flexible(flex: 1, 
          child: Row(
            children: [
              Expanded(
                child: Container(
                  decoration: BoxDecoration(
                    color: Theme.of(context).cardColor
                    ),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('Pomodoros', style: TextStyle(
                        color: Theme.of(context).textTheme.titleMedium!.color,
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text('0', style: TextStyle(
                        color: Theme.of(context).textTheme.titleMedium!.color,
                        fontSize: 57,
                        fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          )),
        ],
      ),
    );
  }
}
