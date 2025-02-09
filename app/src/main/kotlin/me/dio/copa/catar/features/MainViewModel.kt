package me.dio.copa.catar.features

import android.content.Context
import android.content.res.Resources.NotFoundException
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.dio.copa.catar.R
import me.dio.copa.catar.core.BaseViewModel
import me.dio.copa.catar.domain.model.Match
import me.dio.copa.catar.domain.model.MatchDomain
import me.dio.copa.catar.domain.usecase.DisableNotificationUseCase
import me.dio.copa.catar.domain.usecase.EnableNotificationUseCase
import me.dio.copa.catar.domain.usecase.GetMatchesUseCase
import me.dio.copa.catar.remote.UnexpectedException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val context: Context,
    private val getMatchesUseCase: GetMatchesUseCase,
    private val disableNotificationUseCase: DisableNotificationUseCase,
    private val enableNotificationUseCase: EnableNotificationUseCase
) : BaseViewModel<MainUiState, MainUiAction>(MainUiState()) {

    init {
        fetchMatches()
    }

    private fun fetchMatches() = viewModelScope.launch {
        getMatchesUseCase()
            .flowOn(Dispatchers.Main)
            .catch {
                when (it) {
                    is NotFoundException -> sendAction(
                        MainUiAction.MatchesNotFound(
                            it.message ?: context.getString(R.string.not_found_message)
                        )
                    )

                    is UnexpectedException -> sendAction(
                        MainUiAction.Unexpected
                    )
                }
            }.collect {
                setState {
                    copy(matches = it)
                }
            }
    }

    fun toggleNotification(match: Match) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Main) {
                    val action = if (match.notificationEnabled) {
                        disableNotificationUseCase(match.id)
                        MainUiAction.DisableNotification(match)
                    } else {
                        enableNotificationUseCase(match.id)
                        MainUiAction.EnableNotification(match)
                    }

                    sendAction(action)
                }
            }
        }
    }
}


data class MainUiState(
    val matches: List<MatchDomain> = emptyList()
)

sealed interface MainUiAction {
    object Unexpected : MainUiAction
    data class MatchesNotFound(val message: String) : MainUiAction
    data class EnableNotification(val match: MatchDomain) : MainUiAction
    data class DisableNotification(val match: MatchDomain) : MainUiAction
}