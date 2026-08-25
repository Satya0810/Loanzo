package com.loanzo.app.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.loanzo.app.data.entity.LoanEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LoanDao_Impl implements LoanDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LoanEntity> __insertionAdapterOfLoanEntity;

  private final EntityDeletionOrUpdateAdapter<LoanEntity> __deletionAdapterOfLoanEntity;

  private final EntityDeletionOrUpdateAdapter<LoanEntity> __updateAdapterOfLoanEntity;

  public LoanDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLoanEntity = new EntityInsertionAdapter<LoanEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `loans` (`loanId`,`lenderId`,`borrowerId`,`sanctionedAmount`,`disbursedAmount`,`outstandingAmount`,`purpose`,`loanType`,`interestRate`,`interestModel`,`tenureMonths`,`status`,`repaymentFrequency`,`createdAt`,`closedAt`,`notes`,`agreementDocumentId`,`isAgreementSigned`,`agreementUrl`,`penaltyRate`,`penaltyModel`,`originalTenureMonths`,`moratoriumMonths`,`isRestructured`,`restructuredAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LoanEntity entity) {
        statement.bindString(1, entity.getLoanId());
        statement.bindString(2, entity.getLenderId());
        statement.bindString(3, entity.getBorrowerId());
        statement.bindDouble(4, entity.getSanctionedAmount());
        statement.bindDouble(5, entity.getDisbursedAmount());
        statement.bindDouble(6, entity.getOutstandingAmount());
        statement.bindString(7, entity.getPurpose());
        statement.bindString(8, entity.getLoanType());
        statement.bindDouble(9, entity.getInterestRate());
        statement.bindString(10, entity.getInterestModel());
        statement.bindLong(11, entity.getTenureMonths());
        statement.bindString(12, entity.getStatus());
        statement.bindString(13, entity.getRepaymentFrequency());
        statement.bindLong(14, entity.getCreatedAt());
        if (entity.getClosedAt() == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, entity.getClosedAt());
        }
        statement.bindString(16, entity.getNotes());
        if (entity.getAgreementDocumentId() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getAgreementDocumentId());
        }
        final int _tmp = entity.isAgreementSigned() ? 1 : 0;
        statement.bindLong(18, _tmp);
        if (entity.getAgreementUrl() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getAgreementUrl());
        }
        statement.bindDouble(20, entity.getPenaltyRate());
        statement.bindString(21, entity.getPenaltyModel());
        statement.bindLong(22, entity.getOriginalTenureMonths());
        statement.bindLong(23, entity.getMoratoriumMonths());
        final int _tmp_1 = entity.isRestructured() ? 1 : 0;
        statement.bindLong(24, _tmp_1);
        if (entity.getRestructuredAt() == null) {
          statement.bindNull(25);
        } else {
          statement.bindLong(25, entity.getRestructuredAt());
        }
      }
    };
    this.__deletionAdapterOfLoanEntity = new EntityDeletionOrUpdateAdapter<LoanEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `loans` WHERE `loanId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LoanEntity entity) {
        statement.bindString(1, entity.getLoanId());
      }
    };
    this.__updateAdapterOfLoanEntity = new EntityDeletionOrUpdateAdapter<LoanEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `loans` SET `loanId` = ?,`lenderId` = ?,`borrowerId` = ?,`sanctionedAmount` = ?,`disbursedAmount` = ?,`outstandingAmount` = ?,`purpose` = ?,`loanType` = ?,`interestRate` = ?,`interestModel` = ?,`tenureMonths` = ?,`status` = ?,`repaymentFrequency` = ?,`createdAt` = ?,`closedAt` = ?,`notes` = ?,`agreementDocumentId` = ?,`isAgreementSigned` = ?,`agreementUrl` = ?,`penaltyRate` = ?,`penaltyModel` = ?,`originalTenureMonths` = ?,`moratoriumMonths` = ?,`isRestructured` = ?,`restructuredAt` = ? WHERE `loanId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LoanEntity entity) {
        statement.bindString(1, entity.getLoanId());
        statement.bindString(2, entity.getLenderId());
        statement.bindString(3, entity.getBorrowerId());
        statement.bindDouble(4, entity.getSanctionedAmount());
        statement.bindDouble(5, entity.getDisbursedAmount());
        statement.bindDouble(6, entity.getOutstandingAmount());
        statement.bindString(7, entity.getPurpose());
        statement.bindString(8, entity.getLoanType());
        statement.bindDouble(9, entity.getInterestRate());
        statement.bindString(10, entity.getInterestModel());
        statement.bindLong(11, entity.getTenureMonths());
        statement.bindString(12, entity.getStatus());
        statement.bindString(13, entity.getRepaymentFrequency());
        statement.bindLong(14, entity.getCreatedAt());
        if (entity.getClosedAt() == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, entity.getClosedAt());
        }
        statement.bindString(16, entity.getNotes());
        if (entity.getAgreementDocumentId() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getAgreementDocumentId());
        }
        final int _tmp = entity.isAgreementSigned() ? 1 : 0;
        statement.bindLong(18, _tmp);
        if (entity.getAgreementUrl() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getAgreementUrl());
        }
        statement.bindDouble(20, entity.getPenaltyRate());
        statement.bindString(21, entity.getPenaltyModel());
        statement.bindLong(22, entity.getOriginalTenureMonths());
        statement.bindLong(23, entity.getMoratoriumMonths());
        final int _tmp_1 = entity.isRestructured() ? 1 : 0;
        statement.bindLong(24, _tmp_1);
        if (entity.getRestructuredAt() == null) {
          statement.bindNull(25);
        } else {
          statement.bindLong(25, entity.getRestructuredAt());
        }
        statement.bindString(26, entity.getLoanId());
      }
    };
  }

  @Override
  public Object insertLoan(final LoanEntity loan, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLoanEntity.insert(loan);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteLoan(final LoanEntity loan, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfLoanEntity.handle(loan);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLoan(final LoanEntity loan, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLoanEntity.handle(loan);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLoanById(final String loanId,
      final Continuation<? super LoanEntity> $completion) {
    final String _sql = "SELECT * FROM loans WHERE loanId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LoanEntity>() {
      @Override
      @Nullable
      public LoanEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfLenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderId");
          final int _cursorIndexOfBorrowerId = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerId");
          final int _cursorIndexOfSanctionedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "sanctionedAmount");
          final int _cursorIndexOfDisbursedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursedAmount");
          final int _cursorIndexOfOutstandingAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingAmount");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfInterestModel = CursorUtil.getColumnIndexOrThrow(_cursor, "interestModel");
          final int _cursorIndexOfTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "tenureMonths");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRepaymentFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentFrequency");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAgreementDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementDocumentId");
          final int _cursorIndexOfIsAgreementSigned = CursorUtil.getColumnIndexOrThrow(_cursor, "isAgreementSigned");
          final int _cursorIndexOfAgreementUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementUrl");
          final int _cursorIndexOfPenaltyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyRate");
          final int _cursorIndexOfPenaltyModel = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyModel");
          final int _cursorIndexOfOriginalTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTenureMonths");
          final int _cursorIndexOfMoratoriumMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "moratoriumMonths");
          final int _cursorIndexOfIsRestructured = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestructured");
          final int _cursorIndexOfRestructuredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "restructuredAt");
          final LoanEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpLenderId;
            _tmpLenderId = _cursor.getString(_cursorIndexOfLenderId);
            final String _tmpBorrowerId;
            _tmpBorrowerId = _cursor.getString(_cursorIndexOfBorrowerId);
            final double _tmpSanctionedAmount;
            _tmpSanctionedAmount = _cursor.getDouble(_cursorIndexOfSanctionedAmount);
            final double _tmpDisbursedAmount;
            _tmpDisbursedAmount = _cursor.getDouble(_cursorIndexOfDisbursedAmount);
            final double _tmpOutstandingAmount;
            _tmpOutstandingAmount = _cursor.getDouble(_cursorIndexOfOutstandingAmount);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpLoanType;
            _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            final double _tmpInterestRate;
            _tmpInterestRate = _cursor.getDouble(_cursorIndexOfInterestRate);
            final String _tmpInterestModel;
            _tmpInterestModel = _cursor.getString(_cursorIndexOfInterestModel);
            final int _tmpTenureMonths;
            _tmpTenureMonths = _cursor.getInt(_cursorIndexOfTenureMonths);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpRepaymentFrequency;
            _tmpRepaymentFrequency = _cursor.getString(_cursorIndexOfRepaymentFrequency);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAgreementDocumentId;
            if (_cursor.isNull(_cursorIndexOfAgreementDocumentId)) {
              _tmpAgreementDocumentId = null;
            } else {
              _tmpAgreementDocumentId = _cursor.getString(_cursorIndexOfAgreementDocumentId);
            }
            final boolean _tmpIsAgreementSigned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAgreementSigned);
            _tmpIsAgreementSigned = _tmp != 0;
            final String _tmpAgreementUrl;
            if (_cursor.isNull(_cursorIndexOfAgreementUrl)) {
              _tmpAgreementUrl = null;
            } else {
              _tmpAgreementUrl = _cursor.getString(_cursorIndexOfAgreementUrl);
            }
            final double _tmpPenaltyRate;
            _tmpPenaltyRate = _cursor.getDouble(_cursorIndexOfPenaltyRate);
            final String _tmpPenaltyModel;
            _tmpPenaltyModel = _cursor.getString(_cursorIndexOfPenaltyModel);
            final int _tmpOriginalTenureMonths;
            _tmpOriginalTenureMonths = _cursor.getInt(_cursorIndexOfOriginalTenureMonths);
            final int _tmpMoratoriumMonths;
            _tmpMoratoriumMonths = _cursor.getInt(_cursorIndexOfMoratoriumMonths);
            final boolean _tmpIsRestructured;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsRestructured);
            _tmpIsRestructured = _tmp_1 != 0;
            final Long _tmpRestructuredAt;
            if (_cursor.isNull(_cursorIndexOfRestructuredAt)) {
              _tmpRestructuredAt = null;
            } else {
              _tmpRestructuredAt = _cursor.getLong(_cursorIndexOfRestructuredAt);
            }
            _result = new LoanEntity(_tmpLoanId,_tmpLenderId,_tmpBorrowerId,_tmpSanctionedAmount,_tmpDisbursedAmount,_tmpOutstandingAmount,_tmpPurpose,_tmpLoanType,_tmpInterestRate,_tmpInterestModel,_tmpTenureMonths,_tmpStatus,_tmpRepaymentFrequency,_tmpCreatedAt,_tmpClosedAt,_tmpNotes,_tmpAgreementDocumentId,_tmpIsAgreementSigned,_tmpAgreementUrl,_tmpPenaltyRate,_tmpPenaltyModel,_tmpOriginalTenureMonths,_tmpMoratoriumMonths,_tmpIsRestructured,_tmpRestructuredAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<LoanEntity> observeLoan(final String loanId) {
    final String _sql = "SELECT * FROM loans WHERE loanId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<LoanEntity>() {
      @Override
      @Nullable
      public LoanEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfLenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderId");
          final int _cursorIndexOfBorrowerId = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerId");
          final int _cursorIndexOfSanctionedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "sanctionedAmount");
          final int _cursorIndexOfDisbursedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursedAmount");
          final int _cursorIndexOfOutstandingAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingAmount");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfInterestModel = CursorUtil.getColumnIndexOrThrow(_cursor, "interestModel");
          final int _cursorIndexOfTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "tenureMonths");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRepaymentFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentFrequency");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAgreementDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementDocumentId");
          final int _cursorIndexOfIsAgreementSigned = CursorUtil.getColumnIndexOrThrow(_cursor, "isAgreementSigned");
          final int _cursorIndexOfAgreementUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementUrl");
          final int _cursorIndexOfPenaltyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyRate");
          final int _cursorIndexOfPenaltyModel = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyModel");
          final int _cursorIndexOfOriginalTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTenureMonths");
          final int _cursorIndexOfMoratoriumMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "moratoriumMonths");
          final int _cursorIndexOfIsRestructured = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestructured");
          final int _cursorIndexOfRestructuredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "restructuredAt");
          final LoanEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpLenderId;
            _tmpLenderId = _cursor.getString(_cursorIndexOfLenderId);
            final String _tmpBorrowerId;
            _tmpBorrowerId = _cursor.getString(_cursorIndexOfBorrowerId);
            final double _tmpSanctionedAmount;
            _tmpSanctionedAmount = _cursor.getDouble(_cursorIndexOfSanctionedAmount);
            final double _tmpDisbursedAmount;
            _tmpDisbursedAmount = _cursor.getDouble(_cursorIndexOfDisbursedAmount);
            final double _tmpOutstandingAmount;
            _tmpOutstandingAmount = _cursor.getDouble(_cursorIndexOfOutstandingAmount);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpLoanType;
            _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            final double _tmpInterestRate;
            _tmpInterestRate = _cursor.getDouble(_cursorIndexOfInterestRate);
            final String _tmpInterestModel;
            _tmpInterestModel = _cursor.getString(_cursorIndexOfInterestModel);
            final int _tmpTenureMonths;
            _tmpTenureMonths = _cursor.getInt(_cursorIndexOfTenureMonths);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpRepaymentFrequency;
            _tmpRepaymentFrequency = _cursor.getString(_cursorIndexOfRepaymentFrequency);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAgreementDocumentId;
            if (_cursor.isNull(_cursorIndexOfAgreementDocumentId)) {
              _tmpAgreementDocumentId = null;
            } else {
              _tmpAgreementDocumentId = _cursor.getString(_cursorIndexOfAgreementDocumentId);
            }
            final boolean _tmpIsAgreementSigned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAgreementSigned);
            _tmpIsAgreementSigned = _tmp != 0;
            final String _tmpAgreementUrl;
            if (_cursor.isNull(_cursorIndexOfAgreementUrl)) {
              _tmpAgreementUrl = null;
            } else {
              _tmpAgreementUrl = _cursor.getString(_cursorIndexOfAgreementUrl);
            }
            final double _tmpPenaltyRate;
            _tmpPenaltyRate = _cursor.getDouble(_cursorIndexOfPenaltyRate);
            final String _tmpPenaltyModel;
            _tmpPenaltyModel = _cursor.getString(_cursorIndexOfPenaltyModel);
            final int _tmpOriginalTenureMonths;
            _tmpOriginalTenureMonths = _cursor.getInt(_cursorIndexOfOriginalTenureMonths);
            final int _tmpMoratoriumMonths;
            _tmpMoratoriumMonths = _cursor.getInt(_cursorIndexOfMoratoriumMonths);
            final boolean _tmpIsRestructured;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsRestructured);
            _tmpIsRestructured = _tmp_1 != 0;
            final Long _tmpRestructuredAt;
            if (_cursor.isNull(_cursorIndexOfRestructuredAt)) {
              _tmpRestructuredAt = null;
            } else {
              _tmpRestructuredAt = _cursor.getLong(_cursorIndexOfRestructuredAt);
            }
            _result = new LoanEntity(_tmpLoanId,_tmpLenderId,_tmpBorrowerId,_tmpSanctionedAmount,_tmpDisbursedAmount,_tmpOutstandingAmount,_tmpPurpose,_tmpLoanType,_tmpInterestRate,_tmpInterestModel,_tmpTenureMonths,_tmpStatus,_tmpRepaymentFrequency,_tmpCreatedAt,_tmpClosedAt,_tmpNotes,_tmpAgreementDocumentId,_tmpIsAgreementSigned,_tmpAgreementUrl,_tmpPenaltyRate,_tmpPenaltyModel,_tmpOriginalTenureMonths,_tmpMoratoriumMonths,_tmpIsRestructured,_tmpRestructuredAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LoanEntity>> getLoansByBorrower(final String userId) {
    final String _sql = "SELECT * FROM loans WHERE borrowerId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<List<LoanEntity>>() {
      @Override
      @NonNull
      public List<LoanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfLenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderId");
          final int _cursorIndexOfBorrowerId = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerId");
          final int _cursorIndexOfSanctionedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "sanctionedAmount");
          final int _cursorIndexOfDisbursedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursedAmount");
          final int _cursorIndexOfOutstandingAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingAmount");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfInterestModel = CursorUtil.getColumnIndexOrThrow(_cursor, "interestModel");
          final int _cursorIndexOfTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "tenureMonths");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRepaymentFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentFrequency");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAgreementDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementDocumentId");
          final int _cursorIndexOfIsAgreementSigned = CursorUtil.getColumnIndexOrThrow(_cursor, "isAgreementSigned");
          final int _cursorIndexOfAgreementUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementUrl");
          final int _cursorIndexOfPenaltyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyRate");
          final int _cursorIndexOfPenaltyModel = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyModel");
          final int _cursorIndexOfOriginalTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTenureMonths");
          final int _cursorIndexOfMoratoriumMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "moratoriumMonths");
          final int _cursorIndexOfIsRestructured = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestructured");
          final int _cursorIndexOfRestructuredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "restructuredAt");
          final List<LoanEntity> _result = new ArrayList<LoanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LoanEntity _item;
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpLenderId;
            _tmpLenderId = _cursor.getString(_cursorIndexOfLenderId);
            final String _tmpBorrowerId;
            _tmpBorrowerId = _cursor.getString(_cursorIndexOfBorrowerId);
            final double _tmpSanctionedAmount;
            _tmpSanctionedAmount = _cursor.getDouble(_cursorIndexOfSanctionedAmount);
            final double _tmpDisbursedAmount;
            _tmpDisbursedAmount = _cursor.getDouble(_cursorIndexOfDisbursedAmount);
            final double _tmpOutstandingAmount;
            _tmpOutstandingAmount = _cursor.getDouble(_cursorIndexOfOutstandingAmount);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpLoanType;
            _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            final double _tmpInterestRate;
            _tmpInterestRate = _cursor.getDouble(_cursorIndexOfInterestRate);
            final String _tmpInterestModel;
            _tmpInterestModel = _cursor.getString(_cursorIndexOfInterestModel);
            final int _tmpTenureMonths;
            _tmpTenureMonths = _cursor.getInt(_cursorIndexOfTenureMonths);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpRepaymentFrequency;
            _tmpRepaymentFrequency = _cursor.getString(_cursorIndexOfRepaymentFrequency);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAgreementDocumentId;
            if (_cursor.isNull(_cursorIndexOfAgreementDocumentId)) {
              _tmpAgreementDocumentId = null;
            } else {
              _tmpAgreementDocumentId = _cursor.getString(_cursorIndexOfAgreementDocumentId);
            }
            final boolean _tmpIsAgreementSigned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAgreementSigned);
            _tmpIsAgreementSigned = _tmp != 0;
            final String _tmpAgreementUrl;
            if (_cursor.isNull(_cursorIndexOfAgreementUrl)) {
              _tmpAgreementUrl = null;
            } else {
              _tmpAgreementUrl = _cursor.getString(_cursorIndexOfAgreementUrl);
            }
            final double _tmpPenaltyRate;
            _tmpPenaltyRate = _cursor.getDouble(_cursorIndexOfPenaltyRate);
            final String _tmpPenaltyModel;
            _tmpPenaltyModel = _cursor.getString(_cursorIndexOfPenaltyModel);
            final int _tmpOriginalTenureMonths;
            _tmpOriginalTenureMonths = _cursor.getInt(_cursorIndexOfOriginalTenureMonths);
            final int _tmpMoratoriumMonths;
            _tmpMoratoriumMonths = _cursor.getInt(_cursorIndexOfMoratoriumMonths);
            final boolean _tmpIsRestructured;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsRestructured);
            _tmpIsRestructured = _tmp_1 != 0;
            final Long _tmpRestructuredAt;
            if (_cursor.isNull(_cursorIndexOfRestructuredAt)) {
              _tmpRestructuredAt = null;
            } else {
              _tmpRestructuredAt = _cursor.getLong(_cursorIndexOfRestructuredAt);
            }
            _item = new LoanEntity(_tmpLoanId,_tmpLenderId,_tmpBorrowerId,_tmpSanctionedAmount,_tmpDisbursedAmount,_tmpOutstandingAmount,_tmpPurpose,_tmpLoanType,_tmpInterestRate,_tmpInterestModel,_tmpTenureMonths,_tmpStatus,_tmpRepaymentFrequency,_tmpCreatedAt,_tmpClosedAt,_tmpNotes,_tmpAgreementDocumentId,_tmpIsAgreementSigned,_tmpAgreementUrl,_tmpPenaltyRate,_tmpPenaltyModel,_tmpOriginalTenureMonths,_tmpMoratoriumMonths,_tmpIsRestructured,_tmpRestructuredAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LoanEntity>> getLoansByLender(final String userId) {
    final String _sql = "SELECT * FROM loans WHERE lenderId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<List<LoanEntity>>() {
      @Override
      @NonNull
      public List<LoanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfLenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderId");
          final int _cursorIndexOfBorrowerId = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerId");
          final int _cursorIndexOfSanctionedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "sanctionedAmount");
          final int _cursorIndexOfDisbursedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursedAmount");
          final int _cursorIndexOfOutstandingAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingAmount");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfInterestModel = CursorUtil.getColumnIndexOrThrow(_cursor, "interestModel");
          final int _cursorIndexOfTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "tenureMonths");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRepaymentFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentFrequency");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAgreementDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementDocumentId");
          final int _cursorIndexOfIsAgreementSigned = CursorUtil.getColumnIndexOrThrow(_cursor, "isAgreementSigned");
          final int _cursorIndexOfAgreementUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementUrl");
          final int _cursorIndexOfPenaltyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyRate");
          final int _cursorIndexOfPenaltyModel = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyModel");
          final int _cursorIndexOfOriginalTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTenureMonths");
          final int _cursorIndexOfMoratoriumMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "moratoriumMonths");
          final int _cursorIndexOfIsRestructured = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestructured");
          final int _cursorIndexOfRestructuredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "restructuredAt");
          final List<LoanEntity> _result = new ArrayList<LoanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LoanEntity _item;
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpLenderId;
            _tmpLenderId = _cursor.getString(_cursorIndexOfLenderId);
            final String _tmpBorrowerId;
            _tmpBorrowerId = _cursor.getString(_cursorIndexOfBorrowerId);
            final double _tmpSanctionedAmount;
            _tmpSanctionedAmount = _cursor.getDouble(_cursorIndexOfSanctionedAmount);
            final double _tmpDisbursedAmount;
            _tmpDisbursedAmount = _cursor.getDouble(_cursorIndexOfDisbursedAmount);
            final double _tmpOutstandingAmount;
            _tmpOutstandingAmount = _cursor.getDouble(_cursorIndexOfOutstandingAmount);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpLoanType;
            _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            final double _tmpInterestRate;
            _tmpInterestRate = _cursor.getDouble(_cursorIndexOfInterestRate);
            final String _tmpInterestModel;
            _tmpInterestModel = _cursor.getString(_cursorIndexOfInterestModel);
            final int _tmpTenureMonths;
            _tmpTenureMonths = _cursor.getInt(_cursorIndexOfTenureMonths);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpRepaymentFrequency;
            _tmpRepaymentFrequency = _cursor.getString(_cursorIndexOfRepaymentFrequency);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAgreementDocumentId;
            if (_cursor.isNull(_cursorIndexOfAgreementDocumentId)) {
              _tmpAgreementDocumentId = null;
            } else {
              _tmpAgreementDocumentId = _cursor.getString(_cursorIndexOfAgreementDocumentId);
            }
            final boolean _tmpIsAgreementSigned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAgreementSigned);
            _tmpIsAgreementSigned = _tmp != 0;
            final String _tmpAgreementUrl;
            if (_cursor.isNull(_cursorIndexOfAgreementUrl)) {
              _tmpAgreementUrl = null;
            } else {
              _tmpAgreementUrl = _cursor.getString(_cursorIndexOfAgreementUrl);
            }
            final double _tmpPenaltyRate;
            _tmpPenaltyRate = _cursor.getDouble(_cursorIndexOfPenaltyRate);
            final String _tmpPenaltyModel;
            _tmpPenaltyModel = _cursor.getString(_cursorIndexOfPenaltyModel);
            final int _tmpOriginalTenureMonths;
            _tmpOriginalTenureMonths = _cursor.getInt(_cursorIndexOfOriginalTenureMonths);
            final int _tmpMoratoriumMonths;
            _tmpMoratoriumMonths = _cursor.getInt(_cursorIndexOfMoratoriumMonths);
            final boolean _tmpIsRestructured;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsRestructured);
            _tmpIsRestructured = _tmp_1 != 0;
            final Long _tmpRestructuredAt;
            if (_cursor.isNull(_cursorIndexOfRestructuredAt)) {
              _tmpRestructuredAt = null;
            } else {
              _tmpRestructuredAt = _cursor.getLong(_cursorIndexOfRestructuredAt);
            }
            _item = new LoanEntity(_tmpLoanId,_tmpLenderId,_tmpBorrowerId,_tmpSanctionedAmount,_tmpDisbursedAmount,_tmpOutstandingAmount,_tmpPurpose,_tmpLoanType,_tmpInterestRate,_tmpInterestModel,_tmpTenureMonths,_tmpStatus,_tmpRepaymentFrequency,_tmpCreatedAt,_tmpClosedAt,_tmpNotes,_tmpAgreementDocumentId,_tmpIsAgreementSigned,_tmpAgreementUrl,_tmpPenaltyRate,_tmpPenaltyModel,_tmpOriginalTenureMonths,_tmpMoratoriumMonths,_tmpIsRestructured,_tmpRestructuredAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LoanEntity>> getAllLoansForUser(final String userId) {
    final String _sql = "SELECT * FROM loans WHERE (borrowerId = ? OR lenderId = ?) ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<List<LoanEntity>>() {
      @Override
      @NonNull
      public List<LoanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfLenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderId");
          final int _cursorIndexOfBorrowerId = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerId");
          final int _cursorIndexOfSanctionedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "sanctionedAmount");
          final int _cursorIndexOfDisbursedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursedAmount");
          final int _cursorIndexOfOutstandingAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingAmount");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfInterestModel = CursorUtil.getColumnIndexOrThrow(_cursor, "interestModel");
          final int _cursorIndexOfTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "tenureMonths");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRepaymentFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentFrequency");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAgreementDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementDocumentId");
          final int _cursorIndexOfIsAgreementSigned = CursorUtil.getColumnIndexOrThrow(_cursor, "isAgreementSigned");
          final int _cursorIndexOfAgreementUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementUrl");
          final int _cursorIndexOfPenaltyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyRate");
          final int _cursorIndexOfPenaltyModel = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyModel");
          final int _cursorIndexOfOriginalTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTenureMonths");
          final int _cursorIndexOfMoratoriumMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "moratoriumMonths");
          final int _cursorIndexOfIsRestructured = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestructured");
          final int _cursorIndexOfRestructuredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "restructuredAt");
          final List<LoanEntity> _result = new ArrayList<LoanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LoanEntity _item;
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpLenderId;
            _tmpLenderId = _cursor.getString(_cursorIndexOfLenderId);
            final String _tmpBorrowerId;
            _tmpBorrowerId = _cursor.getString(_cursorIndexOfBorrowerId);
            final double _tmpSanctionedAmount;
            _tmpSanctionedAmount = _cursor.getDouble(_cursorIndexOfSanctionedAmount);
            final double _tmpDisbursedAmount;
            _tmpDisbursedAmount = _cursor.getDouble(_cursorIndexOfDisbursedAmount);
            final double _tmpOutstandingAmount;
            _tmpOutstandingAmount = _cursor.getDouble(_cursorIndexOfOutstandingAmount);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpLoanType;
            _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            final double _tmpInterestRate;
            _tmpInterestRate = _cursor.getDouble(_cursorIndexOfInterestRate);
            final String _tmpInterestModel;
            _tmpInterestModel = _cursor.getString(_cursorIndexOfInterestModel);
            final int _tmpTenureMonths;
            _tmpTenureMonths = _cursor.getInt(_cursorIndexOfTenureMonths);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpRepaymentFrequency;
            _tmpRepaymentFrequency = _cursor.getString(_cursorIndexOfRepaymentFrequency);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAgreementDocumentId;
            if (_cursor.isNull(_cursorIndexOfAgreementDocumentId)) {
              _tmpAgreementDocumentId = null;
            } else {
              _tmpAgreementDocumentId = _cursor.getString(_cursorIndexOfAgreementDocumentId);
            }
            final boolean _tmpIsAgreementSigned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAgreementSigned);
            _tmpIsAgreementSigned = _tmp != 0;
            final String _tmpAgreementUrl;
            if (_cursor.isNull(_cursorIndexOfAgreementUrl)) {
              _tmpAgreementUrl = null;
            } else {
              _tmpAgreementUrl = _cursor.getString(_cursorIndexOfAgreementUrl);
            }
            final double _tmpPenaltyRate;
            _tmpPenaltyRate = _cursor.getDouble(_cursorIndexOfPenaltyRate);
            final String _tmpPenaltyModel;
            _tmpPenaltyModel = _cursor.getString(_cursorIndexOfPenaltyModel);
            final int _tmpOriginalTenureMonths;
            _tmpOriginalTenureMonths = _cursor.getInt(_cursorIndexOfOriginalTenureMonths);
            final int _tmpMoratoriumMonths;
            _tmpMoratoriumMonths = _cursor.getInt(_cursorIndexOfMoratoriumMonths);
            final boolean _tmpIsRestructured;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsRestructured);
            _tmpIsRestructured = _tmp_1 != 0;
            final Long _tmpRestructuredAt;
            if (_cursor.isNull(_cursorIndexOfRestructuredAt)) {
              _tmpRestructuredAt = null;
            } else {
              _tmpRestructuredAt = _cursor.getLong(_cursorIndexOfRestructuredAt);
            }
            _item = new LoanEntity(_tmpLoanId,_tmpLenderId,_tmpBorrowerId,_tmpSanctionedAmount,_tmpDisbursedAmount,_tmpOutstandingAmount,_tmpPurpose,_tmpLoanType,_tmpInterestRate,_tmpInterestModel,_tmpTenureMonths,_tmpStatus,_tmpRepaymentFrequency,_tmpCreatedAt,_tmpClosedAt,_tmpNotes,_tmpAgreementDocumentId,_tmpIsAgreementSigned,_tmpAgreementUrl,_tmpPenaltyRate,_tmpPenaltyModel,_tmpOriginalTenureMonths,_tmpMoratoriumMonths,_tmpIsRestructured,_tmpRestructuredAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LoanEntity>> getLoansByStatus(final String status) {
    final String _sql = "SELECT * FROM loans WHERE status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, status);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<List<LoanEntity>>() {
      @Override
      @NonNull
      public List<LoanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfLenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderId");
          final int _cursorIndexOfBorrowerId = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerId");
          final int _cursorIndexOfSanctionedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "sanctionedAmount");
          final int _cursorIndexOfDisbursedAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursedAmount");
          final int _cursorIndexOfOutstandingAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingAmount");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfInterestModel = CursorUtil.getColumnIndexOrThrow(_cursor, "interestModel");
          final int _cursorIndexOfTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "tenureMonths");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRepaymentFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentFrequency");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAgreementDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementDocumentId");
          final int _cursorIndexOfIsAgreementSigned = CursorUtil.getColumnIndexOrThrow(_cursor, "isAgreementSigned");
          final int _cursorIndexOfAgreementUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementUrl");
          final int _cursorIndexOfPenaltyRate = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyRate");
          final int _cursorIndexOfPenaltyModel = CursorUtil.getColumnIndexOrThrow(_cursor, "penaltyModel");
          final int _cursorIndexOfOriginalTenureMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "originalTenureMonths");
          final int _cursorIndexOfMoratoriumMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "moratoriumMonths");
          final int _cursorIndexOfIsRestructured = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestructured");
          final int _cursorIndexOfRestructuredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "restructuredAt");
          final List<LoanEntity> _result = new ArrayList<LoanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LoanEntity _item;
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpLenderId;
            _tmpLenderId = _cursor.getString(_cursorIndexOfLenderId);
            final String _tmpBorrowerId;
            _tmpBorrowerId = _cursor.getString(_cursorIndexOfBorrowerId);
            final double _tmpSanctionedAmount;
            _tmpSanctionedAmount = _cursor.getDouble(_cursorIndexOfSanctionedAmount);
            final double _tmpDisbursedAmount;
            _tmpDisbursedAmount = _cursor.getDouble(_cursorIndexOfDisbursedAmount);
            final double _tmpOutstandingAmount;
            _tmpOutstandingAmount = _cursor.getDouble(_cursorIndexOfOutstandingAmount);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpLoanType;
            _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            final double _tmpInterestRate;
            _tmpInterestRate = _cursor.getDouble(_cursorIndexOfInterestRate);
            final String _tmpInterestModel;
            _tmpInterestModel = _cursor.getString(_cursorIndexOfInterestModel);
            final int _tmpTenureMonths;
            _tmpTenureMonths = _cursor.getInt(_cursorIndexOfTenureMonths);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpRepaymentFrequency;
            _tmpRepaymentFrequency = _cursor.getString(_cursorIndexOfRepaymentFrequency);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAgreementDocumentId;
            if (_cursor.isNull(_cursorIndexOfAgreementDocumentId)) {
              _tmpAgreementDocumentId = null;
            } else {
              _tmpAgreementDocumentId = _cursor.getString(_cursorIndexOfAgreementDocumentId);
            }
            final boolean _tmpIsAgreementSigned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAgreementSigned);
            _tmpIsAgreementSigned = _tmp != 0;
            final String _tmpAgreementUrl;
            if (_cursor.isNull(_cursorIndexOfAgreementUrl)) {
              _tmpAgreementUrl = null;
            } else {
              _tmpAgreementUrl = _cursor.getString(_cursorIndexOfAgreementUrl);
            }
            final double _tmpPenaltyRate;
            _tmpPenaltyRate = _cursor.getDouble(_cursorIndexOfPenaltyRate);
            final String _tmpPenaltyModel;
            _tmpPenaltyModel = _cursor.getString(_cursorIndexOfPenaltyModel);
            final int _tmpOriginalTenureMonths;
            _tmpOriginalTenureMonths = _cursor.getInt(_cursorIndexOfOriginalTenureMonths);
            final int _tmpMoratoriumMonths;
            _tmpMoratoriumMonths = _cursor.getInt(_cursorIndexOfMoratoriumMonths);
            final boolean _tmpIsRestructured;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsRestructured);
            _tmpIsRestructured = _tmp_1 != 0;
            final Long _tmpRestructuredAt;
            if (_cursor.isNull(_cursorIndexOfRestructuredAt)) {
              _tmpRestructuredAt = null;
            } else {
              _tmpRestructuredAt = _cursor.getLong(_cursorIndexOfRestructuredAt);
            }
            _item = new LoanEntity(_tmpLoanId,_tmpLenderId,_tmpBorrowerId,_tmpSanctionedAmount,_tmpDisbursedAmount,_tmpOutstandingAmount,_tmpPurpose,_tmpLoanType,_tmpInterestRate,_tmpInterestModel,_tmpTenureMonths,_tmpStatus,_tmpRepaymentFrequency,_tmpCreatedAt,_tmpClosedAt,_tmpNotes,_tmpAgreementDocumentId,_tmpIsAgreementSigned,_tmpAgreementUrl,_tmpPenaltyRate,_tmpPenaltyModel,_tmpOriginalTenureMonths,_tmpMoratoriumMonths,_tmpIsRestructured,_tmpRestructuredAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Double> getTotalDisbursedForBorrower(final String userId) {
    final String _sql = "SELECT SUM(disbursedAmount) FROM loans WHERE borrowerId = ? AND status = 'ACTIVE'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Double> getTotalOutstandingForBorrower(final String userId) {
    final String _sql = "SELECT SUM(outstandingAmount) FROM loans WHERE borrowerId = ? AND status = 'ACTIVE'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
