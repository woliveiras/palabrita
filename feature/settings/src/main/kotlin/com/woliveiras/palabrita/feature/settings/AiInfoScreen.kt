package com.woliveiras.palabrita.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.PalabritaColors
import com.woliveiras.palabrita.core.common.R as CommonR

@Composable
fun AiInfoScreen(
  onBack: () -> Unit,
  onNavigateToSettings: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  viewModel: AiInfoViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
  ) {
    Spacer(Modifier.height(16.dp))

    // Back button
    IconButton(onClick = onBack) {
      Icon(
        Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = stringResource(CommonR.string.back),
      )
    }

    Spacer(Modifier.height(16.dp))

    // Header with icon
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(56.dp)
            .background(
              Brush.linearGradient(
                listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)
              ),
              RoundedCornerShape(16.dp),
            ),
      ) {
        Icon(
          Icons.Rounded.SmartToy,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(32.dp),
        )
      }
      Spacer(Modifier.width(16.dp))
      Column {
        Text(
          text = stringResource(CommonR.string.ai_info_title),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(CommonR.string.ai_info_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Spacer(Modifier.height(24.dp))

    // What It Does card
    WhatItDoesCard()

    Spacer(Modifier.height(16.dp))

    // Current Model card
    CurrentModelCard(state)

    Spacer(Modifier.height(16.dp))

    // Privacy & Security card
    HighlightCard(
      icon = Icons.Rounded.Shield,
      iconColor = Color(0xFF16A34A),
      borderColor = Color(0xFF16A34A).copy(alpha = 0.3f),
      title = stringResource(CommonR.string.ai_info_privacy_title),
      body = stringResource(CommonR.string.ai_info_privacy_body),
    )

    Spacer(Modifier.height(16.dp))

    // Battery Optimization card
    HighlightCard(
      icon = Icons.Rounded.BatteryChargingFull,
      iconColor = Color(0xFF16A34A),
      borderColor = Color(0xFF16A34A).copy(alpha = 0.3f),
      title = stringResource(CommonR.string.ai_info_battery_title),
      body = stringResource(CommonR.string.ai_info_battery_body),
    )

    Spacer(Modifier.height(16.dp))

    // Prompts used section
    Text(
      text = stringResource(CommonR.string.ai_info_prompts_section),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = stringResource(CommonR.string.ai_info_prompts_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(12.dp))

    PromptCard(
      icon = Icons.Rounded.Psychology,
      title = stringResource(CommonR.string.ai_info_system_prompt),
      content = state.hintSystemPrompt,
    )

    Spacer(Modifier.height(12.dp))

    PromptCard(
      icon = Icons.Rounded.Code,
      title = stringResource(CommonR.string.ai_info_puzzle_prompt),
      content = state.hintSamplePrompt,
    )

    Spacer(Modifier.height(24.dp))

    // Change model button
    if (onNavigateToSettings != null) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onNavigateToSettings),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.Settings,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = stringResource(CommonR.string.ai_info_change_model),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    Spacer(Modifier.height(32.dp))
  }
}

@Composable
private fun WhatItDoesCard() {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        .padding(20.dp)
  ) {
    Text(
      text = stringResource(CommonR.string.ai_info_what_title),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(Modifier.height(12.dp))

    Text(
      text = stringResource(CommonR.string.ai_info_what_body),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(20.dp))

    FeatureItem(
      icon = Icons.Rounded.Psychology,
      iconColor = PalabritaColors.BrandIndigo,
      title = stringResource(CommonR.string.ai_info_feat_generation_title),
      subtitle = stringResource(CommonR.string.ai_info_feat_generation_body),
    )

    Spacer(Modifier.height(16.dp))

    FeatureItem(
      icon = Icons.Rounded.OfflineBolt,
      iconColor = Color(0xFF2563EB),
      title = stringResource(CommonR.string.ai_info_feat_instant_title),
      subtitle = stringResource(CommonR.string.ai_info_feat_instant_body),
    )

    Spacer(Modifier.height(16.dp))

    FeatureItem(
      icon = Icons.Rounded.Security,
      iconColor = Color(0xFF16A34A),
      title = stringResource(CommonR.string.ai_info_feat_privacy_title),
      subtitle = stringResource(CommonR.string.ai_info_feat_privacy_body),
    )
  }
}

@Composable
private fun FeatureItem(icon: ImageVector, iconColor: Color, title: String, subtitle: String) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(40.dp).background(iconColor.copy(alpha = 0.1f), CircleShape),
    ) {
      Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CurrentModelCard(state: AiInfoState) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        .padding(20.dp)
  ) {
    Text(
      text = stringResource(CommonR.string.ai_info_model_section),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(Modifier.height(16.dp))

    val info = state.modelInfo
    if (info != null) {
      ModelRow(
        icon = Icons.Rounded.SmartToy,
        label = stringResource(CommonR.string.ai_info_model_type),
        value = info.displayName,
      )
      Spacer(Modifier.height(12.dp))
      ModelRow(
        icon = Icons.Rounded.Storage,
        label = stringResource(CommonR.string.ai_info_model_size),
        value = formatBytes(info.sizeBytes),
      )
      Spacer(Modifier.height(12.dp))
      ModelRow(
        icon = Icons.Rounded.Memory,
        label = stringResource(CommonR.string.ai_info_model_ram),
        value = "${info.requiredRamMb / 1024} GB",
      )
    } else {
      Text(
        text = stringResource(CommonR.string.ai_info_no_model),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ModelRow(icon: ImageVector, label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun HighlightCard(
  icon: ImageVector,
  iconColor: Color,
  borderColor: Color,
  title: String,
  body: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        .background(iconColor.copy(alpha = 0.04f))
        .padding(20.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
    Spacer(Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = iconColor,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun PromptCard(icon: ImageVector, title: String, content: String) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, PalabritaColors.BrandIndigo.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        .background(PalabritaColors.BrandIndigo.copy(alpha = 0.04f))
        .padding(16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        icon,
        contentDescription = null,
        tint = PalabritaColors.BrandIndigo,
        modifier = Modifier.size(20.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = content,
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private fun formatBytes(bytes: Long): String {
  val gb = bytes / 1_000_000_000.0
  return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(bytes / 1_000_000.0)
}
