package com.loanzo.app.di;

import com.loanzo.app.data.LoanzoDatabase;
import com.loanzo.app.data.dao.PayeeDao;
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
public final class DatabaseModule_ProvidePayeeDaoFactory implements Factory<PayeeDao> {
  private final Provider<LoanzoDatabase> dbProvider;

  public DatabaseModule_ProvidePayeeDaoFactory(Provider<LoanzoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PayeeDao get() {
    return providePayeeDao(dbProvider.get());
  }

  public static DatabaseModule_ProvidePayeeDaoFactory create(Provider<LoanzoDatabase> dbProvider) {
    return new DatabaseModule_ProvidePayeeDaoFactory(dbProvider);
  }

  public static PayeeDao providePayeeDao(LoanzoDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePayeeDao(db));
  }
}
