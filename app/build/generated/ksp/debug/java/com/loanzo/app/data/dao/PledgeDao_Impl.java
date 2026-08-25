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
import com.loanzo.app.data.entity.PledgeEntity;
import java.lang.Class;
import java.lang.Double;
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
public final class PledgeDao_Impl implements PledgeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PledgeEntity> __insertionAdapterOfPledgeEntity;

  private final EntityDeletionOrUpdateAdapter<PledgeEntity> __deletionAdapterOfPledgeEntity;

  private final EntityDeletionOrUpdateAdapter<PledgeEntity> __updateAdapterOfPledgeEntity;

  public PledgeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPledgeEntity = new EntityInsertionAdapter<PledgeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pledges` (`pledgeId`,`loanId`,`assetDescription`,`assetType`,`estimatedValue`,`weight`,`photoUri`,`receiptUri`,`receiptStatus`,`notes`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PledgeEntity entity) {
        statement.bindString(1, entity.getPledgeId());
        statement.bindString(2, entity.getLoanId());
        statement.bindString(3, entity.getAssetDescription());
        statement.bindString(4, entity.getAssetType());
        statement.bindDouble(5, entity.getEstimatedValue());
        statement.bindDouble(6, entity.getWeight());
        statement.bindString(7, entity.getPhotoUri());
        statement.bindString(8, entity.getReceiptUri());
        statement.bindString(9, entity.getReceiptStatus());
        statement.bindString(10, entity.getNotes());
        statement.bindLong(11, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfPledgeEntity = new EntityDeletionOrUpdateAdapter<PledgeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `pledges` WHERE `pledgeId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PledgeEntity entity) {
        statement.bindString(1, entity.getPledgeId());
      }
    };
    this.__updateAdapterOfPledgeEntity = new EntityDeletionOrUpdateAdapter<PledgeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `pledges` SET `pledgeId` = ?,`loanId` = ?,`assetDescription` = ?,`assetType` = ?,`estimatedValue` = ?,`weight` = ?,`photoUri` = ?,`receiptUri` = ?,`receiptStatus` = ?,`notes` = ?,`createdAt` = ? WHERE `pledgeId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PledgeEntity entity) {
        statement.bindString(1, entity.getPledgeId());
        statement.bindString(2, entity.getLoanId());
        statement.bindString(3, entity.getAssetDescription());
        statement.bindString(4, entity.getAssetType());
        statement.bindDouble(5, entity.getEstimatedValue());
        statement.bindDouble(6, entity.getWeight());
        statement.bindString(7, entity.getPhotoUri());
        statement.bindString(8, entity.getReceiptUri());
        statement.bindString(9, entity.getReceiptStatus());
        statement.bindString(10, entity.getNotes());
        statement.bindLong(11, entity.getCreatedAt());
        statement.bindString(12, entity.getPledgeId());
      }
    };
  }

  @Override
  public Object insertPledge(final PledgeEntity pledge,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPledgeEntity.insert(pledge);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePledge(final PledgeEntity pledge,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPledgeEntity.handle(pledge);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePledge(final PledgeEntity pledge,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPledgeEntity.handle(pledge);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPledgeById(final String id,
      final Continuation<? super PledgeEntity> $completion) {
    final String _sql = "SELECT * FROM pledges WHERE pledgeId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PledgeEntity>() {
      @Override
      @Nullable
      public PledgeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPledgeId = CursorUtil.getColumnIndexOrThrow(_cursor, "pledgeId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAssetDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "assetDescription");
          final int _cursorIndexOfAssetType = CursorUtil.getColumnIndexOrThrow(_cursor, "assetType");
          final int _cursorIndexOfEstimatedValue = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedValue");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUri");
          final int _cursorIndexOfReceiptUri = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptUri");
          final int _cursorIndexOfReceiptStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptStatus");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final PledgeEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPledgeId;
            _tmpPledgeId = _cursor.getString(_cursorIndexOfPledgeId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpAssetDescription;
            _tmpAssetDescription = _cursor.getString(_cursorIndexOfAssetDescription);
            final String _tmpAssetType;
            _tmpAssetType = _cursor.getString(_cursorIndexOfAssetType);
            final double _tmpEstimatedValue;
            _tmpEstimatedValue = _cursor.getDouble(_cursorIndexOfEstimatedValue);
            final double _tmpWeight;
            _tmpWeight = _cursor.getDouble(_cursorIndexOfWeight);
            final String _tmpPhotoUri;
            _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            final String _tmpReceiptUri;
            _tmpReceiptUri = _cursor.getString(_cursorIndexOfReceiptUri);
            final String _tmpReceiptStatus;
            _tmpReceiptStatus = _cursor.getString(_cursorIndexOfReceiptStatus);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new PledgeEntity(_tmpPledgeId,_tmpLoanId,_tmpAssetDescription,_tmpAssetType,_tmpEstimatedValue,_tmpWeight,_tmpPhotoUri,_tmpReceiptUri,_tmpReceiptStatus,_tmpNotes,_tmpCreatedAt);
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
  public Flow<List<PledgeEntity>> getPledgesByLoan(final String loanId) {
    final String _sql = "SELECT * FROM pledges WHERE loanId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pledges"}, new Callable<List<PledgeEntity>>() {
      @Override
      @NonNull
      public List<PledgeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPledgeId = CursorUtil.getColumnIndexOrThrow(_cursor, "pledgeId");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAssetDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "assetDescription");
          final int _cursorIndexOfAssetType = CursorUtil.getColumnIndexOrThrow(_cursor, "assetType");
          final int _cursorIndexOfEstimatedValue = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedValue");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUri");
          final int _cursorIndexOfReceiptUri = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptUri");
          final int _cursorIndexOfReceiptStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptStatus");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<PledgeEntity> _result = new ArrayList<PledgeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PledgeEntity _item;
            final String _tmpPledgeId;
            _tmpPledgeId = _cursor.getString(_cursorIndexOfPledgeId);
            final String _tmpLoanId;
            _tmpLoanId = _cursor.getString(_cursorIndexOfLoanId);
            final String _tmpAssetDescription;
            _tmpAssetDescription = _cursor.getString(_cursorIndexOfAssetDescription);
            final String _tmpAssetType;
            _tmpAssetType = _cursor.getString(_cursorIndexOfAssetType);
            final double _tmpEstimatedValue;
            _tmpEstimatedValue = _cursor.getDouble(_cursorIndexOfEstimatedValue);
            final double _tmpWeight;
            _tmpWeight = _cursor.getDouble(_cursorIndexOfWeight);
            final String _tmpPhotoUri;
            _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            final String _tmpReceiptUri;
            _tmpReceiptUri = _cursor.getString(_cursorIndexOfReceiptUri);
            final String _tmpReceiptStatus;
            _tmpReceiptStatus = _cursor.getString(_cursorIndexOfReceiptStatus);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new PledgeEntity(_tmpPledgeId,_tmpLoanId,_tmpAssetDescription,_tmpAssetType,_tmpEstimatedValue,_tmpWeight,_tmpPhotoUri,_tmpReceiptUri,_tmpReceiptStatus,_tmpNotes,_tmpCreatedAt);
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
  public Flow<Double> getTotalPledgeValueForLoan(final String loanId) {
    final String _sql = "SELECT SUM(estimatedValue) FROM pledges WHERE loanId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pledges"}, new Callable<Double>() {
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
