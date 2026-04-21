package com.woliveiras.palabrita.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.model.MessageRole
import androidx.compose.ui.res.stringResource
import com.woliveiras.palabrita.core.common.R as CommonR

private val SUGGESTIONS: List<@Composable () -> String> = listOf(
  { stringResource(CommonR.string.chat_suggestion_origin) },
  { stringResource(CommonR.string.chat_suggestion_synonyms) },
  { stringResource(CommonR.string.chat_suggestion_sentence) },
  { stringResource(CommonR.string.chat_suggestion_english) },
  { stringResource(CommonR.string.chat_suggestion_trivia) },
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
  puzzleId: Long,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ChatViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()

  LaunchedEffect(state.messages.size) {
    if (state.messages.isNotEmpty()) {
      listState.animateScrollToItem(state.messages.size - 1)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(CommonR.string.chat_title, state.word.replaceFirstChar { it.uppercase() })) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(CommonR.string.back))
          }
        },
      )
    },
  ) { padding ->
    Column(
      modifier = modifier.fillMaxSize().padding(padding),
    ) {
      // Messages
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(state.messages) { message ->
          MessageBubble(message)
        }

        if (state.isModelResponding) {
          item {
            TypingIndicator()
          }
        }
      }

      // Message counter
      if (state.userMessageCount > 0) {
        Text(
          text = stringResource(CommonR.string.chat_message_count, state.userMessageCount, state.maxMessages),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }

      // Suggestions
      if (state.suggestionsVisible) {
        FlowRow(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SUGGESTIONS.forEach { suggestionProvider ->
            val suggestion = suggestionProvider()
            AssistChip(
              onClick = { viewModel.onAction(ChatAction.SelectSuggestion(suggestion)) },
              label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        Spacer(Modifier.height(8.dp))
      }

      // Limit reached
      if (state.isAtLimit) {
        Text(
          text = stringResource(CommonR.string.chat_limit_reached),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }

      // Input
      Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = state.currentInput,
          onValueChange = { viewModel.onAction(ChatAction.UpdateInput(it)) },
          modifier = Modifier.weight(1f),
          placeholder = { Text(stringResource(CommonR.string.chat_input_placeholder)) },
          enabled = !state.isModelResponding && !state.isAtLimit,
          singleLine = true,
          shape = RoundedCornerShape(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
          onClick = { viewModel.onAction(ChatAction.SendMessage) },
          enabled = state.currentInput.isNotBlank() && !state.isModelResponding && !state.isAtLimit,
        ) {
          Icon(
            Icons.AutoMirrored.Rounded.Send,
            contentDescription = stringResource(CommonR.string.send),
            tint = if (state.currentInput.isNotBlank() && !state.isModelResponding)
              MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun MessageBubble(message: UiChatMessage) {
  val isUser = message.role == MessageRole.USER
  val alignment = if (isUser) Alignment.End else Alignment.Start
  val bgColor = if (isUser)
    MaterialTheme.colorScheme.primary
  else MaterialTheme.colorScheme.secondaryContainer
  val textColor = if (isUser)
    MaterialTheme.colorScheme.onPrimary
  else MaterialTheme.colorScheme.onSecondaryContainer

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = alignment,
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 280.dp)
        .clip(
          RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp,
          ),
        )
        .background(bgColor)
        .padding(12.dp),
    ) {
      Text(
        text = message.content,
        style = MaterialTheme.typography.bodyMedium,
        color = textColor,
      )
    }
  }
}

@Composable
private fun TypingIndicator() {
  Box(
    modifier = Modifier
      .widthIn(max = 80.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.secondaryContainer)
      .padding(12.dp),
  ) {
    Text(
      text = "...",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
  }
}
