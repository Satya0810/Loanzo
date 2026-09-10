package com.loanzo.app.ui.loan

import com.google.common.truth.Truth.assertThat
import com.loanzo.app.testutil.MainDispatcherExtension
import com.loanzo.app.util.TranslationHelper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TranslationViewModelTest {

    @RegisterExtension
    @JvmField
    val mainDispatcher = MainDispatcherExtension()

    private val translationHelper = mockk<TranslationHelper>()
    private lateinit var viewModel: TranslationViewModel

    @BeforeEach
    fun setUp() {
        viewModel = TranslationViewModel(translationHelper)
    }

    @Test
    @DisplayName("Initial state has isLoading false and empty translated text")
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.translatedText).isEmpty()
    }

    @Test
    @DisplayName("translate updates state with translated text upon success")
    fun `translate updates state with translated text`() = runTest {
        coEvery { translationHelper.translateText("Loan approval", "hi") } returns "ऋण स्वीकृति"

        viewModel.translate("Loan approval", "hi")

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.translatedText).isEqualTo("ऋण स्वीकृति")
    }

    @Test
    @DisplayName("translate sets fallback error message when helper returns null")
    fun `translate provides fallback message when translation returns null`() = runTest {
        coEvery { translationHelper.translateText("Unknown phrase", "fr") } returns null

        viewModel.translate("Unknown phrase", "fr")

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.translatedText).isEqualTo("Translation failed.")
    }
}
