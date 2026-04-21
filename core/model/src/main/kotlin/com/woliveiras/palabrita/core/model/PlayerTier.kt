package com.woliveiras.palabrita.core.model

enum class PlayerTier(val minXp: Int, val displayName: String) {
  NOVATO(0, "Novato"),
  CURIOSO(50, "Curioso"),
  ASTUTO(150, "Astuto"),
  SABIO(400, "Sábio"),
  EPICO(1000, "Épico"),
  LENDARIO(2500, "Lendário");

  companion object {
    fun fromXp(totalXp: Int): PlayerTier = entries.last { totalXp >= it.minXp }
  }
}
