package com.example.adr_nhom_da.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adr_nhom_da.data.model.CustomerTier
import com.example.adr_nhom_da.data.model.EmployeeTier
import com.example.adr_nhom_da.data.model.TableStatus
import com.example.adr_nhom_da.ui.theme.*

@Composable
fun CustomerTierBadge(tier: CustomerTier) {
    val (gradientColors, textColor, iconEmoji) = when (tier) {
        CustomerTier.DONG -> Triple(listOf(Color(0xFF8D6E63), Color(0xFF6D4C41)), Color.White, "🥉")
        CustomerTier.BAC -> Triple(listOf(Color(0xFF90A4AE), Color(0xFF607D8B)), Color.White, "🥈")
        CustomerTier.VANG -> Triple(listOf(Color(0xFFFFD54F), Color(0xFFFFB300)), Color(0xFF5D4037), "🥇")
        CustomerTier.BACH_KIM -> Triple(listOf(Color(0xFF4DD0E1), Color(0xFF00ACC1)), Color.White, "💎")
        CustomerTier.KIM_CUONG -> Triple(listOf(Color(0xFFBA68C8), Color(0xFF8E24AA)), Color.White, "👑")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Brush.horizontalGradient(gradientColors), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(text = iconEmoji, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${tier.displayName} (-${tier.discountPercent.toInt()}%)",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmployeeTierBadge(tier: EmployeeTier) {
    val (bgColor, textColor, label) = when (tier) {
        EmployeeTier.THU_VIEC -> Triple(Color(0xFFECEFF1), Color(0xFF455A64), "Thử việc")
        EmployeeTier.CHINH_THUC -> Triple(Color(0xFFFFEBEE), PrimaryRed, "Chính thức")
        EmployeeTier.QUAN_LY -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "Quản lý")
        EmployeeTier.GIAM_DOC -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Giám đốc")
    }

    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .border(0.8.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TableStatusBadge(status: TableStatus) {
    val (containerColor, borderColor, contentColor, statusDotColor, label) = when (status) {
        TableStatus.TRONG -> Tuple5(
            Color(0xFFE8F5E9),
            Color(0xFFA5D6A7),
            Color(0xFF2E7D32),
            Color(0xFF4CAF50),
            "Bàn Trống"
        )
        TableStatus.DANG_PHUC_VU -> Tuple5(
            Color(0xFFFBE9E7),
            Color(0xFFFFCCBC),
            Color(0xFFD84315),
            Color(0xFFFF5722),
            "Đang Phục Vụ"
        )
        TableStatus.DA_DAT -> Tuple5(
            Color(0xFFE3F2FD),
            Color(0xFF90CAF9),
            Color(0xFF1565C0),
            Color(0xFF2196F3),
            "Đã Đặt Trước"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(containerColor, shape = RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(statusDotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)
