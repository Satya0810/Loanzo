package com.loanzo.app.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.loanzo.app.data.dao.AuditEventDao;
import com.loanzo.app.data.dao.AuditEventDao_Impl;
import com.loanzo.app.data.dao.DisbursementDao;
import com.loanzo.app.data.dao.DisbursementDao_Impl;
import com.loanzo.app.data.dao.GuarantorDao;
import com.loanzo.app.data.dao.GuarantorDao_Impl;
import com.loanzo.app.data.dao.LoanDao;
import com.loanzo.app.data.dao.LoanDao_Impl;
import com.loanzo.app.data.dao.PayeeDao;
import com.loanzo.app.data.dao.PayeeDao_Impl;
import com.loanzo.app.data.dao.PledgeDao;
import com.loanzo.app.data.dao.PledgeDao_Impl;
import com.loanzo.app.data.dao.RepaymentDao;
import com.loanzo.app.data.dao.RepaymentDao_Impl;
import com.loanzo.app.data.dao.SyncQueueDao;
import com.loanzo.app.data.dao.SyncQueueDao_Impl;
import com.loanzo.app.data.dao.UserDao;
import com.loanzo.app.data.dao.UserDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LoanzoDatabase_Impl extends LoanzoDatabase {
  private volatile UserDao _userDao;

  private volatile LoanDao _loanDao;

  private volatile DisbursementDao _disbursementDao;

  private volatile RepaymentDao _repaymentDao;

  private volatile PayeeDao _payeeDao;

  private volatile PledgeDao _pledgeDao;

  private volatile AuditEventDao _auditEventDao;

  private volatile GuarantorDao _guarantorDao;

  private volatile SyncQueueDao _syncQueueDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`userId` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, `role` TEXT NOT NULL, `kycStatus` TEXT NOT NULL, `panNumber` TEXT NOT NULL, `aadhaarVerified` INTEGER NOT NULL, `selfieVerified` INTEGER NOT NULL, `upiId` TEXT NOT NULL, `bankAccountNumber` TEXT NOT NULL, `profilePhotoUri` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`userId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `loans` (`loanId` TEXT NOT NULL, `lenderId` TEXT NOT NULL, `borrowerId` TEXT NOT NULL, `sanctionedAmount` REAL NOT NULL, `disbursedAmount` REAL NOT NULL, `outstandingAmount` REAL NOT NULL, `purpose` TEXT NOT NULL, `loanType` TEXT NOT NULL, `interestRate` REAL NOT NULL, `interestModel` TEXT NOT NULL, `tenureMonths` INTEGER NOT NULL, `status` TEXT NOT NULL, `repaymentFrequency` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `closedAt` INTEGER, `notes` TEXT NOT NULL, `agreementDocumentId` TEXT, `isAgreementSigned` INTEGER NOT NULL, `agreementUrl` TEXT, `penaltyRate` REAL NOT NULL, `penaltyModel` TEXT NOT NULL, `originalTenureMonths` INTEGER NOT NULL, `moratoriumMonths` INTEGER NOT NULL, `isRestructured` INTEGER NOT NULL, `restructuredAt` INTEGER, PRIMARY KEY(`loanId`), FOREIGN KEY(`lenderId`) REFERENCES `users`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`borrowerId`) REFERENCES `users`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_lenderId` ON `loans` (`lenderId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_borrowerId` ON `loans` (`borrowerId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `disbursements` (`disbursementId` TEXT NOT NULL, `loanId` TEXT NOT NULL, `amount` REAL NOT NULL, `payeeId` TEXT, `payeeName` TEXT NOT NULL, `purpose` TEXT NOT NULL, `purposeCategory` TEXT NOT NULL, `verificationStatus` TEXT NOT NULL, `ruleEngineResult` TEXT NOT NULL, `approvalStatus` TEXT NOT NULL, `transactionRef` TEXT NOT NULL, `upiDeepLink` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `lenderNote` TEXT NOT NULL, `borrowerNote` TEXT NOT NULL, PRIMARY KEY(`disbursementId`), FOREIGN KEY(`loanId`) REFERENCES `loans`(`loanId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`payeeId`) REFERENCES `payees`(`payeeId`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_disbursements_loanId` ON `disbursements` (`loanId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_disbursements_payeeId` ON `disbursements` (`payeeId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `repayments` (`repaymentId` TEXT NOT NULL, `loanId` TEXT NOT NULL, `amount` REAL NOT NULL, `transactionRef` TEXT NOT NULL, `status` TEXT NOT NULL, `dueDate` INTEGER NOT NULL, `paidDate` INTEGER, `outstandingSnapshot` REAL NOT NULL, `principalComponent` REAL NOT NULL, `interestComponent` REAL NOT NULL, `penalty` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`repaymentId`), FOREIGN KEY(`loanId`) REFERENCES `loans`(`loanId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_repayments_loanId` ON `repayments` (`loanId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `payees` (`payeeId` TEXT NOT NULL, `name` TEXT NOT NULL, `upiId` TEXT NOT NULL, `gstNumber` TEXT NOT NULL, `businessName` TEXT NOT NULL, `verificationStatus` TEXT NOT NULL, `category` TEXT NOT NULL, `verifiedAt` INTEGER, `addedBy` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`payeeId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pledges` (`pledgeId` TEXT NOT NULL, `loanId` TEXT NOT NULL, `assetDescription` TEXT NOT NULL, `assetType` TEXT NOT NULL, `estimatedValue` REAL NOT NULL, `weight` REAL NOT NULL, `photoUri` TEXT NOT NULL, `receiptUri` TEXT NOT NULL, `receiptStatus` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`pledgeId`), FOREIGN KEY(`loanId`) REFERENCES `loans`(`loanId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pledges_loanId` ON `pledges` (`loanId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_events` (`eventId` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `actor` TEXT NOT NULL, `event` TEXT NOT NULL, `oldState` TEXT NOT NULL, `newState` TEXT NOT NULL, `description` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `reference` TEXT NOT NULL, PRIMARY KEY(`eventId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `guarantors` (`guarantorId` TEXT NOT NULL, `loanId` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `email` TEXT NOT NULL, `panNumber` TEXT NOT NULL, `relationship` TEXT NOT NULL, `consentStatus` TEXT NOT NULL, `consentTimestamp` INTEGER, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`guarantorId`), FOREIGN KEY(`loanId`) REFERENCES `loans`(`loanId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_guarantors_loanId` ON `guarantors` (`loanId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_queue` (`syncId` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `operation` TEXT NOT NULL, `payload` TEXT NOT NULL, `status` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`syncId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '37313dc5546ef8cd31a7aa1be89ef236')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `loans`");
        db.execSQL("DROP TABLE IF EXISTS `disbursements`");
        db.execSQL("DROP TABLE IF EXISTS `repayments`");
        db.execSQL("DROP TABLE IF EXISTS `payees`");
        db.execSQL("DROP TABLE IF EXISTS `pledges`");
        db.execSQL("DROP TABLE IF EXISTS `audit_events`");
        db.execSQL("DROP TABLE IF EXISTS `guarantors`");
        db.execSQL("DROP TABLE IF EXISTS `sync_queue`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(13);
        _columnsUsers.put("userId", new TableInfo.Column("userId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("kycStatus", new TableInfo.Column("kycStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("panNumber", new TableInfo.Column("panNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("aadhaarVerified", new TableInfo.Column("aadhaarVerified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("selfieVerified", new TableInfo.Column("selfieVerified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("upiId", new TableInfo.Column("upiId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("bankAccountNumber", new TableInfo.Column("bankAccountNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("profilePhotoUri", new TableInfo.Column("profilePhotoUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.loanzo.app.data.entity.UserEntity).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsLoans = new HashMap<String, TableInfo.Column>(25);
        _columnsLoans.put("loanId", new TableInfo.Column("loanId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("lenderId", new TableInfo.Column("lenderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("borrowerId", new TableInfo.Column("borrowerId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("sanctionedAmount", new TableInfo.Column("sanctionedAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("disbursedAmount", new TableInfo.Column("disbursedAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("outstandingAmount", new TableInfo.Column("outstandingAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("purpose", new TableInfo.Column("purpose", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("loanType", new TableInfo.Column("loanType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("interestRate", new TableInfo.Column("interestRate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("interestModel", new TableInfo.Column("interestModel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("tenureMonths", new TableInfo.Column("tenureMonths", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("repaymentFrequency", new TableInfo.Column("repaymentFrequency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("closedAt", new TableInfo.Column("closedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("agreementDocumentId", new TableInfo.Column("agreementDocumentId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("isAgreementSigned", new TableInfo.Column("isAgreementSigned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("agreementUrl", new TableInfo.Column("agreementUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("penaltyRate", new TableInfo.Column("penaltyRate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("penaltyModel", new TableInfo.Column("penaltyModel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("originalTenureMonths", new TableInfo.Column("originalTenureMonths", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("moratoriumMonths", new TableInfo.Column("moratoriumMonths", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("isRestructured", new TableInfo.Column("isRestructured", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("restructuredAt", new TableInfo.Column("restructuredAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLoans = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysLoans.add(new TableInfo.ForeignKey("users", "CASCADE", "NO ACTION", Arrays.asList("lenderId"), Arrays.asList("userId")));
        _foreignKeysLoans.add(new TableInfo.ForeignKey("users", "CASCADE", "NO ACTION", Arrays.asList("borrowerId"), Arrays.asList("userId")));
        final HashSet<TableInfo.Index> _indicesLoans = new HashSet<TableInfo.Index>(2);
        _indicesLoans.add(new TableInfo.Index("index_loans_lenderId", false, Arrays.asList("lenderId"), Arrays.asList("ASC")));
        _indicesLoans.add(new TableInfo.Index("index_loans_borrowerId", false, Arrays.asList("borrowerId"), Arrays.asList("ASC")));
        final TableInfo _infoLoans = new TableInfo("loans", _columnsLoans, _foreignKeysLoans, _indicesLoans);
        final TableInfo _existingLoans = TableInfo.read(db, "loans");
        if (!_infoLoans.equals(_existingLoans)) {
          return new RoomOpenHelper.ValidationResult(false, "loans(com.loanzo.app.data.entity.LoanEntity).\n"
                  + " Expected:\n" + _infoLoans + "\n"
                  + " Found:\n" + _existingLoans);
        }
        final HashMap<String, TableInfo.Column> _columnsDisbursements = new HashMap<String, TableInfo.Column>(15);
        _columnsDisbursements.put("disbursementId", new TableInfo.Column("disbursementId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("loanId", new TableInfo.Column("loanId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("payeeId", new TableInfo.Column("payeeId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("payeeName", new TableInfo.Column("payeeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("purpose", new TableInfo.Column("purpose", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("purposeCategory", new TableInfo.Column("purposeCategory", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("verificationStatus", new TableInfo.Column("verificationStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("ruleEngineResult", new TableInfo.Column("ruleEngineResult", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("approvalStatus", new TableInfo.Column("approvalStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("transactionRef", new TableInfo.Column("transactionRef", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("upiDeepLink", new TableInfo.Column("upiDeepLink", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("lenderNote", new TableInfo.Column("lenderNote", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDisbursements.put("borrowerNote", new TableInfo.Column("borrowerNote", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDisbursements = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysDisbursements.add(new TableInfo.ForeignKey("loans", "CASCADE", "NO ACTION", Arrays.asList("loanId"), Arrays.asList("loanId")));
        _foreignKeysDisbursements.add(new TableInfo.ForeignKey("payees", "SET NULL", "NO ACTION", Arrays.asList("payeeId"), Arrays.asList("payeeId")));
        final HashSet<TableInfo.Index> _indicesDisbursements = new HashSet<TableInfo.Index>(2);
        _indicesDisbursements.add(new TableInfo.Index("index_disbursements_loanId", false, Arrays.asList("loanId"), Arrays.asList("ASC")));
        _indicesDisbursements.add(new TableInfo.Index("index_disbursements_payeeId", false, Arrays.asList("payeeId"), Arrays.asList("ASC")));
        final TableInfo _infoDisbursements = new TableInfo("disbursements", _columnsDisbursements, _foreignKeysDisbursements, _indicesDisbursements);
        final TableInfo _existingDisbursements = TableInfo.read(db, "disbursements");
        if (!_infoDisbursements.equals(_existingDisbursements)) {
          return new RoomOpenHelper.ValidationResult(false, "disbursements(com.loanzo.app.data.entity.DisbursementEntity).\n"
                  + " Expected:\n" + _infoDisbursements + "\n"
                  + " Found:\n" + _existingDisbursements);
        }
        final HashMap<String, TableInfo.Column> _columnsRepayments = new HashMap<String, TableInfo.Column>(13);
        _columnsRepayments.put("repaymentId", new TableInfo.Column("repaymentId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("loanId", new TableInfo.Column("loanId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("transactionRef", new TableInfo.Column("transactionRef", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("dueDate", new TableInfo.Column("dueDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("paidDate", new TableInfo.Column("paidDate", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("outstandingSnapshot", new TableInfo.Column("outstandingSnapshot", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("principalComponent", new TableInfo.Column("principalComponent", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("interestComponent", new TableInfo.Column("interestComponent", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("penalty", new TableInfo.Column("penalty", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepayments.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRepayments = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysRepayments.add(new TableInfo.ForeignKey("loans", "CASCADE", "NO ACTION", Arrays.asList("loanId"), Arrays.asList("loanId")));
        final HashSet<TableInfo.Index> _indicesRepayments = new HashSet<TableInfo.Index>(1);
        _indicesRepayments.add(new TableInfo.Index("index_repayments_loanId", false, Arrays.asList("loanId"), Arrays.asList("ASC")));
        final TableInfo _infoRepayments = new TableInfo("repayments", _columnsRepayments, _foreignKeysRepayments, _indicesRepayments);
        final TableInfo _existingRepayments = TableInfo.read(db, "repayments");
        if (!_infoRepayments.equals(_existingRepayments)) {
          return new RoomOpenHelper.ValidationResult(false, "repayments(com.loanzo.app.data.entity.RepaymentEntity).\n"
                  + " Expected:\n" + _infoRepayments + "\n"
                  + " Found:\n" + _existingRepayments);
        }
        final HashMap<String, TableInfo.Column> _columnsPayees = new HashMap<String, TableInfo.Column>(10);
        _columnsPayees.put("payeeId", new TableInfo.Column("payeeId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("upiId", new TableInfo.Column("upiId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("gstNumber", new TableInfo.Column("gstNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("businessName", new TableInfo.Column("businessName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("verificationStatus", new TableInfo.Column("verificationStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("verifiedAt", new TableInfo.Column("verifiedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("addedBy", new TableInfo.Column("addedBy", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayees.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPayees = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPayees = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPayees = new TableInfo("payees", _columnsPayees, _foreignKeysPayees, _indicesPayees);
        final TableInfo _existingPayees = TableInfo.read(db, "payees");
        if (!_infoPayees.equals(_existingPayees)) {
          return new RoomOpenHelper.ValidationResult(false, "payees(com.loanzo.app.data.entity.PayeeEntity).\n"
                  + " Expected:\n" + _infoPayees + "\n"
                  + " Found:\n" + _existingPayees);
        }
        final HashMap<String, TableInfo.Column> _columnsPledges = new HashMap<String, TableInfo.Column>(11);
        _columnsPledges.put("pledgeId", new TableInfo.Column("pledgeId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("loanId", new TableInfo.Column("loanId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("assetDescription", new TableInfo.Column("assetDescription", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("assetType", new TableInfo.Column("assetType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("estimatedValue", new TableInfo.Column("estimatedValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("weight", new TableInfo.Column("weight", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("photoUri", new TableInfo.Column("photoUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("receiptUri", new TableInfo.Column("receiptUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("receiptStatus", new TableInfo.Column("receiptStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPledges.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPledges = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysPledges.add(new TableInfo.ForeignKey("loans", "CASCADE", "NO ACTION", Arrays.asList("loanId"), Arrays.asList("loanId")));
        final HashSet<TableInfo.Index> _indicesPledges = new HashSet<TableInfo.Index>(1);
        _indicesPledges.add(new TableInfo.Index("index_pledges_loanId", false, Arrays.asList("loanId"), Arrays.asList("ASC")));
        final TableInfo _infoPledges = new TableInfo("pledges", _columnsPledges, _foreignKeysPledges, _indicesPledges);
        final TableInfo _existingPledges = TableInfo.read(db, "pledges");
        if (!_infoPledges.equals(_existingPledges)) {
          return new RoomOpenHelper.ValidationResult(false, "pledges(com.loanzo.app.data.entity.PledgeEntity).\n"
                  + " Expected:\n" + _infoPledges + "\n"
                  + " Found:\n" + _existingPledges);
        }
        final HashMap<String, TableInfo.Column> _columnsAuditEvents = new HashMap<String, TableInfo.Column>(10);
        _columnsAuditEvents.put("eventId", new TableInfo.Column("eventId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("entityType", new TableInfo.Column("entityType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("entityId", new TableInfo.Column("entityId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("actor", new TableInfo.Column("actor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("event", new TableInfo.Column("event", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("oldState", new TableInfo.Column("oldState", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("newState", new TableInfo.Column("newState", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditEvents.put("reference", new TableInfo.Column("reference", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAuditEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAuditEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAuditEvents = new TableInfo("audit_events", _columnsAuditEvents, _foreignKeysAuditEvents, _indicesAuditEvents);
        final TableInfo _existingAuditEvents = TableInfo.read(db, "audit_events");
        if (!_infoAuditEvents.equals(_existingAuditEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "audit_events(com.loanzo.app.data.entity.AuditEventEntity).\n"
                  + " Expected:\n" + _infoAuditEvents + "\n"
                  + " Found:\n" + _existingAuditEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsGuarantors = new HashMap<String, TableInfo.Column>(10);
        _columnsGuarantors.put("guarantorId", new TableInfo.Column("guarantorId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("loanId", new TableInfo.Column("loanId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("panNumber", new TableInfo.Column("panNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("relationship", new TableInfo.Column("relationship", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("consentStatus", new TableInfo.Column("consentStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("consentTimestamp", new TableInfo.Column("consentTimestamp", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuarantors.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGuarantors = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysGuarantors.add(new TableInfo.ForeignKey("loans", "CASCADE", "NO ACTION", Arrays.asList("loanId"), Arrays.asList("loanId")));
        final HashSet<TableInfo.Index> _indicesGuarantors = new HashSet<TableInfo.Index>(1);
        _indicesGuarantors.add(new TableInfo.Index("index_guarantors_loanId", false, Arrays.asList("loanId"), Arrays.asList("ASC")));
        final TableInfo _infoGuarantors = new TableInfo("guarantors", _columnsGuarantors, _foreignKeysGuarantors, _indicesGuarantors);
        final TableInfo _existingGuarantors = TableInfo.read(db, "guarantors");
        if (!_infoGuarantors.equals(_existingGuarantors)) {
          return new RoomOpenHelper.ValidationResult(false, "guarantors(com.loanzo.app.data.entity.GuarantorEntity).\n"
                  + " Expected:\n" + _infoGuarantors + "\n"
                  + " Found:\n" + _existingGuarantors);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncQueue = new HashMap<String, TableInfo.Column>(8);
        _columnsSyncQueue.put("syncId", new TableInfo.Column("syncId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("entityType", new TableInfo.Column("entityType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("entityId", new TableInfo.Column("entityId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("operation", new TableInfo.Column("operation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("payload", new TableInfo.Column("payload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("retryCount", new TableInfo.Column("retryCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncQueue = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncQueue = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncQueue = new TableInfo("sync_queue", _columnsSyncQueue, _foreignKeysSyncQueue, _indicesSyncQueue);
        final TableInfo _existingSyncQueue = TableInfo.read(db, "sync_queue");
        if (!_infoSyncQueue.equals(_existingSyncQueue)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_queue(com.loanzo.app.data.entity.SyncQueueEntity).\n"
                  + " Expected:\n" + _infoSyncQueue + "\n"
                  + " Found:\n" + _existingSyncQueue);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "37313dc5546ef8cd31a7aa1be89ef236", "f8705baaa2204e87f4a22b29a343d35b");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "users","loans","disbursements","repayments","payees","pledges","audit_events","guarantors","sync_queue");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `loans`");
      _db.execSQL("DELETE FROM `disbursements`");
      _db.execSQL("DELETE FROM `repayments`");
      _db.execSQL("DELETE FROM `payees`");
      _db.execSQL("DELETE FROM `pledges`");
      _db.execSQL("DELETE FROM `audit_events`");
      _db.execSQL("DELETE FROM `guarantors`");
      _db.execSQL("DELETE FROM `sync_queue`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LoanDao.class, LoanDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DisbursementDao.class, DisbursementDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RepaymentDao.class, RepaymentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PayeeDao.class, PayeeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PledgeDao.class, PledgeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AuditEventDao.class, AuditEventDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GuarantorDao.class, GuarantorDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SyncQueueDao.class, SyncQueueDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public LoanDao loanDao() {
    if (_loanDao != null) {
      return _loanDao;
    } else {
      synchronized(this) {
        if(_loanDao == null) {
          _loanDao = new LoanDao_Impl(this);
        }
        return _loanDao;
      }
    }
  }

  @Override
  public DisbursementDao disbursementDao() {
    if (_disbursementDao != null) {
      return _disbursementDao;
    } else {
      synchronized(this) {
        if(_disbursementDao == null) {
          _disbursementDao = new DisbursementDao_Impl(this);
        }
        return _disbursementDao;
      }
    }
  }

  @Override
  public RepaymentDao repaymentDao() {
    if (_repaymentDao != null) {
      return _repaymentDao;
    } else {
      synchronized(this) {
        if(_repaymentDao == null) {
          _repaymentDao = new RepaymentDao_Impl(this);
        }
        return _repaymentDao;
      }
    }
  }

  @Override
  public PayeeDao payeeDao() {
    if (_payeeDao != null) {
      return _payeeDao;
    } else {
      synchronized(this) {
        if(_payeeDao == null) {
          _payeeDao = new PayeeDao_Impl(this);
        }
        return _payeeDao;
      }
    }
  }

  @Override
  public PledgeDao pledgeDao() {
    if (_pledgeDao != null) {
      return _pledgeDao;
    } else {
      synchronized(this) {
        if(_pledgeDao == null) {
          _pledgeDao = new PledgeDao_Impl(this);
        }
        return _pledgeDao;
      }
    }
  }

  @Override
  public AuditEventDao auditEventDao() {
    if (_auditEventDao != null) {
      return _auditEventDao;
    } else {
      synchronized(this) {
        if(_auditEventDao == null) {
          _auditEventDao = new AuditEventDao_Impl(this);
        }
        return _auditEventDao;
      }
    }
  }

  @Override
  public GuarantorDao guarantorDao() {
    if (_guarantorDao != null) {
      return _guarantorDao;
    } else {
      synchronized(this) {
        if(_guarantorDao == null) {
          _guarantorDao = new GuarantorDao_Impl(this);
        }
        return _guarantorDao;
      }
    }
  }

  @Override
  public SyncQueueDao syncQueueDao() {
    if (_syncQueueDao != null) {
      return _syncQueueDao;
    } else {
      synchronized(this) {
        if(_syncQueueDao == null) {
          _syncQueueDao = new SyncQueueDao_Impl(this);
        }
        return _syncQueueDao;
      }
    }
  }
}
