package com.flowbytestudio.rencar.ui.theme

import androidx.compose.ui.unit.dp

// Tasarım token'ları: değerler mevcut tasarımın birebir aynısı — amaç görsel
// değişiklik değil, tek yerden yönetim. Ölçekte olmayan tekil değerler (2.dp,
// 9.dp gibi) bilinçli olarak token'a bağlanmaz.
object Dimens {

    // Boşluk ölçeği (padding, Arrangement.spacedBy, Spacer)
    val SpaceXxs = 4.dp
    val SpaceXs = 8.dp
    val SpaceS = 12.dp
    val SpaceM = 16.dp
    val SpaceL = 20.dp
    val SpaceXl = 24.dp
    val SpaceXxl = 32.dp

    // Köşe yarıçapları
    val CornerXs = 6.dp
    val CornerS = 8.dp
    val CornerM = 12.dp

    /** Kart ve giriş alanlarının varsayılan köşesi. */
    val CornerCard = 14.dp
    val CornerL = 16.dp

    /** Birincil butonların köşesi. */
    val CornerButton = 18.dp
    val CornerXl = 20.dp
    val CornerXxl = 24.dp

    // Bileşen ölçüleri
    val IconSizeS = 16.dp
    val IconSizeM = 20.dp
    val IconSizeL = 24.dp

    /** Birincil buton ve giriş alanı yüksekliği. */
    val ControlHeight = 56.dp
}
