package com.loanzo.app.di;

import com.loanzo.app.data.LoanzoDatabase;
import com.loanzo.app.data.dao.RepaymentDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideRepaymentDaoFactory implements Factory<RepaymentDao> {
  private final Provider<LoanzoDatabase> dbProvider;

  public DatabaseModule_ProvideRepaymentDaoFactory(Provider<LoanzoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public RepaymentDao get() {
    return provideRepaymentDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideRepaymentDaoFactory create(
      Provider<LoanzoDatabase> dbProvider) {
    return new DatabaseModule_ProvideRepaymentDaoFactory(dbProvider);
  }

  public static RepaymentDao provideRepaymentDao(LoanzoDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRepaymentDao(db));
  }
}
