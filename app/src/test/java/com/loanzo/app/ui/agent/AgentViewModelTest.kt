package com.loanzo.app.ui.agent

import com.google.common.truth.Truth.assertThat
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.data.repository.AgentRepository
import com.loanzo.app.data.repository.UserRepository
import com.loanzo.app.testutil.MainDispatcherExtension
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AgentViewModelTest {

    @RegisterExtension
    @JvmField
    val mainDispatcher = MainDispatcherExtension()

    private val agentRepository = mockk<AgentRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)

    private val sampleOfficer = UserEntity(
        userId = "agt_vikram_01",
        name = "Vikram Malhotra",
        role = "AGENT",
        agentStatus = "ACTIVE",
        isOnDuty = true,
        totalAgentEarnings = 4500.0,
        kycStatus = "VERIFIED"
    )

    private val sampleVisit = AgentVisitEntity(
        visitId = "vst_101",
        agentId = "agt_vikram_01",
        loanId = "loan_991",
        visitType = "COLLATERAL_VERIFICATION",
        title = "Gold Appraisal - Anita Roy",
        targetAddress = "Flat 402, Royal Residency, Indiranagar, Bengaluru",
        payoutAmount = 350.0,
        status = "ASSIGNED",
        stage = "SCHEDULED",
        scheduledDate = "Today",
        collateralItemName = "22K Gold Bangles",
        collateralEstimatedValue = 185000.0,
        handshakePin = "4821"
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        coEvery { userRepository.getCurrentUserIdSync() } returns "agt_vikram_01"
        coEvery { userRepository.getUserById("agt_vikram_01") } returns sampleOfficer
        every { userRepository.observeUser("agt_vikram_01") } returns flowOf(sampleOfficer)
        every { agentRepository.observeVisitsByAgent("agt_vikram_01") } returns flowOf(listOf(sampleVisit))
    }

    private fun createViewModel(): AgentViewModel {
        return AgentViewModel(
            agentRepository = agentRepository,
            userRepository = userRepository
        )
    }

    @Nested
    @DisplayName("Initialization & State Hydration")
    inner class InitializationTests {

        @Test
        @DisplayName("GIVEN active officer, WHEN initialized, THEN state is hydrated with officer and visits")
        fun hydratesInitialStateCorrectly() = runTest {
            val vm = createViewModel()
            val state = vm.uiState.value

            assertThat(state.isLoading).isFalse()
            assertThat(state.user?.name).isEqualTo("Vikram Malhotra")
            assertThat(state.isOnDuty).isTrue()
            assertThat(state.totalEarnings).isEqualTo(4500.0)
            assertThat(state.visits).hasSize(1)
            assertThat(state.todayVisitsCount).isEqualTo(1)
            assertThat(state.pendingVisitsCount).isEqualTo(1)
            assertThat(state.completedVisitsCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Duty Status & Tab Controls")
    inner class IntentTests {

        @Test
        @DisplayName("WHEN ToggleDutyStatus is dispatched, THEN repository updates duty status")
        fun togglesDutyStatus() = runTest {
            val vm = createViewModel()

            vm.onIntent(AgentIntent.ToggleDutyStatus(false))

            coVerify { userRepository.updateAgentDutyStatus("agt_vikram_01", false) }
            assertThat(vm.uiState.value.isOnDuty).isFalse()
        }

        @Test
        @DisplayName("WHEN SelectTab is dispatched, THEN selectedTab index updates")
        fun updatesSelectedTab() = runTest {
            val vm = createViewModel()

            vm.onIntent(AgentIntent.SelectTab(2))

            assertThat(vm.uiState.value.selectedTab).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Inspection Workflow")
    inner class InspectionTests {

        @Test
        @DisplayName("WHEN StartInspection is dispatched, THEN activeInspectionVisit is set")
        fun startsInspection() = runTest {
            val vm = createViewModel()

            vm.onIntent(AgentIntent.StartInspection(sampleVisit))

            assertThat(vm.uiState.value.activeInspectionVisit).isEqualTo(sampleVisit)
        }

        @Test
        @DisplayName("WHEN DismissInspection is dispatched, THEN activeInspectionVisit is cleared")
        fun dismissesInspection() = runTest {
            val vm = createViewModel()
            vm.onIntent(AgentIntent.StartInspection(sampleVisit))

            vm.onIntent(AgentIntent.DismissInspection)

            assertThat(vm.uiState.value.activeInspectionVisit).isNull()
        }

        @Test
        @DisplayName("WHEN CompleteVisitWithGps is dispatched, THEN repository is invoked and payout dialog shown")
        fun completesVisitWithGps() = runTest {
            val vm = createViewModel()

            vm.onIntent(
                AgentIntent.CompleteVisitWithGps(
                    visitId = "vst_101",
                    remarks = "All physical tests passed.",
                    isCollateralAuthentic = true,
                    isBorrowerVerified = true,
                    isLenderVerified = false,
                    photoProof = "content://proof_photo.jpg",
                    appraisedValue = 192000.0,
                    officerRecommendation = "RECOMMEND_APPROVAL",
                    latitude = 12.9716,
                    longitude = 77.5946
                )
            )

            coVerify {
                agentRepository.completeVisit(
                    visitId = "vst_101",
                    remarks = "All physical tests passed.",
                    isCollateralAuthentic = true,
                    isBorrowerVerified = true,
                    isLenderVerified = false,
                    photoProof = "content://proof_photo.jpg",
                    appraisedValue = 192000.0,
                    officerRecommendation = "RECOMMEND_APPROVAL",
                    agentLatitude = 12.9716,
                    agentLongitude = 77.5946
                )
            }
            assertThat(vm.uiState.value.activeInspectionVisit).isNull()
            assertThat(vm.uiState.value.showPayoutSuccessDialog).isEqualTo(350.0)
        }
    }
}
