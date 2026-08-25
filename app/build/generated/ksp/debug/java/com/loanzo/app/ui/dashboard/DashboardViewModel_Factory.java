package com.loanzo.app.ui.dashboard;

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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<LoanRepository> loanRepositoryProvider;

  public DashboardViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<LoanRepository> loanRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.loanRepositoryProvider = loanRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(userRepositoryProvider.get(), loanRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<LoanRepository> loanRepositoryProvider) {
    return new DashboardViewModel_Factory(userRepositoryProvider, loanRepositoryProvider);
  }

  public static DashboardViewModel newInstance(UserRepository userRepository,
      LoanRepository loanRepository) {
    return new DashboardViewModel(userRepository, loanRepository);
  }
}
