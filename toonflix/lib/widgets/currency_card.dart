import 'package:flutter/material.dart';

class CurrenceyCard extends StatelessWidget {
  final String name, code, amount;
  final IconData icon;
  final bool isInverted;
  final _blackColor = const Color(0xFF1F2123);
  final int number;

  const CurrenceyCard({
    super.key,
    required this.name,
    required this.code,
    required this.amount,
    required this.icon,
    required this.isInverted,
    required this.number,
  });

  @override
  Widget build(BuildContext context) {
    return Transform.translate(
      offset: Offset(0, -number * 20),
      child: Container(
        clipBehavior: Clip.hardEdge,
        decoration: BoxDecoration(
          color: isInverted ? _blackColor : Colors.white,
          borderRadius: const BorderRadiusGeometry.all(Radius.circular(20)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(17.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: TextStyle(
                      color: isInverted ? Colors.white : _blackColor,
                      fontSize: 25,
                    ),
                  ),
                  const SizedBox(height: 1),
                  Row(
                    children: [
                      Text(
                        amount,
                        style: TextStyle(
                          color: isInverted
                              ? Colors.white.withValues(alpha: 0.8)
                              : _blackColor,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(width: 5),
                      Text(
                        code,
                        style: TextStyle(
                          color: isInverted
                              ? Colors.white.withValues(alpha: 0.8)
                              : _blackColor,
                          fontSize: 15,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              Transform.scale(
                scale: 2.1,
                child: Transform.translate(
                  offset: const Offset(-7, 7),
                  child: Icon(
                    icon,
                    color: isInverted
                        ? Colors.white.withValues(alpha: 0.8)
                        : _blackColor,
                    size: 75,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
