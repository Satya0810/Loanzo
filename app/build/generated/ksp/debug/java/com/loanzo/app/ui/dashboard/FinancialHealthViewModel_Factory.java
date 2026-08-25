package com.loanzo.app.ui.dashboard;

import com.loanzo.app.data.dao.DisbursementDao;
import com.loanzo.app.data.dao.RepaymentDao;
import com.loanzo.app.data.repository.LoanRepository;
import com.loanzo.app.data.repository.UserRepository;
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
public final class FinancialHealthViewModel_Factory implements Factory<FinancialHealthViewModel> {
  private final Provider<LoanRepository> loanRepositoryProvider;

  private final Provider<DisbursementDao> disbursementDaoProvider;

  private final Provider<RepaymentDao> repaymentDaoProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public FinancialHealthViewModel_Factory(Provider<LoanRepository> loanRepositoryProvider,
      Provider<DisbursementDao> disbursementDaoProvider,
      Provider<RepaymentDao> repaymentDaoProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.loanRepositoryProvider = loanRepositoryProvider;
    this.disbursementDaoProvider = disbursementDaoProvider;
    this.repaymentDaoProvider = repaymentDaoProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public FinancialHealthViewModel get() {
    return newInstance(loanRepositoryProvider.get(), disbursementDaoProvider.get(), repaymentDaoProvider.get(), userRepositoryProvider.get());
  }

  public static FinancialHealthViewModel_Factory create(
      Provider<LoanRepository> loanRepositoryProvider,
      Provider<DisbursementDao> disbursementDaoProvider,
      Provider<RepaymentDao> repaymentDaoProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new FinancialHealthViewModel_Factory(loanRepositoryProvider, disbursementDaoProvider, repaymentDaoProvider, userRepositoryProvider);
  }

  public static FinancialHealthViewModel newInstance(LoanRepository loanRepository,
      DisbursementDao disbursementDao, RepaymentDao repaymentDao, UserRepository userRepository) {
    return new FinancialHealthViewModel(loanRepository, disbursementDao, repaymentDao, userRepository);
  }
}
