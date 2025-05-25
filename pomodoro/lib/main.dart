import 'package:flutter/material.dart';

void main() {
  runApp(const App());
}

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(
        textTheme: const TextTheme(titleLarge: TextStyle(color: Colors.red)),
        cardColor: Color(0xFFF4EDDB),
      ),
      home: Scaffold(backgroundColor: const Color(0xFFF4EDDB), body: Center()),
    );
  }
}
