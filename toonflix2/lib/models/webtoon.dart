class WebtoonModel {
  final String title, thumb, id;

  WebtoonModel.formJson(Map<String, dynamic> json)
    : title = json['title'],
      thumb = json['thumb'],
      id = json['id'];
}
