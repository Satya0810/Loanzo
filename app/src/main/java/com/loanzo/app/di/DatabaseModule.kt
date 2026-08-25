package com.loanzo.app.di

import android.content.Context
import androidx.room.Room
import com.loanzo.app.data.LoanzoDatabase
import com.loanzo.app.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LoanzoDatabase {
        return Room.databaseBuilder(
            context,
            LoanzoDatabase::class.java,
            "loanzo_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides fun provideUserDao(db: LoanzoDatabase): UserDao = db.userDao()
    @Provides fun provideLoanDao(db: LoanzoDatabase): LoanDao = db.loanDao()
    @Provides fun provideDisbursementDao(db: LoanzoDatabase): DisbursementDao = db.disbursementDao()
    @Provides fun provideRepaymentDao(db: LoanzoDatabase): RepaymentDao = db.repaymentDao()
    @Provides fun providePayeeDao(db: LoanzoDatabase): PayeeDao = db.payeeDao()
    @Provides fun providePledgeDao(db: LoanzoDatabase): PledgeDao = db.pledgeDao()
    @Provides fun provideAuditEventDao(db: LoanzoDatabase): AuditEventDao = db.auditEventDao()
    @Provides fun provideGuarantorDao(db: LoanzoDatabase): GuarantorDao = db.guarantorDao()
    @Provides fun provideSyncQueueDao(db: LoanzoDatabase): SyncQueueDao = db.syncQueueDao()
}
