package com.loanzo.app.data.repository;

import android.content.Context;
import com.loanzo.app.data.dao.UserDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class UserRepository_Factory implements Factory<UserRepository> {
  private final Provider<UserDao> userDaoProvider;

  private final Provider<Context> contextProvider;

  public UserRepository_Factory(Provider<UserDao> userDaoProvider,
      Provider<Context> contextProvider) {
    this.userDaoProvider = userDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public UserRepository get() {
    return newInstance(userDaoProvider.get(), contextProvider.get());
  }

  public static UserRepository_Factory create(Provider<UserDao> userDaoProvider,
      Provider<Context> contextProvider) {
    return new UserRepository_Factory(userDaoProvider, contextProvider);
  }

  public static UserRepository newInstance(UserDao userDao, Context context) {
    return new UserRepository(userDao, context);
  }
}
