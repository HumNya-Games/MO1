import 'package:flutter/material.dart';
import 'package:toonflix/widgets/button.dart';
import 'package:toonflix/widgets/currency_card.dart';

void main() {
  runApp(const App());
}

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        backgroundColor: const Color(0xff181818),
        body: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsetsGeometry.symmetric(horizontal: 15),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    const SizedBox(height: 80),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        const Text(
                          "안녕! 흠냐!!",
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          "귀환을 환영합니다",
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.8),
                            fontWeight: FontWeight.w500,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 120),
                Text(
                  "Tatal Balance",
                  style: TextStyle(
                    fontSize: 22,
                    color: Colors.white.withValues(alpha: 0.8),
                  ),
                ),
                const SizedBox(height: 2),
                const Text(
                  "\$17,200,000",
                  style: TextStyle(
                    fontSize: 42,
                    fontWeight: FontWeight.w900,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 13),
                const Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Button(
                      text: 'Transfer',
                      bgColor: Color(0xFFF1B33B),
                      textColor: Colors.black,
                    ),
                    Button(
                      text: 'Request',
                      bgColor: Color(0xFF1F2123),
                      textColor: Colors.white,
                    ),
                  ],
                ),
                const SizedBox(height: 80),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Wallets',
                      style: TextStyle(
                        fontSize: 40,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      'View all',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w400,
                        color: Colors.white.withValues(alpha: 0.7),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                Transform.translate(
                  offset: const Offset(0, -0),
                  child: const CurrenceyCard(
                    name: 'Euro',
                    code: 'euo',
                    amount: '6 482',
                    icon: Icons.euro_sharp,
                    isInverted: true,
                  ),
                ),
                Transform.translate(
                  offset: const Offset(0, -15),
                  child: const CurrenceyCard(
                    name: 'Doller',
                    code: 'dol',
                    amount: '8 482',
                    icon: Icons.money_off_sharp,
                    isInverted: false,
                  ),
                ),
                Transform.translate(
                  offset: const Offset(0, -30),
                  child: const CurrenceyCard(
                    name: 'BitCoin',
                    code: 'bit',
                    amount: '99 482',
                    icon: Icons.currency_bitcoin_sharp,
                    isInverted: true,
                  ),
                ),
                Transform.translate(
                  offset: const Offset(0, -45),
                  child: const CurrenceyCard(
                    name: 'Won',
                    code: 'won',
                    amount: '999 482',
                    icon: Icons.money_sharp,
                    isInverted: false,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
