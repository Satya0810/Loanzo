package com.loanzo.app.ui.loan;

import com.loanzo.app.data.dao.PayeeDao;
import com.loanzo.app.data.network.LeegalityService;
import com.loanzo.app.data.repository.LoanRepository;
import com.loanzo.app.data.repository.UserRepository;
import com.loanzo.app.domain.RuleEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class LoanViewModel_Factory implements Factory<LoanViewModel> {
  private final Provider<LoanRepository> loanRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<PayeeDao> payeeDaoProvider;

  private final Provider<RuleEngine> ruleEngineProvider;

  private final Provider<LeegalityService> leegalityServiceProvider;

  public LoanViewModel_Factory(Provider<LoanRepository> loanRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<PayeeDao> payeeDaoProvider,
      Provider<RuleEngine> ruleEngineProvider,
      Provider<LeegalityService> leegalityServiceProvider) {
    this.loanRepositoryProvider = loanRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.payeeDaoProvider = payeeDaoProvider;
    this.ruleEngineProvider = ruleEngineProvider;
    this.leegalityServiceProvider = leegalityServiceProvider;
  }

  @Override
  public LoanViewModel get() {
    return newInstance(loanRepositoryProvider.get(), userRepositoryProvider.get(), payeeDaoProvider.get(), ruleEngineProvider.get(), leegalityServiceProvider.get());
  }

  public static LoanViewModel_Factory create(Provider<LoanRepository> loanRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<PayeeDao> payeeDaoProvider,
      Provider<RuleEngine> ruleEngineProvider,
      Provider<LeegalityService> leegalityServiceProvider) {
    return new LoanViewModel_Factory(loanRepositoryProvider, userRepositoryProvider, payeeDaoProvider, ruleEngineProvider, leegalityServiceProvider);
  }

  public static LoanViewModel newInstance(LoanRepository loanRepository,
      UserRepository userRepository, PayeeDao payeeDao, RuleEngine ruleEngine,
      LeegalityService leegalityService) {
    return new LoanViewModel(loanRepository, userRepository, payeeDao, ruleEngine, leegalityService);
  }
}
