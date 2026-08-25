package com.loanzo.app.di;

import com.loanzo.app.data.LoanzoDatabase;
import com.loanzo.app.data.dao.AuditEventDao;
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
public final class DatabaseModule_ProvideAuditEventDaoFactory implements Factory<AuditEventDao> {
  private final Provider<LoanzoDatabase> dbProvider;

  public DatabaseModule_ProvideAuditEventDaoFactory(Provider<LoanzoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public AuditEventDao get() {
    return provideAuditEventDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideAuditEventDaoFactory create(
      Provider<LoanzoDatabase> dbProvider) {
    return new DatabaseModule_ProvideAuditEventDaoFactory(dbProvider);
  }

  public static AuditEventDao provideAuditEventDao(LoanzoDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAuditEventDao(db));
  }
}
