package com.example

import com.example.core.common.AppResult
import com.example.core.common.BankError
import com.example.domain.model.AccountStatus
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.TransactionDirection
import com.example.domain.model.UserSession
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BankingRepository
import com.example.domain.repository.SessionState
import com.example.domain.usecase.GetAccountBalanceUseCase
import com.example.domain.usecase.GetAccountTransactionsUseCase
import com.example.domain.usecase.GetBankAccountsUseCase
import com.example.domain.usecase.ObserveSessionUseCase
import com.example.ui.dashboard.DashboardContentState
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.TransactionsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits Success state with balance and transactions when session is authenticated`() = runTest {
        val sessionFlow = MutableStateFlow<SessionState>(
            SessionState.Authenticated(
                UserSession(
                    provider = OpenBankingProvider.FINOTECH,
                    accessToken = "tok_1234567890",
                    refreshToken = "ref_123",
                    expiresAtEpochMs = System.currentTimeMillis() + 3600_000L,
                    clientId = "client_1"
                )
            )
        )
        val fakeAuthRepo = FakeAuthRepository(sessionFlow)
        val fakeBankingRepo = FakeBankingRepository(shouldFail = false)

        val vm = DashboardViewModel(
            observeSessionUseCase = ObserveSessionUseCase(fakeAuthRepo),
            authRepository = fakeAuthRepo,
            getBankAccountsUseCase = GetBankAccountsUseCase(fakeBankingRepo),
            getAccountBalanceUseCase = GetAccountBalanceUseCase(fakeBankingRepo),
            getAccountTransactionsUseCase = GetAccountTransactionsUseCase(fakeBankingRepo)
        )

        advanceUntilIdle()

        val content = vm.uiState.value.contentState
        assertTrue(content is DashboardContentState.Success)
        val success = content as DashboardContentState.Success
        assertEquals(1, success.accounts.size)
        assertEquals(85_000_000L, success.accounts.first().balanceRial)
        assertTrue(success.transactionsState is TransactionsUiState.Success)
        val txs = (success.transactionsState as TransactionsUiState.Success).transactions
        assertEquals(1, txs.size)
        assertEquals("Paya Transfer", txs.first().description)
    }

    @Test
    fun `emits Error state when repository fails to fetch accounts`() = runTest {
        val sessionFlow = MutableStateFlow<SessionState>(
            SessionState.Authenticated(
                UserSession(
                    provider = OpenBankingProvider.FINOTECH,
                    accessToken = "tok_1234567890",
                    refreshToken = null,
                    expiresAtEpochMs = System.currentTimeMillis() + 3600_000L,
                    clientId = "client_1"
                )
            )
        )
        val fakeAuthRepo = FakeAuthRepository(sessionFlow)
        val fakeBankingRepo = FakeBankingRepository(shouldFail = true)

        val vm = DashboardViewModel(
            observeSessionUseCase = ObserveSessionUseCase(fakeAuthRepo),
            authRepository = fakeAuthRepo,
            getBankAccountsUseCase = GetBankAccountsUseCase(fakeBankingRepo),
            getAccountBalanceUseCase = GetAccountBalanceUseCase(fakeBankingRepo),
            getAccountTransactionsUseCase = GetAccountTransactionsUseCase(fakeBankingRepo)
        )

        advanceUntilIdle()

        val content = vm.uiState.value.contentState
        assertTrue(content is DashboardContentState.Error)
        assertTrue((content as DashboardContentState.Error).error is BankError.Network)
    }

    private class FakeAuthRepository(
        private val flow: MutableStateFlow<SessionState>
    ) : AuthRepository {
        override fun observeSessionState(): Flow<SessionState> = flow
        override fun getSelectedProvider(): OpenBankingProvider = OpenBankingProvider.FINOTECH
        override suspend fun switchProvider(provider: OpenBankingProvider) = Unit
        override suspend fun exchangeAuthorizationCode(
            provider: OpenBankingProvider,
            authorizationCode: String,
            redirectUri: String
        ): AppResult<UserSession> = AppResult.Error(BankError.Unauthorized())
        override suspend fun requestClientCredentialsSession(provider: OpenBankingProvider): AppResult<UserSession> =
            AppResult.Error(BankError.Unauthorized())
        override suspend fun refreshActiveSession(): AppResult<UserSession> = AppResult.Error(BankError.Unauthorized())
        override suspend fun logout() = Unit
        override fun isProviderConfigured(provider: OpenBankingProvider): Boolean = true
    }

    private class FakeBankingRepository(private val shouldFail: Boolean) : BankingRepository {
        private val sampleAccount = BankAccount(
            depositNumber = "10023456789",
            iban = "IR820540102680020817909002",
            cardNumberMasked = null,
            bankName = "Parsian Bank",
            bankCode = "054",
            accountTitle = "Current Account",
            balanceRial = 85_000_000L,
            withdrawableBalanceRial = 85_000_000L,
            status = AccountStatus.ACTIVE,
            provider = OpenBankingProvider.FINOTECH
        )

        override suspend fun getAccounts(): AppResult<List<BankAccount>> =
            if (shouldFail) AppResult.Error(BankError.Network("Gateway unreachable"))
            else AppResult.Success(listOf(sampleAccount))

        override suspend fun getAccountBalance(depositNumber: String): AppResult<BankAccount> =
            if (shouldFail) AppResult.Error(BankError.Network("Gateway unreachable"))
            else AppResult.Success(sampleAccount)

        override suspend fun getAccountTransactions(depositNumber: String): AppResult<List<BankTransaction>> =
            if (shouldFail) AppResult.Error(BankError.Network("Gateway unreachable"))
            else AppResult.Success(
                listOf(
                    BankTransaction(
                        id = "tx_1",
                        depositNumber = depositNumber,
                        amountRial = 5_000_000L,
                        balanceAfterRial = 85_000_000L,
                        direction = TransactionDirection.CREDIT,
                        description = "Paya Transfer",
                        dateLabel = "14030815",
                        referenceNumber = "567891",
                        rail = null
                    )
                )
            )

        override suspend fun inquireIban(iban: String): AppResult<IbanInquiryResult> =
            AppResult.Error(BankError.Network())
    }
}
