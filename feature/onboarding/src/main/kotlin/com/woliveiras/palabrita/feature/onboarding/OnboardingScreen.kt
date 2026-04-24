package com.woliveiras.palabrita.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.common.PalabritaColors
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.ModelId
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
  onComplete: () -> Unit,
  onNavigateToGeneration: (com.woliveiras.palabrita.core.model.ModelId) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OnboardingViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(state.currentStep) {
    when (state.currentStep) {
      OnboardingStep.GENERATION -> state.selectedModel?.let { onNavigateToGeneration(it) }
      OnboardingStep.COMPLETE -> onComplete()
      else -> Unit
    }
  }

  // Map transient navigation steps to DOWNLOAD so the screen keeps rendering
  // meaningful content while the NavHost exit animation plays, preventing a blank frame.
  val displayStep =
    when (state.currentStep) {
      OnboardingStep.GENERATION,
      OnboardingStep.COMPLETE -> OnboardingStep.DOWNLOAD
      else -> state.currentStep
    }

  AnimatedContent(
    targetState = displayStep,
    transitionSpec = {
      (slideInHorizontally { it } + fadeIn()) togetherWith
        (slideOutHorizontally { -it } + fadeOut())
    },
    label = "onboarding-step",
  ) { step ->
    when (step) {
      OnboardingStep.WELCOME ->
        WelcomeScreen(onNext = { viewModel.onAction(OnboardingAction.Next) })
      OnboardingStep.LANGUAGE ->
        LanguageScreen(
          selectedLanguage = state.selectedLanguage,
          onSelectLanguage = { viewModel.onAction(OnboardingAction.SelectLanguage(it)) },
          onNext = { viewModel.onAction(OnboardingAction.Next) },
          onBack = { viewModel.onAction(OnboardingAction.Back) },
        )
      OnboardingStep.MODEL_SELECTION ->
        ModelSelectionScreen(
          deviceTier = state.deviceTier,
          selectedModel = state.selectedModel,
          showTierWarning = state.showTierWarning,
          onSelectModel = { viewModel.onAction(OnboardingAction.SelectModel(it)) },
          onAutoSelect = { viewModel.onAction(OnboardingAction.AutoSelectModel) },
          onDismissWarning = { viewModel.onAction(OnboardingAction.DismissTierWarning) },
          onNext = { viewModel.onAction(OnboardingAction.Next) },
          onBack = { viewModel.onAction(OnboardingAction.Back) },
        )
      OnboardingStep.DOWNLOAD ->
        DownloadScreen(
          modelId = state.selectedModel,
          downloadProgress = state.downloadProgress,
          downloadedBytes = state.downloadedBytes,
          totalBytes = state.totalBytes,
          downloadFailed = state.downloadFailed,
          errorMessage = state.downloadErrorMessage,
          downloadComplete = state.downloadComplete,
          onRetry = { viewModel.onAction(OnboardingAction.RetryDownload) },
          onCancel = { viewModel.onAction(OnboardingAction.CancelDownload) },
          onContinue = { viewModel.onAction(OnboardingAction.ProceedToGeneration) },
        )
      OnboardingStep.GENERATION,
      OnboardingStep.COMPLETE -> Unit
    }
  }

  if (state.showTierWarning) {
    TierWarningDialog(
      onConfirm = { viewModel.onAction(OnboardingAction.DismissTierWarning) },
      onChooseOther = {
        viewModel.onAction(OnboardingAction.DismissTierWarning)
        viewModel.onAction(OnboardingAction.AutoSelectModel)
      },
    )
  }
}

@Composable
private fun WelcomeScreen(onNext: () -> Unit) {
  var logoVisible by remember { mutableStateOf(false) }
  var titleVisible by remember { mutableStateOf(false) }
  var descVisible by remember { mutableStateOf(false) }
  var card0Visible by remember { mutableStateOf(false) }
  var card1Visible by remember { mutableStateOf(false) }
  var card2Visible by remember { mutableStateOf(false) }
  var buttonVisible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    logoVisible = true
    delay(100)
    titleVisible = true
    delay(100)
    descVisible = true
    delay(100)
    card0Visible = true
    delay(100)
    card1Visible = true
    delay(100)
    card2Visible = true
    delay(100)
    buttonVisible = true
  }

  val features =
    listOf(
      Triple(
        Icons.Rounded.Psychology,
        CommonR.string.feature_ai_title,
        CommonR.string.feature_ai_desc,
      ),
      Triple(
        Icons.Rounded.Bolt,
        CommonR.string.feature_offline_title,
        CommonR.string.feature_offline_desc,
      ),
      Triple(
        Icons.Rounded.Shield,
        CommonR.string.feature_private_title,
        CommonR.string.feature_private_desc,
      ),
    )
  val cardVisibilities = listOf(card0Visible, card1Visible, card2Visible)

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(32.dp))

    AnimatedVisibility(
      visible = logoVisible,
      enter =
        scaleIn(initialScale = 0.9f, animationSpec = tween(500, easing = FastOutSlowInEasing)) +
          fadeIn(tween(500)),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(80.dp)
            .background(
              Brush.linearGradient(
                listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)
              ),
              RoundedCornerShape(24.dp),
            ),
      ) {
        Icon(
          imageVector = Icons.Rounded.Psychology,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(40.dp),
        )
      }
    }

    Spacer(Modifier.height(32.dp))

    AnimatedVisibility(
      visible = titleVisible,
      enter =
        slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(500)),
    ) {
      Text(
        text = stringResource(CommonR.string.welcome_title_new),
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    Spacer(Modifier.height(16.dp))

    AnimatedVisibility(
      visible = descVisible,
      enter =
        slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(500)),
    ) {
      Text(
        text = stringResource(CommonR.string.welcome_description),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(Modifier.height(32.dp))

    features.forEachIndexed { index, (icon, titleRes, descRes) ->
      AnimatedVisibility(
        visible = cardVisibilities[index],
        enter =
          slideInHorizontally(tween(500, easing = FastOutSlowInEasing)) { -it / 2 } +
            fadeIn(tween(500)),
      ) {
        FeatureCard(icon = icon, titleRes = titleRes, descRes = descRes)
      }
      if (index < features.lastIndex) Spacer(Modifier.height(12.dp))
    }

    Spacer(Modifier.height(32.dp))

    AnimatedVisibility(
      visible = buttonVisible,
      enter = fadeIn(tween(500)),
      modifier = Modifier.padding(bottom = 24.dp),
    ) {
      GradientButton(text = stringResource(CommonR.string.welcome_start), onClick = onNext)
    }
  }
}

@Composable
private fun FeatureCard(icon: ImageVector, titleRes: Int, descRes: Int) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.size(48.dp)
          .background(
            Brush.linearGradient(listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)),
            RoundedCornerShape(12.dp),
          ),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(24.dp),
      )
    }
    Column {
      Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = stringResource(descRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun LanguageScreen(
  selectedLanguage: String,
  onSelectLanguage: (String) -> Unit,
  onNext: () -> Unit,
  onBack: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
        .navigationBarsPadding()
  ) {
    BackButton(onBack)
    Column(
      modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(CommonR.string.language_title),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(CommonR.string.language_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(32.dp))

      LanguageCard("PT", "Português", "pt", selectedLanguage, onSelectLanguage)
      Spacer(Modifier.height(12.dp))
      LanguageCard("EN", "English", "en", selectedLanguage, onSelectLanguage)
      Spacer(Modifier.height(12.dp))
      LanguageCard("ES", "Español", "es", selectedLanguage, onSelectLanguage)
    }

    GradientButton(
      text = stringResource(CommonR.string.continue_button),
      onClick = onNext,
      modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
    )
  }
}

@Composable
private fun LanguageCard(
  flag: String,
  label: String,
  code: String,
  selected: String,
  onSelect: (String) -> Unit,
) {
  val isSelected = code == selected
  val borderColor =
    if (isSelected) PalabritaColors.BrandPurple else MaterialTheme.colorScheme.outline
  val bgColor =
    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.surface

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(bgColor)
        .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
        .clickable { onSelect(code) }
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Surface(
      shape = CircleShape,
      color =
        if (isSelected) PalabritaColors.BrandPurple else MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.size(40.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = flag,
          style = MaterialTheme.typography.labelMedium,
          color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Text(
      text = label,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun ModelSelectionScreen(
  deviceTier: DeviceTier,
  selectedModel: ModelId?,
  showTierWarning: Boolean,
  onSelectModel: (ModelId) -> Unit,
  onAutoSelect: () -> Unit,
  onDismissWarning: () -> Unit,
  onNext: () -> Unit,
  onBack: () -> Unit,
) {
  var headerVisible by remember { mutableStateOf(false) }
  val cardVisibilities = remember { List(6) { mutableStateOf(false) } }
  var buttonVisible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    headerVisible = true
    cardVisibilities.forEach { state ->
      delay(100)
      state.value = true
    }
    delay(100)
    buttonVisible = true
  }

  val modelOptions =
    listOf(
      Triple(
        ModelId.GEMMA4_E4B,
        stringResource(CommonR.string.model_gemma4_e4b_title),
        Pair(
          stringResource(CommonR.string.model_gemma4_e4b_subtitle),
          stringResource(CommonR.string.model_gemma4_e4b_info),
        ),
      ) to Pair(Icons.Rounded.AutoAwesome, listOf(Color(0xFFD946EF), Color(0xFF7C3AED))),
      Triple(
        ModelId.GEMMA4_E2B,
        stringResource(CommonR.string.model_powerful_title),
        Pair(
          stringResource(CommonR.string.model_powerful_subtitle),
          stringResource(CommonR.string.model_powerful_info),
        ),
      ) to
        Pair(
          Icons.Rounded.Memory,
          listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet),
        ),
      Triple(
        ModelId.PHI4_MINI,
        stringResource(CommonR.string.model_phi4_mini_title),
        Pair(
          stringResource(CommonR.string.model_phi4_mini_subtitle),
          stringResource(CommonR.string.model_phi4_mini_info),
        ),
      ) to Pair(Icons.Rounded.Psychology, listOf(Color(0xFF0EA5E9), Color(0xFF6366F1))),
      Triple(
        ModelId.DEEPSEEK_R1_1_5B,
        stringResource(CommonR.string.model_deepseek_r1_title),
        Pair(
          stringResource(CommonR.string.model_deepseek_r1_subtitle),
          stringResource(CommonR.string.model_deepseek_r1_info),
        ),
      ) to Pair(Icons.Rounded.Spellcheck, listOf(Color(0xFF10B981), Color(0xFF0891B2))),
      Triple(
        ModelId.QWEN2_5_1_5B,
        stringResource(CommonR.string.model_qwen25_1_5b_title),
        Pair(
          stringResource(CommonR.string.model_qwen25_1_5b_subtitle),
          stringResource(CommonR.string.model_qwen25_1_5b_info),
        ),
      ) to Pair(Icons.Rounded.Bolt, listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
      Triple(
        ModelId.QWEN3_0_6B,
        stringResource(CommonR.string.model_compact_title),
        Pair(
          stringResource(CommonR.string.model_compact_subtitle),
          stringResource(CommonR.string.model_compact_info),
        ),
      ) to Pair(Icons.Rounded.PhoneAndroid, listOf(Color(0xFF06B6D4), Color(0xFF2563EB))),
    )

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
        .navigationBarsPadding()
  ) {
    BackButton(onBack)

    Column(
      modifier =
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
    ) {
      AnimatedVisibility(
        visible = headerVisible,
        enter =
          slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 2 } +
            fadeIn(tween(500)),
      ) {
        Column {
          Text(
            text = stringResource(CommonR.string.model_selection_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = stringResource(CommonR.string.model_selection_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(24.dp))
        }
      }

      modelOptions.forEachIndexed { index, (modelTriple, iconPair) ->
        val (modelId, title, subtitleInfo) = modelTriple
        val (subtitle, info) = subtitleInfo
        val (icon, gradient) = iconPair
        AnimatedVisibility(
          visible = cardVisibilities[index].value,
          enter =
            slideInHorizontally(tween(500, easing = FastOutSlowInEasing)) { -it / 2 } +
              fadeIn(tween(500)),
        ) {
          ModelCard(
            title = title,
            subtitle = subtitle,
            info = info,
            icon = icon,
            iconGradient = gradient,
            isSelected = selectedModel == modelId,
            onClick = { onSelectModel(modelId) },
          )
        }
        if (index < modelOptions.lastIndex) Spacer(Modifier.height(12.dp))
      }

      Spacer(Modifier.height(16.dp))

      TextButton(onClick = onAutoSelect, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CommonR.string.model_auto_select), color = PalabritaColors.BrandPurple)
      }

      Spacer(Modifier.height(16.dp))
    }

    AnimatedVisibility(
      visible = buttonVisible,
      enter =
        slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(500)),
      modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
    ) {
      GradientButton(
        text = stringResource(CommonR.string.continue_button),
        onClick = onNext,
        enabled = selectedModel != null,
      )
    }
  }
}

@Composable
private fun ModelCard(
  title: String,
  subtitle: String,
  info: String,
  icon: ImageVector,
  iconGradient: List<Color>,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  val borderColor =
    if (isSelected) PalabritaColors.BrandPurple else MaterialTheme.colorScheme.outline
  val bgColor =
    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.surface

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(bgColor)
        .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(24.dp))
        .clickable { onClick() }
        .padding(20.dp),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.size(56.dp)
          .background(Brush.linearGradient(iconGradient), RoundedCornerShape(16.dp)),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(28.dp),
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (isSelected) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = PalabritaColors.BrandPurple,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Chip(text = info) }
    }
  }
}

@Composable
private fun DownloadScreen(
  modelId: ModelId?,
  downloadProgress: Float,
  downloadedBytes: Long,
  totalBytes: Long,
  downloadFailed: Boolean,
  errorMessage: String?,
  downloadComplete: Boolean,
  onRetry: () -> Unit,
  onCancel: () -> Unit,
  onContinue: () -> Unit,
) {
  // Derive visual status from progress + failed flag
  val isComplete = downloadProgress >= 1f && !downloadFailed
  val isVerifying = downloadProgress >= 1f && !isComplete && !downloadFailed

  val animatedProgress by
    animateFloatAsState(
      targetValue = downloadProgress,
      animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
      label = "download-progress",
    )

  val modelName =
    when (modelId) {
      ModelId.GEMMA4_E4B -> stringResource(CommonR.string.download_model_gemma4_e4b)
      ModelId.GEMMA4_E2B -> stringResource(CommonR.string.download_model_gemma4)
      ModelId.PHI4_MINI -> stringResource(CommonR.string.download_model_phi4_mini)
      ModelId.DEEPSEEK_R1_1_5B -> stringResource(CommonR.string.download_model_deepseek_r1)
      ModelId.QWEN2_5_1_5B -> stringResource(CommonR.string.download_model_qwen25_1_5b)
      ModelId.QWEN3_0_6B -> stringResource(CommonR.string.download_model_qwen3)
      else -> stringResource(CommonR.string.download_model_unknown)
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    // Status icon
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.size(96.dp)
          .background(
            Brush.linearGradient(listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)),
            RoundedCornerShape(24.dp),
          ),
    ) {
      val statusIcon =
        when {
          downloadFailed -> Icons.Rounded.ErrorOutline
          isComplete -> Icons.Rounded.Check
          else -> Icons.Rounded.Download
        }
      Icon(
        imageVector = statusIcon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(48.dp),
      )
    }

    Spacer(Modifier.height(32.dp))

    Text(
      text =
        when {
          downloadFailed -> stringResource(CommonR.string.download_failed)
          isComplete -> stringResource(CommonR.string.download_complete_title)
          isVerifying -> stringResource(CommonR.string.download_verifying_title)
          else -> stringResource(CommonR.string.download_title)
        },
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(Modifier.height(8.dp))

    Text(
      text =
        when {
          downloadFailed -> errorMessage ?: stringResource(CommonR.string.download_failed)
          isComplete -> stringResource(CommonR.string.download_complete_subtitle)
          isVerifying -> stringResource(CommonR.string.download_verifying_subtitle)
          else -> stringResource(CommonR.string.download_subtitle, modelName)
        },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(32.dp))

    if (!downloadFailed) {
      // Progress row
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = stringResource(CommonR.string.download_progress_label),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${(animatedProgress * 100).toInt()}%",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Spacer(Modifier.height(8.dp))

      // Gradient progress bar
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.outline)
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
              .height(10.dp)
              .clip(RoundedCornerShape(50.dp))
              .background(
                Brush.horizontalGradient(
                  listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)
                )
              )
        )
      }
    }

    Spacer(Modifier.height(24.dp))

    // Curiosities slider
    if (!downloadFailed) {
      CuriositySlider()
    }

    if (downloadFailed) {
      Spacer(Modifier.height(24.dp))
      GradientButton(text = stringResource(CommonR.string.download_retry), onClick = onRetry)
      Spacer(Modifier.height(12.dp))
      TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CommonR.string.download_choose_other))
      }
    } else if (isComplete) {
      Spacer(Modifier.height(24.dp))
      GradientButton(
        text = stringResource(CommonR.string.continue_button),
        onClick = onContinue,
        enabled = downloadComplete,
      )
    } else {
      Spacer(Modifier.height(16.dp))
      TextButton(onClick = onCancel) { Text(stringResource(CommonR.string.cancel)) }
    }
  }
}

@Composable
private fun GradientButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .fillMaxWidth()
        .height(56.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          if (enabled) {
            Brush.horizontalGradient(
              listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)
            )
          } else {
            Brush.horizontalGradient(
              listOf(
                PalabritaColors.BrandIndigo.copy(alpha = 0.4f),
                PalabritaColors.BrandViolet.copy(alpha = 0.4f),
              )
            )
          }
        )
        .clickable(enabled = enabled, onClick = onClick),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
      color = Color.White,
    )
  }
}

@Composable
private fun CuriositySlider() {
  val curiosities =
    listOf(
      stringResource(CommonR.string.download_curiosity_1),
      stringResource(CommonR.string.download_curiosity_2),
      stringResource(CommonR.string.download_curiosity_3),
      stringResource(CommonR.string.download_curiosity_4),
      stringResource(CommonR.string.download_curiosity_5),
      stringResource(CommonR.string.download_curiosity_6),
    )

  var currentIndex by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(4000)
      currentIndex = (currentIndex + 1) % curiosities.size
    }
  }

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    AnimatedContent(
      targetState = currentIndex,
      transitionSpec = { (fadeIn(tween(400))) togetherWith (fadeOut(tween(400))) },
      label = "curiosity-slide",
    ) { index ->
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
            .border(
              1.dp,
              MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
              RoundedCornerShape(16.dp),
            )
            .padding(16.dp)
      ) {
        Text(
          text = curiosities[index],
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      curiosities.indices.forEach { i ->
        Box(
          modifier =
            Modifier.size(if (i == currentIndex) 8.dp else 6.dp)
              .background(
                if (i == currentIndex) PalabritaColors.BrandIndigo
                else PalabritaColors.BrandIndigo.copy(alpha = 0.3f),
                CircleShape,
              )
        )
      }
    }
  }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
  IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
      contentDescription = stringResource(CommonR.string.back),
      tint = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun Chip(text: String) {
  Box(
    modifier =
      Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50.dp))
        .padding(horizontal = 12.dp, vertical = 4.dp)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun TierWarningDialog(onConfirm: () -> Unit, onChooseOther: () -> Unit) {
  AlertDialog(
    onDismissRequest = onChooseOther,
    title = { Text(stringResource(CommonR.string.warning)) },
    text = { Text(stringResource(CommonR.string.tier_warning_message)) },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(CommonR.string.tier_warning_confirm)) }
    },
    dismissButton = {
      TextButton(onClick = onChooseOther) {
        Text(stringResource(CommonR.string.tier_warning_choose_other))
      }
    },
  )
}

private fun formatBytes(bytes: Long): String {
  val gb = bytes / 1_000_000_000.0
  return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(bytes / 1_000_000.0)
}
