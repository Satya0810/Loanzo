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
import com.loanzo.app.data.entity.PayeeEntity;
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
public final class PayeeDao_Impl implements PayeeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PayeeEntity> __insertionAdapterOfPayeeEntity;

  private final EntityDeletionOrUpdateAdapter<PayeeEntity> __deletionAdapterOfPayeeEntity;

  private final EntityDeletionOrUpdateAdapter<PayeeEntity> __updateAdapterOfPayeeEntity;

  public PayeeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPayeeEntity = new EntityInsertionAdapter<PayeeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `payees` (`payeeId`,`name`,`upiId`,`gstNumber`,`businessName`,`verificationStatus`,`category`,`verifiedAt`,`addedBy`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PayeeEntity entity) {
        statement.bindString(1, entity.getPayeeId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getUpiId());
        statement.bindString(4, entity.getGstNumber());
        statement.bindString(5, entity.getBusinessName());
        statement.bindString(6, entity.getVerificationStatus());
        statement.bindString(7, entity.getCategory());
        if (entity.getVerifiedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getVerifiedAt());
        }
        statement.bindString(9, entity.getAddedBy());
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfPayeeEntity = new EntityDeletionOrUpdateAdapter<PayeeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `payees` WHERE `payeeId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PayeeEntity entity) {
        statement.bindString(1, entity.getPayeeId());
      }
    };
    this.__updateAdapterOfPayeeEntity = new EntityDeletionOrUpdateAdapter<PayeeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `payees` SET `payeeId` = ?,`name` = ?,`upiId` = ?,`gstNumber` = ?,`businessName` = ?,`verificationStatus` = ?,`category` = ?,`verifiedAt` = ?,`addedBy` = ?,`createdAt` = ? WHERE `payeeId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PayeeEntity entity) {
        statement.bindString(1, entity.getPayeeId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getUpiId());
        statement.bindString(4, entity.getGstNumber());
        statement.bindString(5, entity.getBusinessName());
        statement.bindString(6, entity.getVerificationStatus());
        statement.bindString(7, entity.getCategory());
        if (entity.getVerifiedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getVerifiedAt());
        }
        statement.bindString(9, entity.getAddedBy());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindString(11, entity.getPayeeId());
      }
    };
  }

  @Override
  public Object insertPayee(final PayeeEntity payee, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPayeeEntity.insert(payee);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePayee(final PayeeEntity payee, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPayeeEntity.handle(payee);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePayee(final PayeeEntity payee, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPayeeEntity.handle(payee);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPayeeById(final String id, final Continuation<? super PayeeEntity> $completion) {
    final String _sql = "SELECT * FROM payees WHERE payeeId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PayeeEntity>() {
      @Override
      @Nullable
      public PayeeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfGstNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "gstNumber");
          final int _cursorIndexOfBusinessName = CursorUtil.getColumnIndexOrThrow(_cursor, "businessName");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "verifiedAt");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final PayeeEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPayeeId;
            _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpGstNumber;
            _tmpGstNumber = _cursor.getString(_cursorIndexOfGstNumber);
            final String _tmpBusinessName;
            _tmpBusinessName = _cursor.getString(_cursorIndexOfBusinessName);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final Long _tmpVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfVerifiedAt)) {
              _tmpVerifiedAt = null;
            } else {
              _tmpVerifiedAt = _cursor.getLong(_cursorIndexOfVerifiedAt);
            }
            final String _tmpAddedBy;
            _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new PayeeEntity(_tmpPayeeId,_tmpName,_tmpUpiId,_tmpGstNumber,_tmpBusinessName,_tmpVerificationStatus,_tmpCategory,_tmpVerifiedAt,_tmpAddedBy,_tmpCreatedAt);
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
  public Object getPayeeByUpiId(final String upiId,
      final Continuation<? super PayeeEntity> $completion) {
    final String _sql = "SELECT * FROM payees WHERE upiId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, upiId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PayeeEntity>() {
      @Override
      @Nullable
      public PayeeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfGstNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "gstNumber");
          final int _cursorIndexOfBusinessName = CursorUtil.getColumnIndexOrThrow(_cursor, "businessName");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "verifiedAt");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final PayeeEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPayeeId;
            _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpGstNumber;
            _tmpGstNumber = _cursor.getString(_cursorIndexOfGstNumber);
            final String _tmpBusinessName;
            _tmpBusinessName = _cursor.getString(_cursorIndexOfBusinessName);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final Long _tmpVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfVerifiedAt)) {
              _tmpVerifiedAt = null;
            } else {
              _tmpVerifiedAt = _cursor.getLong(_cursorIndexOfVerifiedAt);
            }
            final String _tmpAddedBy;
            _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new PayeeEntity(_tmpPayeeId,_tmpName,_tmpUpiId,_tmpGstNumber,_tmpBusinessName,_tmpVerificationStatus,_tmpCategory,_tmpVerifiedAt,_tmpAddedBy,_tmpCreatedAt);
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
  public Flow<List<PayeeEntity>> getPayeesByUser(final String userId) {
    final String _sql = "SELECT * FROM payees WHERE addedBy = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payees"}, new Callable<List<PayeeEntity>>() {
      @Override
      @NonNull
      public List<PayeeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfGstNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "gstNumber");
          final int _cursorIndexOfBusinessName = CursorUtil.getColumnIndexOrThrow(_cursor, "businessName");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "verifiedAt");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<PayeeEntity> _result = new ArrayList<PayeeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PayeeEntity _item;
            final String _tmpPayeeId;
            _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpGstNumber;
            _tmpGstNumber = _cursor.getString(_cursorIndexOfGstNumber);
            final String _tmpBusinessName;
            _tmpBusinessName = _cursor.getString(_cursorIndexOfBusinessName);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final Long _tmpVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfVerifiedAt)) {
              _tmpVerifiedAt = null;
            } else {
              _tmpVerifiedAt = _cursor.getLong(_cursorIndexOfVerifiedAt);
            }
            final String _tmpAddedBy;
            _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new PayeeEntity(_tmpPayeeId,_tmpName,_tmpUpiId,_tmpGstNumber,_tmpBusinessName,_tmpVerificationStatus,_tmpCategory,_tmpVerifiedAt,_tmpAddedBy,_tmpCreatedAt);
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
  public Flow<List<PayeeEntity>> getVerifiedPayees() {
    final String _sql = "SELECT * FROM payees WHERE verificationStatus = 'VERIFIED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payees"}, new Callable<List<PayeeEntity>>() {
      @Override
      @NonNull
      public List<PayeeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfGstNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "gstNumber");
          final int _cursorIndexOfBusinessName = CursorUtil.getColumnIndexOrThrow(_cursor, "businessName");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "verifiedAt");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<PayeeEntity> _result = new ArrayList<PayeeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PayeeEntity _item;
            final String _tmpPayeeId;
            _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpGstNumber;
            _tmpGstNumber = _cursor.getString(_cursorIndexOfGstNumber);
            final String _tmpBusinessName;
            _tmpBusinessName = _cursor.getString(_cursorIndexOfBusinessName);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final Long _tmpVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfVerifiedAt)) {
              _tmpVerifiedAt = null;
            } else {
              _tmpVerifiedAt = _cursor.getLong(_cursorIndexOfVerifiedAt);
            }
            final String _tmpAddedBy;
            _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new PayeeEntity(_tmpPayeeId,_tmpName,_tmpUpiId,_tmpGstNumber,_tmpBusinessName,_tmpVerificationStatus,_tmpCategory,_tmpVerifiedAt,_tmpAddedBy,_tmpCreatedAt);
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
  public Flow<List<PayeeEntity>> getPayeesByCategory(final String category) {
    final String _sql = "SELECT * FROM payees WHERE category = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payees"}, new Callable<List<PayeeEntity>>() {
      @Override
      @NonNull
      public List<PayeeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfGstNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "gstNumber");
          final int _cursorIndexOfBusinessName = CursorUtil.getColumnIndexOrThrow(_cursor, "businessName");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "verifiedAt");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<PayeeEntity> _result = new ArrayList<PayeeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PayeeEntity _item;
            final String _tmpPayeeId;
            _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpGstNumber;
            _tmpGstNumber = _cursor.getString(_cursorIndexOfGstNumber);
            final String _tmpBusinessName;
            _tmpBusinessName = _cursor.getString(_cursorIndexOfBusinessName);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final Long _tmpVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfVerifiedAt)) {
              _tmpVerifiedAt = null;
            } else {
              _tmpVerifiedAt = _cursor.getLong(_cursorIndexOfVerifiedAt);
            }
            final String _tmpAddedBy;
            _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new PayeeEntity(_tmpPayeeId,_tmpName,_tmpUpiId,_tmpGstNumber,_tmpBusinessName,_tmpVerificationStatus,_tmpCategory,_tmpVerifiedAt,_tmpAddedBy,_tmpCreatedAt);
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
  public Flow<List<PayeeEntity>> getAllPayees() {
    final String _sql = "SELECT * FROM payees ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payees"}, new Callable<List<PayeeEntity>>() {
      @Override
      @NonNull
      public List<PayeeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPayeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "payeeId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfGstNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "gstNumber");
          final int _cursorIndexOfBusinessName = CursorUtil.getColumnIndexOrThrow(_cursor, "businessName");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "verifiedAt");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<PayeeEntity> _result = new ArrayList<PayeeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PayeeEntity _item;
            final String _tmpPayeeId;
            _tmpPayeeId = _cursor.getString(_cursorIndexOfPayeeId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpGstNumber;
            _tmpGstNumber = _cursor.getString(_cursorIndexOfGstNumber);
            final String _tmpBusinessName;
            _tmpBusinessName = _cursor.getString(_cursorIndexOfBusinessName);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final Long _tmpVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfVerifiedAt)) {
              _tmpVerifiedAt = null;
            } else {
              _tmpVerifiedAt = _cursor.getLong(_cursorIndexOfVerifiedAt);
            }
            final String _tmpAddedBy;
            _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new PayeeEntity(_tmpPayeeId,_tmpName,_tmpUpiId,_tmpGstNumber,_tmpBusinessName,_tmpVerificationStatus,_tmpCategory,_tmpVerifiedAt,_tmpAddedBy,_tmpCreatedAt);
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
