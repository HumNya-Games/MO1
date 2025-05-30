import 'dart:typed_data';

class WebtoonModel {
  final String title, thumb, id;
  Uint8List? imageBytes;

  WebtoonModel({
    required this.title,
    required this.thumb,
    required this.id,
    this.imageBytes,
  });

  WebtoonModel.fromJson(Map<String, dynamic> json)
    : title = json['title'] ?? "",
      thumb = json['thumb'] ?? "",
      id = json['id'] ?? "";
}
