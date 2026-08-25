package com.loanzo.app.data.repository;

import com.loanzo.app.data.dao.AuditEventDao;
import com.loanzo.app.data.dao.DisbursementDao;
import com.loanzo.app.data.dao.LoanDao;
import com.loanzo.app.data.dao.PledgeDao;
import com.loanzo.app.data.dao.RepaymentDao;
import com.loanzo.app.domain.PenaltyEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class LoanRepository_Factory implements Factory<LoanRepository> {
  private final Provider<LoanDao> loanDaoProvider;

  private final Provider<DisbursementDao> disbursementDaoProvider;

  private final Provider<RepaymentDao> repaymentDaoProvider;

  private final Provider<PledgeDao> pledgeDaoProvider;

  private final Provider<AuditEventDao> auditEventDaoProvider;

  private final Provider<PenaltyEngine> penaltyEngineProvider;

  public LoanRepository_Factory(Provider<LoanDao> loanDaoProvider,
      Provider<DisbursementDao> disbursementDaoProvider,
      Provider<RepaymentDao> repaymentDaoProvider, Provider<PledgeDao> pledgeDaoProvider,
      Provider<AuditEventDao> auditEventDaoProvider,
      Provider<PenaltyEngine> penaltyEngineProvider) {
    this.loanDaoProvider = loanDaoProvider;
    this.disbursementDaoProvider = disbursementDaoProvider;
    this.repaymentDaoProvider = repaymentDaoProvider;
    this.pledgeDaoProvider = pledgeDaoProvider;
    this.auditEventDaoProvider = auditEventDaoProvider;
    this.penaltyEngineProvider = penaltyEngineProvider;
  }

  @Override
  public LoanRepository get() {
    return newInstance(loanDaoProvider.get(), disbursementDaoProvider.get(), repaymentDaoProvider.get(), pledgeDaoProvider.get(), auditEventDaoProvider.get(), penaltyEngineProvider.get());
  }

  public static LoanRepository_Factory create(Provider<LoanDao> loanDaoProvider,
      Provider<DisbursementDao> disbursementDaoProvider,
      Provider<RepaymentDao> repaymentDaoProvider, Provider<PledgeDao> pledgeDaoProvider,
      Provider<AuditEventDao> auditEventDaoProvider,
      Provider<PenaltyEngine> penaltyEngineProvider) {
    return new LoanRepository_Factory(loanDaoProvider, disbursementDaoProvider, repaymentDaoProvider, pledgeDaoProvider, auditEventDaoProvider, penaltyEngineProvider);
  }

  public static LoanRepository newInstance(LoanDao loanDao, DisbursementDao disbursementDao,
      RepaymentDao repaymentDao, PledgeDao pledgeDao, AuditEventDao auditEventDao,
      PenaltyEngine penaltyEngine) {
    return new LoanRepository(loanDao, disbursementDao, repaymentDao, pledgeDao, auditEventDao, penaltyEngine);
  }
}
