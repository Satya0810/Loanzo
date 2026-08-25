package com.loanzo.app.di;

import com.loanzo.app.data.LoanzoDatabase;
import com.loanzo.app.data.dao.GuarantorDao;
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
public final class DatabaseModule_ProvideGuarantorDaoFactory implements Factory<GuarantorDao> {
  private final Provider<LoanzoDatabase> dbProvider;

  public DatabaseModule_ProvideGuarantorDaoFactory(Provider<LoanzoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public GuarantorDao get() {
    return provideGuarantorDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideGuarantorDaoFactory create(
      Provider<LoanzoDatabase> dbProvider) {
    return new DatabaseModule_ProvideGuarantorDaoFactory(dbProvider);
  }

  public static GuarantorDao provideGuarantorDao(LoanzoDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideGuarantorDao(db));
  }
}
