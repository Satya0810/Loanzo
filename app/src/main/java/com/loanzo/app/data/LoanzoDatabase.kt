package com.loanzo.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.loanzo.app.data.dao.*
import com.loanzo.app.data.entity.*

@Database(
    entities = [
        UserEntity::class,
        LoanEntity::class,
        DisbursementEntity::class,
        RepaymentEntity::class,
        PayeeEntity::class,
        PledgeEntity::class,
        AuditEventEntity::class,
        GuarantorEntity::class,
        SyncQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LoanzoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun loanDao(): LoanDao
    abstract fun disbursementDao(): DisbursementDao
    abstract fun repaymentDao(): RepaymentDao
    abstract fun payeeDao(): PayeeDao
    abstract fun pledgeDao(): PledgeDao
    abstract fun auditEventDao(): AuditEventDao
    abstract fun guarantorDao(): GuarantorDao
    abstract fun syncQueueDao(): SyncQueueDao
}
