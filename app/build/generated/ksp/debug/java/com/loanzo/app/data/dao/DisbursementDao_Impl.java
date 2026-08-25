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
import com.loanzo.app.data.entity.DisbursementEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
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
public final class DisbursementDao_Impl implements DisbursementDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DisbursementEntity> __insertionAdapterOfDisbursementEntity;

  private final EntityDeletionOrUpdateAdapter<DisbursementEntity> __deletionAdapterOfDisbursementEntity;

  private final EntityDeletionOrUpdateAdapter<DisbursementEntity> __updateAdapterOfDisbursementEntity;

  public DisbursementDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDisbursementEntity = new EntityInsertionAdapter<DisbursementEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `disbursements` (`disbursementId`,`loanId`,`amount`,`payeeId`,`payeeName`,`purpose`,`purposeCategory`,`verificationStatus`,`ruleEngineResult`,`approvalStatus`,`transactionRef`,`upiDeepLink`,`timestamp`,`lenderNote`,`borrowerNote`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DisbursementEntity entity) {
        statement.bindString(1, entity.getDisbursementId());
        statement.bindString(2, entity.getLoanId());
        statement.bindDouble(3, entity.getAmount());
        if (entity.getPayeeId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPayeeId());
        }
        statement.bindString(5, entity.getPayeeName());
        statement.bindString(6, entity.getPurpose());
        statement.bindString(7, entity.getPurposeCategory());
        statement.bindString(8, entity.getVerificationStatus());
        statement.bindString(9, entity.getRuleEngineResult());
        statement.bindString(10, entity.getApprovalStatus());
        statement.bindString(11, entity.getTransactionRef());
        statement.bindString(12, entity.getUpiDeepLink());
        statement.bindLong(13, entity.getTimestamp());
        statement.bindString(14, entity.getLenderNote());
        statement.bindString(15, entity.getBorrowerNote());
      }
    };
    this.__deletionAdapterOfDisbursementEntity = new EntityDeletionOrUpdateAdapter<DisbursementEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `disbursements` WHERE `disbursementId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DisbursementEntity entity) {
        statement.bindString(1, entity.getDisbursementId());
      }
    };
    this.__updateAdapterOfDisbursementEntity = new EntityDeletionOrUpdateAdapter<DisbursementEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `disbursements` SET `disbursementId` = ?,`loanId` = ?,`amount` = ?,`payeeId` = ?,`payeeName` = ?,`purpose` = ?,`purposeCategory` = ?,`verificationStatus` = ?,`ruleEngineResult` = ?,`approvalStatus` = ?,`transactionRef` = ?,`upiDeepLink` = ?,`timestamp` = ?,`lenderNote` = ?,`borrowerNote` = ? WHERE `disbursementId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DisbursementEntity entity) {
        statement.bindString(1, entity.getDisbursementId());
        statement.bindString(2, entity.getLoanId());
        statement.bindDouble(3, entity.getAmount());
        if (entity.getPayeeId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPayeeId());
        }
        statement.bindString(5, entity.getPayeeName());
        statement.bindString(6, entity.getPurpose());
        statement.bindString(7, entity.getPurposeCategory());
        statement.bindString(8, entity.getVerificationStatus());
        statement.bindString(9, entity.getRuleEngineResult());
        statement.bindString(10, entity.getApprovalStatus());
        statement.bindString(11, entity.getTransactionRef());
        statement.bindString(12, entity.getUpiDeepLink());
        statement.bindLong(13, entity.getTimestamp());
        statement.bindString(14, entity.getLenderNote());
        statement.bindString(15, entity.getBorrowerNote());
        statement.bindString(16, entity.getDisbursementId());
      }
    };
  }

  @Override
  public Object insertDisbursement(final DisbursementEntity disbursement,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDisbursementEntity.insert(disbursement);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDisbursement(final DisbursementEntity disbursement,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDisbursementEntity.handle(disbursement);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDisbursement(final DisbursementEntity disbursement,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDisbursementEntity.handle(disbursement);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDisbursementById(final String id,
      final Continuation<? super DisbursementEntity> $completion) {
    final String _sql = "SELECT * FROM disbursements WHERE disbursementId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DisbursementEntity>() {
      @Override
      @Nullable
      public DisbursementEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDisbursementId = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursementId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfPayeeName = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeName");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfPurposeCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "purposeCategory");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfRuleEngineResult = CursorUtil.getColumnIndexOrThrow(_cursor, "ruleEngineResult");
          final int _cursorIndexOfApprovalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "approvalStatus");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfUpiDeepLink = CursorUtil.getColumnIndexOrThrow(_cursor, "upiDeepLink");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLenderNote = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderNote");
          final int _cursorIndexOfBorrowerNote = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerNote");
          final DisbursementEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDisbursementId;
            _tmpDisbursementId = _cursor.getString(_cursorIndexOfDisbursementId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpPayeeId;
            if (_cursor.isNull(_cursorIndexOfPayeeId)) {
              _tmpPayeeId = null;
            } else {
              _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            }
            final String _tmpPayeeName;
            _tmpPayeeName = _cursor.getString(_cursorIndexOfPayeeName);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpPurposeCategory;
            _tmpPurposeCategory = _cursor.getString(_cursorIndexOfPurposeCategory);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpRuleEngineResult;
            _tmpRuleEngineResult = _cursor.getString(_cursorIndexOfRuleEngineResult);
            final String _tmpApprovalStatus;
            _tmpApprovalStatus = _cursor.getString(_cursorIndexOfApprovalStatus);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpUpiDeepLink;
            _tmpUpiDeepLink = _cursor.getString(_cursorIndexOfUpiDeepLink);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpLenderNote;
            _tmpLenderNote = _cursor.getString(_cursorIndexOfLenderNote);
            final String _tmpBorrowerNote;
            _tmpBorrowerNote = _cursor.getString(_cursorIndexOfBorrowerNote);
            _result = new DisbursementEntity(_tmpDisbursementId,_tmpLoanId,_tmpAmount,_tmpPayeeId,_tmpPayeeName,_tmpPurpose,_tmpPurposeCategory,_tmpVerificationStatus,_tmpRuleEngineResult,_tmpApprovalStatus,_tmpTransactionRef,_tmpUpiDeepLink,_tmpTimestamp,_tmpLenderNote,_tmpBorrowerNote);
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
  public Flow<List<DisbursementEntity>> getDisbursementsByLoan(final String loanId) {
    final String _sql = "SELECT * FROM disbursements WHERE loanId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"disbursements"}, new Callable<List<DisbursementEntity>>() {
      @Override
      @NonNull
      public List<DisbursementEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDisbursementId = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursementId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfPayeeName = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeName");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfPurposeCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "purposeCategory");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfRuleEngineResult = CursorUtil.getColumnIndexOrThrow(_cursor, "ruleEngineResult");
          final int _cursorIndexOfApprovalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "approvalStatus");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfUpiDeepLink = CursorUtil.getColumnIndexOrThrow(_cursor, "upiDeepLink");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLenderNote = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderNote");
          final int _cursorIndexOfBorrowerNote = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerNote");
          final List<DisbursementEntity> _result = new ArrayList<DisbursementEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DisbursementEntity _item;
            final String _tmpDisbursementId;
            _tmpDisbursementId = _cursor.getString(_cursorIndexOfDisbursementId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpPayeeId;
            if (_cursor.isNull(_cursorIndexOfPayeeId)) {
              _tmpPayeeId = null;
            } else {
              _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            }
            final String _tmpPayeeName;
            _tmpPayeeName = _cursor.getString(_cursorIndexOfPayeeName);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpPurposeCategory;
            _tmpPurposeCategory = _cursor.getString(_cursorIndexOfPurposeCategory);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpRuleEngineResult;
            _tmpRuleEngineResult = _cursor.getString(_cursorIndexOfRuleEngineResult);
            final String _tmpApprovalStatus;
            _tmpApprovalStatus = _cursor.getString(_cursorIndexOfApprovalStatus);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpUpiDeepLink;
            _tmpUpiDeepLink = _cursor.getString(_cursorIndexOfUpiDeepLink);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpLenderNote;
            _tmpLenderNote = _cursor.getString(_cursorIndexOfLenderNote);
            final String _tmpBorrowerNote;
            _tmpBorrowerNote = _cursor.getString(_cursorIndexOfBorrowerNote);
            _item = new DisbursementEntity(_tmpDisbursementId,_tmpLoanId,_tmpAmount,_tmpPayeeId,_tmpPayeeName,_tmpPurpose,_tmpPurposeCategory,_tmpVerificationStatus,_tmpRuleEngineResult,_tmpApprovalStatus,_tmpTransactionRef,_tmpUpiDeepLink,_tmpTimestamp,_tmpLenderNote,_tmpBorrowerNote);
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
  public Flow<List<DisbursementEntity>> getVerifiedDisbursements(final String loanId) {
    final String _sql = "SELECT * FROM disbursements WHERE loanId = ? AND verificationStatus = 'VERIFIED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"disbursements"}, new Callable<List<DisbursementEntity>>() {
      @Override
      @NonNull
      public List<DisbursementEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDisbursementId = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursementId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfPayeeName = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeName");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfPurposeCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "purposeCategory");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfRuleEngineResult = CursorUtil.getColumnIndexOrThrow(_cursor, "ruleEngineResult");
          final int _cursorIndexOfApprovalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "approvalStatus");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfUpiDeepLink = CursorUtil.getColumnIndexOrThrow(_cursor, "upiDeepLink");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLenderNote = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderNote");
          final int _cursorIndexOfBorrowerNote = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerNote");
          final List<DisbursementEntity> _result = new ArrayList<DisbursementEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DisbursementEntity _item;
            final String _tmpDisbursementId;
            _tmpDisbursementId = _cursor.getString(_cursorIndexOfDisbursementId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpPayeeId;
            if (_cursor.isNull(_cursorIndexOfPayeeId)) {
              _tmpPayeeId = null;
            } else {
              _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            }
            final String _tmpPayeeName;
            _tmpPayeeName = _cursor.getString(_cursorIndexOfPayeeName);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpPurposeCategory;
            _tmpPurposeCategory = _cursor.getString(_cursorIndexOfPurposeCategory);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpRuleEngineResult;
            _tmpRuleEngineResult = _cursor.getString(_cursorIndexOfRuleEngineResult);
            final String _tmpApprovalStatus;
            _tmpApprovalStatus = _cursor.getString(_cursorIndexOfApprovalStatus);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpUpiDeepLink;
            _tmpUpiDeepLink = _cursor.getString(_cursorIndexOfUpiDeepLink);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpLenderNote;
            _tmpLenderNote = _cursor.getString(_cursorIndexOfLenderNote);
            final String _tmpBorrowerNote;
            _tmpBorrowerNote = _cursor.getString(_cursorIndexOfBorrowerNote);
            _item = new DisbursementEntity(_tmpDisbursementId,_tmpLoanId,_tmpAmount,_tmpPayeeId,_tmpPayeeName,_tmpPurpose,_tmpPurposeCategory,_tmpVerificationStatus,_tmpRuleEngineResult,_tmpApprovalStatus,_tmpTransactionRef,_tmpUpiDeepLink,_tmpTimestamp,_tmpLenderNote,_tmpBorrowerNote);
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
  public Flow<Double> getTotalDisbursedForLoan(final String loanId) {
    final String _sql = "SELECT SUM(amount) FROM disbursements WHERE loanId = ? AND approvalStatus = 'APPROVED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"disbursements"}, new Callable<Double>() {
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
  public Flow<Double> getTotalVerifiedForLoan(final String loanId) {
    final String _sql = "SELECT SUM(amount) FROM disbursements WHERE loanId = ? AND verificationStatus = 'VERIFIED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"disbursements"}, new Callable<Double>() {
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
  public Flow<List<DisbursementEntity>> getPendingApprovalsForLender(final String lenderId) {
    final String _sql = "SELECT * FROM disbursements WHERE approvalStatus = 'PENDING' AND loanId IN (SELECT loanId FROM loans WHERE lenderId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, lenderId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"disbursements",
        "loans"}, new Callable<List<DisbursementEntity>>() {
      @Override
      @NonNull
      public List<DisbursementEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDisbursementId = CursorUtil.getColumnIndexOrThrow(_cursor, "disbursementId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfPayeeName = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeName");
          final int _cursorIndexOfPurpose = CursorUtil.getColumnIndexOrThrow(_cursor, "purpose");
          final int _cursorIndexOfPurposeCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "purposeCategory");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfRuleEngineResult = CursorUtil.getColumnIndexOrThrow(_cursor, "ruleEngineResult");
          final int _cursorIndexOfApprovalStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "approvalStatus");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfUpiDeepLink = CursorUtil.getColumnIndexOrThrow(_cursor, "upiDeepLink");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLenderNote = CursorUtil.getColumnIndexOrThrow(_cursor, "lenderNote");
          final int _cursorIndexOfBorrowerNote = CursorUtil.getColumnIndexOrThrow(_cursor, "borrowerNote");
          final List<DisbursementEntity> _result = new ArrayList<DisbursementEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DisbursementEntity _item;
            final String _tmpDisbursementId;
            _tmpDisbursementId = _cursor.getString(_cursorIndexOfDisbursementId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpPayeeId;
            if (_cursor.isNull(_cursorIndexOfPayeeId)) {
              _tmpPayeeId = null;
            } else {
              _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            }
            final String _tmpPayeeName;
            _tmpPayeeName = _cursor.getString(_cursorIndexOfPayeeName);
            final String _tmpPurpose;
            _tmpPurpose = _cursor.getString(_cursorIndexOfPurpose);
            final String _tmpPurposeCategory;
            _tmpPurposeCategory = _cursor.getString(_cursorIndexOfPurposeCategory);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpRuleEngineResult;
            _tmpRuleEngineResult = _cursor.getString(_cursorIndexOfRuleEngineResult);
            final String _tmpApprovalStatus;
            _tmpApprovalStatus = _cursor.getString(_cursorIndexOfApprovalStatus);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpUpiDeepLink;
            _tmpUpiDeepLink = _cursor.getString(_cursorIndexOfUpiDeepLink);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpLenderNote;
            _tmpLenderNote = _cursor.getString(_cursorIndexOfLenderNote);
            final String _tmpBorrowerNote;
            _tmpBorrowerNote = _cursor.getString(_cursorIndexOfBorrowerNote);
            _item = new DisbursementEntity(_tmpDisbursementId,_tmpLoanId,_tmpAmount,_tmpPayeeId,_tmpPayeeName,_tmpPurpose,_tmpPurposeCategory,_tmpVerificationStatus,_tmpRuleEngineResult,_tmpApprovalStatus,_tmpTransactionRef,_tmpUpiDeepLink,_tmpTimestamp,_tmpLenderNote,_tmpBorrowerNote);
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
  public Flow<Integer> getFlaggedCountForLoan(final String loanId) {
    final String _sql = "SELECT COUNT(*) FROM disbursements WHERE loanId = ? AND verificationStatus = 'FLAGGED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"disbursements"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Flow<List<CategoryBreakdown>> getCategoryBreakdownForUser(final String userId) {
    final String _sql = "SELECT purposeCategory, SUM(amount) as total FROM disbursements WHERE loanId IN (SELECT loanId FROM loans WHERE borrowerId = ?) AND approvalStatus = 'APPROVED' GROUP BY purposeCategory";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"disbursements",
        "loans"}, new Callable<List<CategoryBreakdown>>() {
      @Override
      @NonNull
      public List<CategoryBreakdown> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPurposeCategory = 0;
          final int _cursorIndexOfTotal = 1;
          final List<CategoryBreakdown> _result = new ArrayList<CategoryBreakdown>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategoryBreakdown _item;
            final String _tmpPurposeCategory;
            _tmpPurposeCategory = _cursor.getString(_cursorIndexOfPurposeCategory);
            final double _tmpTotal;
            _tmpTotal = _cursor.getDouble(_cursorIndexOfTotal);
            _item = new CategoryBreakdown(_tmpPurposeCategory,_tmpTotal);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
