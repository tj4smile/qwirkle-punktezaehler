package com.thomas.qwirkle.ui.theme

import androidx.compose.ui.graphics.Color
import com.thomas.qwirkle.util.PlayerColors

// Markenfarben (Hell)
val QwirkleBlue = Color(0xFF1E5AA8)
val QwirkleOrange = Color(0xFFE67E22)
val QwirkleGreen = Color(0xFF2E8B57)

// Markenfarben (Dunkel)
val QwirkleBlueLight = Color(0xFF9CC3FF)
val QwirkleOrangeLight = Color(0xFFFFB877)
val QwirkleGreenLight = Color(0xFF8ED8AE)

// Plakette "Gespielte Partien" (Grün, Text mit gutem Kontrast)
val GamesChipContainerLight = Color(0xFFC8E6C9)
val GamesChipContentLight = Color(0xFF1B5E20)
val GamesChipContainerDark = Color(0xFF1F4A2A)
val GamesChipContentDark = Color(0xFFB9F6CA)

/** Spielerfarbe aus dem gespeicherten colorIndex (siehe PlayerColors). */
fun playerColor(colorIndex: Int): Color = Color(PlayerColors.argb(colorIndex))
