package com.loanzo.app.di;

import com.loanzo.app.data.LoanzoDatabase;
import com.loanzo.app.data.dao.DisbursementDao;
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
public final class DatabaseModule_ProvideDisbursementDaoFactory implements Factory<DisbursementDao> {
  private final Provider<LoanzoDatabase> dbProvider;

  public DatabaseModule_ProvideDisbursementDaoFactory(Provider<LoanzoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DisbursementDao get() {
    return provideDisbursementDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDisbursementDaoFactory create(
      Provider<LoanzoDatabase> dbProvider) {
    return new DatabaseModule_ProvideDisbursementDaoFactory(dbProvider);
  }

  public static DisbursementDao provideDisbursementDao(LoanzoDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDisbursementDao(db));
  }
}
