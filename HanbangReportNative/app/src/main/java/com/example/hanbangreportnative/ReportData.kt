package com.example.hanbangreportnative

data class ReportData(
    val id: String, // 예: 20240705-00001
    var type: String, // "신고 대기" 또는 "신고 완료"
    var selected: Boolean, // 리스트에서 선택 여부
    val violationType: String, // "교통 위반" 또는 "신호 위반"
    val datetime: String, // 예: "2024.07.05 12:12:59"
    val location: String, // 예: "서울시 서대문구 홍제천로 4길 14"
    val thumbnail: String // 예: "EX_art1"
) 