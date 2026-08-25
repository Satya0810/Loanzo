package com.loanzo.app.ui.auth;

import com.loanzo.app.data.didit.DiditVerificationService;
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<DiditVerificationService> diditServiceProvider;

  public AuthViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<DiditVerificationService> diditServiceProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.diditServiceProvider = diditServiceProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(userRepositoryProvider.get(), diditServiceProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<DiditVerificationService> diditServiceProvider) {
    return new AuthViewModel_Factory(userRepositoryProvider, diditServiceProvider);
  }

  public static AuthViewModel newInstance(UserRepository userRepository,
      DiditVerificationService diditService) {
    return new AuthViewModel(userRepository, diditService);
  }
}
