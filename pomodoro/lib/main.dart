import 'package:flutter/material.dart';
import 'package:pomodoro/Screen/home_screen.dart';

void main() {
  runApp(const App());
}

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(
        scaffoldBackgroundColor: const Color(0xFFE7626C),
        textTheme: const TextTheme(
          titleLarge: TextStyle(color: Colors.red),
          titleMedium: TextStyle(color: Color(0xFF232B55)),
          ),
        cardColor: Color(0xFFF4EDDB),
      ),
      home: HomeScreen(),
    );
  }
}
