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
import com.loanzo.app.data.entity.RepaymentEntity;
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
public final class RepaymentDao_Impl implements RepaymentDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RepaymentEntity> __insertionAdapterOfRepaymentEntity;

  private final EntityDeletionOrUpdateAdapter<RepaymentEntity> __deletionAdapterOfRepaymentEntity;

  private final EntityDeletionOrUpdateAdapter<RepaymentEntity> __updateAdapterOfRepaymentEntity;

  public RepaymentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRepaymentEntity = new EntityInsertionAdapter<RepaymentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `repayments` (`repaymentId`,`loanId`,`amount`,`transactionRef`,`status`,`dueDate`,`paidDate`,`outstandingSnapshot`,`principalComponent`,`interestComponent`,`penalty`,`timestamp`,`note`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RepaymentEntity entity) {
        statement.bindString(1, entity.getRepaymentId());
        statement.bindString(2, entity.getLoanId());
        statement.bindDouble(3, entity.getAmount());
        statement.bindString(4, entity.getTransactionRef());
        statement.bindString(5, entity.getStatus());
        statement.bindLong(6, entity.getDueDate());
        if (entity.getPaidDate() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getPaidDate());
        }
        statement.bindDouble(8, entity.getOutstandingSnapshot());
        statement.bindDouble(9, entity.getPrincipalComponent());
        statement.bindDouble(10, entity.getInterestComponent());
        statement.bindDouble(11, entity.getPenalty());
        statement.bindLong(12, entity.getTimestamp());
        statement.bindString(13, entity.getNote());
      }
    };
    this.__deletionAdapterOfRepaymentEntity = new EntityDeletionOrUpdateAdapter<RepaymentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `repayments` WHERE `repaymentId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RepaymentEntity entity) {
        statement.bindString(1, entity.getRepaymentId());
      }
    };
    this.__updateAdapterOfRepaymentEntity = new EntityDeletionOrUpdateAdapter<RepaymentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `repayments` SET `repaymentId` = ?,`loanId` = ?,`amount` = ?,`transactionRef` = ?,`status` = ?,`dueDate` = ?,`paidDate` = ?,`outstandingSnapshot` = ?,`principalComponent` = ?,`interestComponent` = ?,`penalty` = ?,`timestamp` = ?,`note` = ? WHERE `repaymentId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RepaymentEntity entity) {
        statement.bindString(1, entity.getRepaymentId());
        statement.bindString(2, entity.getLoanId());
        statement.bindDouble(3, entity.getAmount());
        statement.bindString(4, entity.getTransactionRef());
        statement.bindString(5, entity.getStatus());
        statement.bindLong(6, entity.getDueDate());
        if (entity.getPaidDate() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getPaidDate());
        }
        statement.bindDouble(8, entity.getOutstandingSnapshot());
        statement.bindDouble(9, entity.getPrincipalComponent());
        statement.bindDouble(10, entity.getInterestComponent());
        statement.bindDouble(11, entity.getPenalty());
        statement.bindLong(12, entity.getTimestamp());
        statement.bindString(13, entity.getNote());
        statement.bindString(14, entity.getRepaymentId());
      }
    };
  }

  @Override
  public Object insertRepayment(final RepaymentEntity repayment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRepaymentEntity.insert(repayment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRepayment(final RepaymentEntity repayment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRepaymentEntity.handle(repayment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRepayment(final RepaymentEntity repayment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRepaymentEntity.handle(repayment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRepaymentById(final String id,
      final Continuation<? super RepaymentEntity> $completion) {
    final String _sql = "SELECT * FROM repayments WHERE repaymentId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RepaymentEntity>() {
      @Override
      @Nullable
      public RepaymentEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRepaymentId = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfOutstandingSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingSnapshot");
          final int _cursorIndexOfPrincipalComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "principalComponent");
          final int _cursorIndexOfInterestComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestComponent");
          final int _cursorIndexOfPenalty = CursorUtil.getColumnIndexOrThrow(_cursor, "penalty");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final RepaymentEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpRepaymentId;
            _tmpRepaymentId = _cursor.getString(_cursorIndexOfRepaymentId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpDueDate;
            _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            final Long _tmpPaidDate;
            if (_cursor.isNull(_cursorIndexOfPaidDate)) {
              _tmpPaidDate = null;
            } else {
              _tmpPaidDate = _cursor.getLong(_cursorIndexOfPaidDate);
            }
            final double _tmpOutstandingSnapshot;
            _tmpOutstandingSnapshot = _cursor.getDouble(_cursorIndexOfOutstandingSnapshot);
            final double _tmpPrincipalComponent;
            _tmpPrincipalComponent = _cursor.getDouble(_cursorIndexOfPrincipalComponent);
            final double _tmpInterestComponent;
            _tmpInterestComponent = _cursor.getDouble(_cursorIndexOfInterestComponent);
            final double _tmpPenalty;
            _tmpPenalty = _cursor.getDouble(_cursorIndexOfPenalty);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _result = new RepaymentEntity(_tmpRepaymentId,_tmpLoanId,_tmpAmount,_tmpTransactionRef,_tmpStatus,_tmpDueDate,_tmpPaidDate,_tmpOutstandingSnapshot,_tmpPrincipalComponent,_tmpInterestComponent,_tmpPenalty,_tmpTimestamp,_tmpNote);
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
  public Flow<List<RepaymentEntity>> getRepaymentsByLoan(final String loanId) {
    final String _sql = "SELECT * FROM repayments WHERE loanId = ? ORDER BY dueDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repayments"}, new Callable<List<RepaymentEntity>>() {
      @Override
      @NonNull
      public List<RepaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRepaymentId = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfOutstandingSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingSnapshot");
          final int _cursorIndexOfPrincipalComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "principalComponent");
          final int _cursorIndexOfInterestComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestComponent");
          final int _cursorIndexOfPenalty = CursorUtil.getColumnIndexOrThrow(_cursor, "penalty");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<RepaymentEntity> _result = new ArrayList<RepaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepaymentEntity _item;
            final String _tmpRepaymentId;
            _tmpRepaymentId = _cursor.getString(_cursorIndexOfRepaymentId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpDueDate;
            _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            final Long _tmpPaidDate;
            if (_cursor.isNull(_cursorIndexOfPaidDate)) {
              _tmpPaidDate = null;
            } else {
              _tmpPaidDate = _cursor.getLong(_cursorIndexOfPaidDate);
            }
            final double _tmpOutstandingSnapshot;
            _tmpOutstandingSnapshot = _cursor.getDouble(_cursorIndexOfOutstandingSnapshot);
            final double _tmpPrincipalComponent;
            _tmpPrincipalComponent = _cursor.getDouble(_cursorIndexOfPrincipalComponent);
            final double _tmpInterestComponent;
            _tmpInterestComponent = _cursor.getDouble(_cursorIndexOfInterestComponent);
            final double _tmpPenalty;
            _tmpPenalty = _cursor.getDouble(_cursorIndexOfPenalty);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new RepaymentEntity(_tmpRepaymentId,_tmpLoanId,_tmpAmount,_tmpTransactionRef,_tmpStatus,_tmpDueDate,_tmpPaidDate,_tmpOutstandingSnapshot,_tmpPrincipalComponent,_tmpInterestComponent,_tmpPenalty,_tmpTimestamp,_tmpNote);
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
  public Flow<RepaymentEntity> getNextDueRepayment(final String loanId) {
    final String _sql = "SELECT * FROM repayments WHERE loanId = ? AND status = 'SCHEDULED' ORDER BY dueDate ASC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repayments"}, new Callable<RepaymentEntity>() {
      @Override
      @Nullable
      public RepaymentEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRepaymentId = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfOutstandingSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingSnapshot");
          final int _cursorIndexOfPrincipalComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "principalComponent");
          final int _cursorIndexOfInterestComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestComponent");
          final int _cursorIndexOfPenalty = CursorUtil.getColumnIndexOrThrow(_cursor, "penalty");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final RepaymentEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpRepaymentId;
            _tmpRepaymentId = _cursor.getString(_cursorIndexOfRepaymentId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpDueDate;
            _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            final Long _tmpPaidDate;
            if (_cursor.isNull(_cursorIndexOfPaidDate)) {
              _tmpPaidDate = null;
            } else {
              _tmpPaidDate = _cursor.getLong(_cursorIndexOfPaidDate);
            }
            final double _tmpOutstandingSnapshot;
            _tmpOutstandingSnapshot = _cursor.getDouble(_cursorIndexOfOutstandingSnapshot);
            final double _tmpPrincipalComponent;
            _tmpPrincipalComponent = _cursor.getDouble(_cursorIndexOfPrincipalComponent);
            final double _tmpInterestComponent;
            _tmpInterestComponent = _cursor.getDouble(_cursorIndexOfInterestComponent);
            final double _tmpPenalty;
            _tmpPenalty = _cursor.getDouble(_cursorIndexOfPenalty);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _result = new RepaymentEntity(_tmpRepaymentId,_tmpLoanId,_tmpAmount,_tmpTransactionRef,_tmpStatus,_tmpDueDate,_tmpPaidDate,_tmpOutstandingSnapshot,_tmpPrincipalComponent,_tmpInterestComponent,_tmpPenalty,_tmpTimestamp,_tmpNote);
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
  public Flow<List<RepaymentEntity>> getOverdueRepaymentsForBorrower(final String userId) {
    final String _sql = "SELECT * FROM repayments WHERE status = 'OVERDUE' AND loanId IN (SELECT loanId FROM loans WHERE borrowerId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repayments",
        "loans"}, new Callable<List<RepaymentEntity>>() {
      @Override
      @NonNull
      public List<RepaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRepaymentId = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfOutstandingSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingSnapshot");
          final int _cursorIndexOfPrincipalComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "principalComponent");
          final int _cursorIndexOfInterestComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestComponent");
          final int _cursorIndexOfPenalty = CursorUtil.getColumnIndexOrThrow(_cursor, "penalty");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<RepaymentEntity> _result = new ArrayList<RepaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepaymentEntity _item;
            final String _tmpRepaymentId;
            _tmpRepaymentId = _cursor.getString(_cursorIndexOfRepaymentId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpDueDate;
            _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            final Long _tmpPaidDate;
            if (_cursor.isNull(_cursorIndexOfPaidDate)) {
              _tmpPaidDate = null;
            } else {
              _tmpPaidDate = _cursor.getLong(_cursorIndexOfPaidDate);
            }
            final double _tmpOutstandingSnapshot;
            _tmpOutstandingSnapshot = _cursor.getDouble(_cursorIndexOfOutstandingSnapshot);
            final double _tmpPrincipalComponent;
            _tmpPrincipalComponent = _cursor.getDouble(_cursorIndexOfPrincipalComponent);
            final double _tmpInterestComponent;
            _tmpInterestComponent = _cursor.getDouble(_cursorIndexOfInterestComponent);
            final double _tmpPenalty;
            _tmpPenalty = _cursor.getDouble(_cursorIndexOfPenalty);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new RepaymentEntity(_tmpRepaymentId,_tmpLoanId,_tmpAmount,_tmpTransactionRef,_tmpStatus,_tmpDueDate,_tmpPaidDate,_tmpOutstandingSnapshot,_tmpPrincipalComponent,_tmpInterestComponent,_tmpPenalty,_tmpTimestamp,_tmpNote);
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
  public Flow<List<RepaymentEntity>> getOverdueRepaymentsForLender(final String userId) {
    final String _sql = "SELECT * FROM repayments WHERE status = 'OVERDUE' AND loanId IN (SELECT loanId FROM loans WHERE lenderId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repayments",
        "loans"}, new Callable<List<RepaymentEntity>>() {
      @Override
      @NonNull
      public List<RepaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRepaymentId = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfOutstandingSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingSnapshot");
          final int _cursorIndexOfPrincipalComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "principalComponent");
          final int _cursorIndexOfInterestComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestComponent");
          final int _cursorIndexOfPenalty = CursorUtil.getColumnIndexOrThrow(_cursor, "penalty");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<RepaymentEntity> _result = new ArrayList<RepaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepaymentEntity _item;
            final String _tmpRepaymentId;
            _tmpRepaymentId = _cursor.getString(_cursorIndexOfRepaymentId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpDueDate;
            _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            final Long _tmpPaidDate;
            if (_cursor.isNull(_cursorIndexOfPaidDate)) {
              _tmpPaidDate = null;
            } else {
              _tmpPaidDate = _cursor.getLong(_cursorIndexOfPaidDate);
            }
            final double _tmpOutstandingSnapshot;
            _tmpOutstandingSnapshot = _cursor.getDouble(_cursorIndexOfOutstandingSnapshot);
            final double _tmpPrincipalComponent;
            _tmpPrincipalComponent = _cursor.getDouble(_cursorIndexOfPrincipalComponent);
            final double _tmpInterestComponent;
            _tmpInterestComponent = _cursor.getDouble(_cursorIndexOfInterestComponent);
            final double _tmpPenalty;
            _tmpPenalty = _cursor.getDouble(_cursorIndexOfPenalty);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new RepaymentEntity(_tmpRepaymentId,_tmpLoanId,_tmpAmount,_tmpTransactionRef,_tmpStatus,_tmpDueDate,_tmpPaidDate,_tmpOutstandingSnapshot,_tmpPrincipalComponent,_tmpInterestComponent,_tmpPenalty,_tmpTimestamp,_tmpNote);
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
  public Flow<Double> getTotalPaidForLoan(final String loanId) {
    final String _sql = "SELECT SUM(amount) FROM repayments WHERE loanId = ? AND status = 'PAID'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repayments"}, new Callable<Double>() {
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
  public Flow<List<RepaymentEntity>> getUpcomingRepayments(final long startTime,
      final long endTime) {
    final String _sql = "SELECT * FROM repayments WHERE dueDate BETWEEN ? AND ? AND status = 'SCHEDULED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repayments"}, new Callable<List<RepaymentEntity>>() {
      @Override
      @NonNull
      public List<RepaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRepaymentId = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTransactionRef = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionRef");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfOutstandingSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "outstandingSnapshot");
          final int _cursorIndexOfPrincipalComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "principalComponent");
          final int _cursorIndexOfInterestComponent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestComponent");
          final int _cursorIndexOfPenalty = CursorUtil.getColumnIndexOrThrow(_cursor, "penalty");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<RepaymentEntity> _result = new ArrayList<RepaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepaymentEntity _item;
            final String _tmpRepaymentId;
            _tmpRepaymentId = _cursor.getString(_cursorIndexOfRepaymentId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpTransactionRef;
            _tmpTransactionRef = _cursor.getString(_cursorIndexOfTransactionRef);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpDueDate;
            _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            final Long _tmpPaidDate;
            if (_cursor.isNull(_cursorIndexOfPaidDate)) {
              _tmpPaidDate = null;
            } else {
              _tmpPaidDate = _cursor.getLong(_cursorIndexOfPaidDate);
            }
            final double _tmpOutstandingSnapshot;
            _tmpOutstandingSnapshot = _cursor.getDouble(_cursorIndexOfOutstandingSnapshot);
            final double _tmpPrincipalComponent;
            _tmpPrincipalComponent = _cursor.getDouble(_cursorIndexOfPrincipalComponent);
            final double _tmpInterestComponent;
            _tmpInterestComponent = _cursor.getDouble(_cursorIndexOfInterestComponent);
            final double _tmpPenalty;
            _tmpPenalty = _cursor.getDouble(_cursorIndexOfPenalty);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new RepaymentEntity(_tmpRepaymentId,_tmpLoanId,_tmpAmount,_tmpTransactionRef,_tmpStatus,_tmpDueDate,_tmpPaidDate,_tmpOutstandingSnapshot,_tmpPrincipalComponent,_tmpInterestComponent,_tmpPenalty,_tmpTimestamp,_tmpNote);
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
