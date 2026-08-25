package com.loanzo.app.di;

import com.loanzo.app.data.LoanzoDatabase;
import com.loanzo.app.data.dao.PledgeDao;
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
public final class DatabaseModule_ProvidePledgeDaoFactory implements Factory<PledgeDao> {
  private final Provider<LoanzoDatabase> dbProvider;

  public DatabaseModule_ProvidePledgeDaoFactory(Provider<LoanzoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PledgeDao get() {
    return providePledgeDao(dbProvider.get());
  }

  public static DatabaseModule_ProvidePledgeDaoFactory create(Provider<LoanzoDatabase> dbProvider) {
    return new DatabaseModule_ProvidePledgeDaoFactory(dbProvider);
  }

  public static PledgeDao providePledgeDao(LoanzoDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePledgeDao(db));
  }
}
