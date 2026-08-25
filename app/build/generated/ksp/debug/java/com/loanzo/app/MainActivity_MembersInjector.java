package com.loanzo.app;

import com.loanzo.app.data.repository.UserRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<UserRepository> userRepositoryProvider;

  public MainActivity_MembersInjector(Provider<UserRepository> userRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<UserRepository> userRepositoryProvider) {
    return new MainActivity_MembersInjector(userRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectUserRepository(instance, userRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.loanzo.app.MainActivity.userRepository")
  public static void injectUserRepository(MainActivity instance, UserRepository userRepository) {
    instance.userRepository = userRepository;
  }
}
