package com.woliveiras.palabrita.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woliveiras.palabrita.core.common.LocalGameColors
import com.woliveiras.palabrita.core.common.PalabritaColors
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.GameRules

@Composable
fun HowToPlayScreen(onBack: () -> Unit, onStartPlaying: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp)
  ) {
    // --- Back button ---
    IconButton(onClick = onBack) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = stringResource(CommonR.string.back),
      )
    }

    Spacer(Modifier.height(8.dp))

    // --- Header ---
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Icon(
            imageVector = Icons.Rounded.HelpOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
          )
        }
      }
      Spacer(Modifier.width(16.dp))
      Column {
        Text(
          text = stringResource(CommonR.string.htp_title),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(CommonR.string.htp_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Spacer(Modifier.height(24.dp))

    // --- The Goal ---
    SectionCard {
      Text(
        text = stringResource(CommonR.string.htp_goal_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(CommonR.string.htp_goal_body, GameRules.MAX_ATTEMPTS),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(Modifier.height(16.dp))

    // --- Examples ---
    SectionCard {
      Text(
        text = stringResource(CommonR.string.htp_examples_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(16.dp))
      ExampleRow(
        letters = listOf("P", "I", "X", "E", "L"),
        highlightIndex = 0,
        highlightColor = LocalGameColors.current.correct,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(CommonR.string.htp_example_correct),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))
      ExampleRow(
        letters = listOf("C", "O", "D", "E", "R"),
        highlightIndex = 1,
        highlightColor = LocalGameColors.current.present,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(CommonR.string.htp_example_present),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))
      ExampleRow(
        letters = listOf("B", "Y", "T", "E", "S"),
        highlightIndex = -1,
        highlightColor = LocalGameColors.current.absent,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(CommonR.string.htp_example_absent),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(Modifier.height(16.dp))

    // --- Color Guide ---
    SectionCard {
      Text(
        text = stringResource(CommonR.string.htp_color_guide_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(12.dp))
      ColorGuideRow(
        color = LocalGameColors.current.correct,
        icon = Icons.Rounded.CheckCircle,
        label = stringResource(CommonR.string.htp_color_green),
        description = stringResource(CommonR.string.htp_color_green_desc),
      )
      Spacer(Modifier.height(12.dp))
      ColorGuideRow(
        color = LocalGameColors.current.present,
        icon = Icons.Rounded.HelpOutline,
        label = stringResource(CommonR.string.htp_color_yellow),
        description = stringResource(CommonR.string.htp_color_yellow_desc),
      )
      Spacer(Modifier.height(12.dp))
      ColorGuideRow(
        color = LocalGameColors.current.absent,
        icon = Icons.Rounded.Cancel,
        label = stringResource(CommonR.string.htp_color_gray),
        description = stringResource(CommonR.string.htp_color_gray_desc),
      )
    }

    Spacer(Modifier.height(16.dp))

    // --- Pro Tip ---
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.tertiaryContainer,
      contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "\uD83D\uDCA1 ${stringResource(CommonR.string.htp_pro_tip_title)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = stringResource(CommonR.string.htp_pro_tip_body),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    Spacer(Modifier.height(24.dp))

    // --- Start Playing CTA ---
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxWidth()
          .height(56.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(
            Brush.horizontalGradient(
              listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)
            )
          )
          .clickable(onClick = onStartPlaying),
    ) {
      Text(
        text = stringResource(CommonR.string.htp_start_playing),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
    }

    Spacer(Modifier.height(24.dp))
  }
}

// --- Section Card ---

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp)) { content() }
  }
}

// --- Example Row ---

@Composable
private fun ExampleRow(letters: List<String>, highlightIndex: Int, highlightColor: Color) {
  val gameColors = LocalGameColors.current
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    letters.forEachIndexed { index, letter ->
      val bgColor =
        when {
          highlightIndex < 0 -> gameColors.unused
          index == highlightIndex -> highlightColor
          else -> gameColors.unused
        }
      val textColor =
        if (bgColor == gameColors.unused) MaterialTheme.colorScheme.onSurface else Color.White
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(bgColor),
      ) {
        Text(
          text = letter,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = textColor,
        )
      }
    }
  }
}

// --- Color Guide Row ---

@Composable
private fun ColorGuideRow(
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  description: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(36.dp)) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Column {
      Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
