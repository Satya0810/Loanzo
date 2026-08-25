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
import com.loanzo.app.data.entity.UserEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserEntity> __insertionAdapterOfUserEntity;

  private final EntityDeletionOrUpdateAdapter<UserEntity> __deletionAdapterOfUserEntity;

  private final EntityDeletionOrUpdateAdapter<UserEntity> __updateAdapterOfUserEntity;

  public UserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserEntity = new EntityInsertionAdapter<UserEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `users` (`userId`,`name`,`email`,`phone`,`role`,`kycStatus`,`panNumber`,`aadhaarVerified`,`selfieVerified`,`upiId`,`bankAccountNumber`,`profilePhotoUri`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserEntity entity) {
        statement.bindString(1, entity.getUserId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getEmail());
        statement.bindString(4, entity.getPhone());
        statement.bindString(5, entity.getRole());
        statement.bindString(6, entity.getKycStatus());
        statement.bindString(7, entity.getPanNumber());
        final int _tmp = entity.getAadhaarVerified() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.getSelfieVerified() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindString(10, entity.getUpiId());
        statement.bindString(11, entity.getBankAccountNumber());
        statement.bindString(12, entity.getProfilePhotoUri());
        statement.bindLong(13, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfUserEntity = new EntityDeletionOrUpdateAdapter<UserEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `users` WHERE `userId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserEntity entity) {
        statement.bindString(1, entity.getUserId());
      }
    };
    this.__updateAdapterOfUserEntity = new EntityDeletionOrUpdateAdapter<UserEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `users` SET `userId` = ?,`name` = ?,`email` = ?,`phone` = ?,`role` = ?,`kycStatus` = ?,`panNumber` = ?,`aadhaarVerified` = ?,`selfieVerified` = ?,`upiId` = ?,`bankAccountNumber` = ?,`profilePhotoUri` = ?,`createdAt` = ? WHERE `userId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserEntity entity) {
        statement.bindString(1, entity.getUserId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getEmail());
        statement.bindString(4, entity.getPhone());
        statement.bindString(5, entity.getRole());
        statement.bindString(6, entity.getKycStatus());
        statement.bindString(7, entity.getPanNumber());
        final int _tmp = entity.getAadhaarVerified() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.getSelfieVerified() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindString(10, entity.getUpiId());
        statement.bindString(11, entity.getBankAccountNumber());
        statement.bindString(12, entity.getProfilePhotoUri());
        statement.bindLong(13, entity.getCreatedAt());
        statement.bindString(14, entity.getUserId());
      }
    };
  }

  @Override
  public Object insertUser(final UserEntity user, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserEntity.insert(user);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteUser(final UserEntity user, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfUserEntity.handle(user);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateUser(final UserEntity user, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfUserEntity.handle(user);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getUserById(final String userId,
      final Continuation<? super UserEntity> $completion) {
    final String _sql = "SELECT * FROM users WHERE userId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserEntity>() {
      @Override
      @Nullable
      public UserEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfKycStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "kycStatus");
          final int _cursorIndexOfPanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "panNumber");
          final int _cursorIndexOfAadhaarVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "aadhaarVerified");
          final int _cursorIndexOfSelfieVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "selfieVerified");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfBankAccountNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "bankAccountNumber");
          final int _cursorIndexOfProfilePhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profilePhotoUri");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final UserEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpKycStatus;
            _tmpKycStatus = _cursor.getString(_cursorIndexOfKycStatus);
            final String _tmpPanNumber;
            _tmpPanNumber = _cursor.getString(_cursorIndexOfPanNumber);
            final boolean _tmpAadhaarVerified;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAadhaarVerified);
            _tmpAadhaarVerified = _tmp != 0;
            final boolean _tmpSelfieVerified;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSelfieVerified);
            _tmpSelfieVerified = _tmp_1 != 0;
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpBankAccountNumber;
            _tmpBankAccountNumber = _cursor.getString(_cursorIndexOfBankAccountNumber);
            final String _tmpProfilePhotoUri;
            _tmpProfilePhotoUri = _cursor.getString(_cursorIndexOfProfilePhotoUri);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new UserEntity(_tmpUserId,_tmpName,_tmpEmail,_tmpPhone,_tmpRole,_tmpKycStatus,_tmpPanNumber,_tmpAadhaarVerified,_tmpSelfieVerified,_tmpUpiId,_tmpBankAccountNumber,_tmpProfilePhotoUri,_tmpCreatedAt);
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
  public Flow<UserEntity> observeUser(final String userId) {
    final String _sql = "SELECT * FROM users WHERE userId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"users"}, new Callable<UserEntity>() {
      @Override
      @Nullable
      public UserEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfKycStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "kycStatus");
          final int _cursorIndexOfPanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "panNumber");
          final int _cursorIndexOfAadhaarVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "aadhaarVerified");
          final int _cursorIndexOfSelfieVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "selfieVerified");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfBankAccountNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "bankAccountNumber");
          final int _cursorIndexOfProfilePhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profilePhotoUri");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final UserEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpKycStatus;
            _tmpKycStatus = _cursor.getString(_cursorIndexOfKycStatus);
            final String _tmpPanNumber;
            _tmpPanNumber = _cursor.getString(_cursorIndexOfPanNumber);
            final boolean _tmpAadhaarVerified;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAadhaarVerified);
            _tmpAadhaarVerified = _tmp != 0;
            final boolean _tmpSelfieVerified;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSelfieVerified);
            _tmpSelfieVerified = _tmp_1 != 0;
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpBankAccountNumber;
            _tmpBankAccountNumber = _cursor.getString(_cursorIndexOfBankAccountNumber);
            final String _tmpProfilePhotoUri;
            _tmpProfilePhotoUri = _cursor.getString(_cursorIndexOfProfilePhotoUri);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new UserEntity(_tmpUserId,_tmpName,_tmpEmail,_tmpPhone,_tmpRole,_tmpKycStatus,_tmpPanNumber,_tmpAadhaarVerified,_tmpSelfieVerified,_tmpUpiId,_tmpBankAccountNumber,_tmpProfilePhotoUri,_tmpCreatedAt);
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
  public Object getUserByEmail(final String email,
      final Continuation<? super UserEntity> $completion) {
    final String _sql = "SELECT * FROM users WHERE email = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, email);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserEntity>() {
      @Override
      @Nullable
      public UserEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfKycStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "kycStatus");
          final int _cursorIndexOfPanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "panNumber");
          final int _cursorIndexOfAadhaarVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "aadhaarVerified");
          final int _cursorIndexOfSelfieVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "selfieVerified");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfBankAccountNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "bankAccountNumber");
          final int _cursorIndexOfProfilePhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profilePhotoUri");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final UserEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpKycStatus;
            _tmpKycStatus = _cursor.getString(_cursorIndexOfKycStatus);
            final String _tmpPanNumber;
            _tmpPanNumber = _cursor.getString(_cursorIndexOfPanNumber);
            final boolean _tmpAadhaarVerified;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAadhaarVerified);
            _tmpAadhaarVerified = _tmp != 0;
            final boolean _tmpSelfieVerified;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSelfieVerified);
            _tmpSelfieVerified = _tmp_1 != 0;
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpBankAccountNumber;
            _tmpBankAccountNumber = _cursor.getString(_cursorIndexOfBankAccountNumber);
            final String _tmpProfilePhotoUri;
            _tmpProfilePhotoUri = _cursor.getString(_cursorIndexOfProfilePhotoUri);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new UserEntity(_tmpUserId,_tmpName,_tmpEmail,_tmpPhone,_tmpRole,_tmpKycStatus,_tmpPanNumber,_tmpAadhaarVerified,_tmpSelfieVerified,_tmpUpiId,_tmpBankAccountNumber,_tmpProfilePhotoUri,_tmpCreatedAt);
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
  public Object getUserByPhone(final String phone,
      final Continuation<? super UserEntity> $completion) {
    final String _sql = "SELECT * FROM users WHERE phone = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, phone);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserEntity>() {
      @Override
      @Nullable
      public UserEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfKycStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "kycStatus");
          final int _cursorIndexOfPanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "panNumber");
          final int _cursorIndexOfAadhaarVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "aadhaarVerified");
          final int _cursorIndexOfSelfieVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "selfieVerified");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfBankAccountNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "bankAccountNumber");
          final int _cursorIndexOfProfilePhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profilePhotoUri");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final UserEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpKycStatus;
            _tmpKycStatus = _cursor.getString(_cursorIndexOfKycStatus);
            final String _tmpPanNumber;
            _tmpPanNumber = _cursor.getString(_cursorIndexOfPanNumber);
            final boolean _tmpAadhaarVerified;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAadhaarVerified);
            _tmpAadhaarVerified = _tmp != 0;
            final boolean _tmpSelfieVerified;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSelfieVerified);
            _tmpSelfieVerified = _tmp_1 != 0;
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpBankAccountNumber;
            _tmpBankAccountNumber = _cursor.getString(_cursorIndexOfBankAccountNumber);
            final String _tmpProfilePhotoUri;
            _tmpProfilePhotoUri = _cursor.getString(_cursorIndexOfProfilePhotoUri);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new UserEntity(_tmpUserId,_tmpName,_tmpEmail,_tmpPhone,_tmpRole,_tmpKycStatus,_tmpPanNumber,_tmpAadhaarVerified,_tmpSelfieVerified,_tmpUpiId,_tmpBankAccountNumber,_tmpProfilePhotoUri,_tmpCreatedAt);
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
  public Flow<List<UserEntity>> getUsersByRole(final String role) {
    final String _sql = "SELECT * FROM users WHERE role = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, role);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"users"}, new Callable<List<UserEntity>>() {
      @Override
      @NonNull
      public List<UserEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfKycStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "kycStatus");
          final int _cursorIndexOfPanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "panNumber");
          final int _cursorIndexOfAadhaarVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "aadhaarVerified");
          final int _cursorIndexOfSelfieVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "selfieVerified");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfBankAccountNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "bankAccountNumber");
          final int _cursorIndexOfProfilePhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profilePhotoUri");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<UserEntity> _result = new ArrayList<UserEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserEntity _item;
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpKycStatus;
            _tmpKycStatus = _cursor.getString(_cursorIndexOfKycStatus);
            final String _tmpPanNumber;
            _tmpPanNumber = _cursor.getString(_cursorIndexOfPanNumber);
            final boolean _tmpAadhaarVerified;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAadhaarVerified);
            _tmpAadhaarVerified = _tmp != 0;
            final boolean _tmpSelfieVerified;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSelfieVerified);
            _tmpSelfieVerified = _tmp_1 != 0;
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpBankAccountNumber;
            _tmpBankAccountNumber = _cursor.getString(_cursorIndexOfBankAccountNumber);
            final String _tmpProfilePhotoUri;
            _tmpProfilePhotoUri = _cursor.getString(_cursorIndexOfProfilePhotoUri);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new UserEntity(_tmpUserId,_tmpName,_tmpEmail,_tmpPhone,_tmpRole,_tmpKycStatus,_tmpPanNumber,_tmpAadhaarVerified,_tmpSelfieVerified,_tmpUpiId,_tmpBankAccountNumber,_tmpProfilePhotoUri,_tmpCreatedAt);
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
  public Flow<List<UserEntity>> getAllUsers() {
    final String _sql = "SELECT * FROM users";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"users"}, new Callable<List<UserEntity>>() {
      @Override
      @NonNull
      public List<UserEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfKycStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "kycStatus");
          final int _cursorIndexOfPanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "panNumber");
          final int _cursorIndexOfAadhaarVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "aadhaarVerified");
          final int _cursorIndexOfSelfieVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "selfieVerified");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfBankAccountNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "bankAccountNumber");
          final int _cursorIndexOfProfilePhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profilePhotoUri");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<UserEntity> _result = new ArrayList<UserEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserEntity _item;
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpKycStatus;
            _tmpKycStatus = _cursor.getString(_cursorIndexOfKycStatus);
            final String _tmpPanNumber;
            _tmpPanNumber = _cursor.getString(_cursorIndexOfPanNumber);
            final boolean _tmpAadhaarVerified;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAadhaarVerified);
            _tmpAadhaarVerified = _tmp != 0;
            final boolean _tmpSelfieVerified;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSelfieVerified);
            _tmpSelfieVerified = _tmp_1 != 0;
            final String _tmpUpiId;
            _tmpUpiId = _cursor.getString(_cursorIndexOfUpiId);
            final String _tmpBankAccountNumber;
            _tmpBankAccountNumber = _cursor.getString(_cursorIndexOfBankAccountNumber);
            final String _tmpProfilePhotoUri;
            _tmpProfilePhotoUri = _cursor.getString(_cursorIndexOfProfilePhotoUri);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new UserEntity(_tmpUserId,_tmpName,_tmpEmail,_tmpPhone,_tmpRole,_tmpKycStatus,_tmpPanNumber,_tmpAadhaarVerified,_tmpSelfieVerified,_tmpUpiId,_tmpBankAccountNumber,_tmpProfilePhotoUri,_tmpCreatedAt);
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
