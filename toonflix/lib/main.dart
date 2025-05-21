import 'package:flutter/material.dart';

void main() {
  runApp(App());
}

class App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text('이제 시작이다 열심히하자!!')),
        body: Center(child: Text('오호 이건가??')),
      ),
    );
  }
}
