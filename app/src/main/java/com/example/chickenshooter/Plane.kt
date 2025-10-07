package com.example.chickenshooter

data class Plane(
    val id: String,         // Thêm id để xác định máy bay (giúp mapping với prefs)
    val displayResId: Int,  // Ảnh góc chéo (menu)
    val gameResId: Int      // Ảnh góc thẳng (game)
)