package com.hitsuji.pdfdrivereader

import com.hitsuji.pdfdrivereader.domain.model.AppMode
import com.hitsuji.pdfdrivereader.domain.model.AppSession
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the [MainViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private val appConfigRepository: AppConfigurationRepository = mock()
    private lateinit var viewModel: MainViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun `setup viewModel`() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun `tear down`() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that the ViewModel correctly exposes the session from the repository.
     */
    @Test
    fun `session should reflect data from repository`() = runTest {
        val expectedSession = AppSession(AppMode.READER, "uri1")
        whenever(appConfigRepository.getSession()) doReturn flowOf(expectedSession)
        
        viewModel = MainViewModel(appConfigRepository)
        advanceUntilIdle()
        
        assertEquals(expectedSession, viewModel.session.value)
    }
}
