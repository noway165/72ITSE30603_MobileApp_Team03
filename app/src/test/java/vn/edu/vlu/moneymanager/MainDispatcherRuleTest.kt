package vn.edu.vlu.moneymanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Checks the test setup itself: viewModelScope can run once MainDispatcherRule replaces Dispatchers.Main. */
class MainDispatcherRuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class CounterViewModel : ViewModel() {
        val count = MutableStateFlow(0)
        fun increment() {
            viewModelScope.launch { count.value++ }
        }
    }

    @Test
    fun viewModelScope_runsOnTestDispatcher() = runTest {
        val viewModel = CounterViewModel()
        viewModel.increment()
        assertEquals(1, viewModel.count.value)
    }
}
