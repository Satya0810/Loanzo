package com.loanzo.app.di;

import com.loanzo.app.data.LoanzoDatabase;
import com.loanzo.app.data.dao.LoanDao;
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
public final class DatabaseModule_ProvideLoanDaoFactory implements Factory<LoanDao> {
  private final Provider<LoanzoDatabase> dbProvider;

  public DatabaseModule_ProvideLoanDaoFactory(Provider<LoanzoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public LoanDao get() {
    return provideLoanDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideLoanDaoFactory create(Provider<LoanzoDatabase> dbProvider) {
    return new DatabaseModule_ProvideLoanDaoFactory(dbProvider);
  }

  public static LoanDao provideLoanDao(LoanzoDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLoanDao(db));
  }
}
