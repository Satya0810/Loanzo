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
import com.loanzo.app.data.entity.GuarantorEntity;
import java.lang.Class;
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
public final class GuarantorDao_Impl implements GuarantorDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GuarantorEntity> __insertionAdapterOfGuarantorEntity;

  private final EntityDeletionOrUpdateAdapter<GuarantorEntity> __deletionAdapterOfGuarantorEntity;

  private final EntityDeletionOrUpdateAdapter<GuarantorEntity> __updateAdapterOfGuarantorEntity;

  public GuarantorDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGuarantorEntity = new EntityInsertionAdapter<GuarantorEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `guarantors` (`guarantorId`,`loanId`,`name`,`phone`,`email`,`panNumber`,`relationship`,`consentStatus`,`consentTimestamp`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GuarantorEntity entity) {
        statement.bindString(1, entity.getGuarantorId());
        statement.bindString(2, entity.getLoanId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getPhone());
        statement.bindString(5, entity.getEmail());
        statement.bindString(6, entity.getPanNumber());
        statement.bindString(7, entity.getRelationship());
        statement.bindString(8, entity.getConsentStatus());
        if (entity.getConsentTimestamp() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getConsentTimestamp());
        }
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfGuarantorEntity = new EntityDeletionOrUpdateAdapter<GuarantorEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `guarantors` WHERE `guarantorId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GuarantorEntity entity) {
        statement.bindString(1, entity.getGuarantorId());
      }
    };
    this.__updateAdapterOfGuarantorEntity = new EntityDeletionOrUpdateAdapter<GuarantorEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `guarantors` SET `guarantorId` = ?,`loanId` = ?,`name` = ?,`phone` = ?,`email` = ?,`panNumber` = ?,`relationship` = ?,`consentStatus` = ?,`consentTimestamp` = ?,`createdAt` = ? WHERE `guarantorId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GuarantorEntity entity) {
        statement.bindString(1, entity.getGuarantorId());
        statement.bindString(2, entity.getLoanId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getPhone());
        statement.bindString(5, entity.getEmail());
        statement.bindString(6, entity.getPanNumber());
        statement.bindString(7, entity.getRelationship());
        statement.bindString(8, entity.getConsentStatus());
        if (entity.getConsentTimestamp() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getConsentTimestamp());
        }
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindString(11, entity.getGuarantorId());
      }
    };
  }

  @Override
  public Object insertGuarantor(final GuarantorEntity guarantor,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGuarantorEntity.insert(guarantor);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteGuarantor(final GuarantorEntity guarantor,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfGuarantorEntity.handle(guarantor);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateGuarantor(final GuarantorEntity guarantor,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfGuarantorEntity.handle(guarantor);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GuarantorEntity>> getGuarantorsByLoan(final String loanId) {
    final String _sql = "SELECT * FROM guarantors WHERE loanId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"guarantors"}, new Callable<List<GuarantorEntity>>() {
      @Override
      @NonNull
      public List<GuarantorEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGuarantorId = CursorUtil.getColumnIndexOrThrow(_cursor, "guarantorId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfPanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "panNumber");
          final int _cursorIndexOfRelationship = CursorUtil.getColumnIndexOrThrow(_cursor, "relationship");
          final int _cursorIndexOfConsentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "consentStatus");
          final int _cursorIndexOfConsentTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "consentTimestamp");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<GuarantorEntity> _result = new ArrayList<GuarantorEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GuarantorEntity _item;
            final String _tmpGuarantorId;
            _tmpGuarantorId = _cursor.getString(_cursorIndexOfGuarantorId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpPanNumber;
            _tmpPanNumber = _cursor.getString(_cursorIndexOfPanNumber);
            final String _tmpRelationship;
            _tmpRelationship = _cursor.getString(_cursorIndexOfRelationship);
            final String _tmpConsentStatus;
            _tmpConsentStatus = _cursor.getString(_cursorIndexOfConsentStatus);
            final Long _tmpConsentTimestamp;
            if (_cursor.isNull(_cursorIndexOfConsentTimestamp)) {
              _tmpConsentTimestamp = null;
            } else {
              _tmpConsentTimestamp = _cursor.getLong(_cursorIndexOfConsentTimestamp);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new GuarantorEntity(_tmpGuarantorId,_tmpLoanId,_tmpName,_tmpPhone,_tmpEmail,_tmpPanNumber,_tmpRelationship,_tmpConsentStatus,_tmpConsentTimestamp,_tmpCreatedAt);
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
  public Object getGuarantorById(final String id,
      final Continuation<? super GuarantorEntity> $completion) {
    final String _sql = "SELECT * FROM guarantors WHERE guarantorId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GuarantorEntity>() {
      @Override
      @Nullable
      public GuarantorEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGuarantorId = CursorUtil.getColumnIndexOrThrow(_cursor, "guarantorId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfPanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "panNumber");
          final int _cursorIndexOfRelationship = CursorUtil.getColumnIndexOrThrow(_cursor, "relationship");
          final int _cursorIndexOfConsentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "consentStatus");
          final int _cursorIndexOfConsentTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "consentTimestamp");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final GuarantorEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpGuarantorId;
            _tmpGuarantorId = _cursor.getString(_cursorIndexOfGuarantorId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpPanNumber;
            _tmpPanNumber = _cursor.getString(_cursorIndexOfPanNumber);
            final String _tmpRelationship;
            _tmpRelationship = _cursor.getString(_cursorIndexOfRelationship);
            final String _tmpConsentStatus;
            _tmpConsentStatus = _cursor.getString(_cursorIndexOfConsentStatus);
            final Long _tmpConsentTimestamp;
            if (_cursor.isNull(_cursorIndexOfConsentTimestamp)) {
              _tmpConsentTimestamp = null;
            } else {
              _tmpConsentTimestamp = _cursor.getLong(_cursorIndexOfConsentTimestamp);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new GuarantorEntity(_tmpGuarantorId,_tmpLoanId,_tmpName,_tmpPhone,_tmpEmail,_tmpPanNumber,_tmpRelationship,_tmpConsentStatus,_tmpConsentTimestamp,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
