package com.loanzo.app.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.loanzo.app.data.entity.AuditEventEntity;
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
public final class AuditEventDao_Impl implements AuditEventDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AuditEventEntity> __insertionAdapterOfAuditEventEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldEvents;

  public AuditEventDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAuditEventEntity = new EntityInsertionAdapter<AuditEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `audit_events` (`eventId`,`entityType`,`entityId`,`actor`,`event`,`oldState`,`newState`,`description`,`timestamp`,`reference`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AuditEventEntity entity) {
        statement.bindString(1, entity.getEventId());
        statement.bindString(2, entity.getEntityType());
        statement.bindString(3, entity.getEntityId());
        statement.bindString(4, entity.getActor());
        statement.bindString(5, entity.getEvent());
        statement.bindString(6, entity.getOldState());
        statement.bindString(7, entity.getNewState());
        statement.bindString(8, entity.getDescription());
        statement.bindLong(9, entity.getTimestamp());
        statement.bindString(10, entity.getReference());
      }
    };
    this.__preparedStmtOfDeleteOldEvents = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM audit_events WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertEvent(final AuditEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAuditEventEntity.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldEvents(final long beforeTimestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldEvents.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, beforeTimestamp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOldEvents.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AuditEventEntity>> getEventsForEntity(final String entityType,
      final String entityId) {
    final String _sql = "SELECT * FROM audit_events WHERE entityType = ? AND entityId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, entityType);
    _argIndex = 2;
    _statement.bindString(_argIndex, entityId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"audit_events"}, new Callable<List<AuditEventEntity>>() {
      @Override
      @NonNull
      public List<AuditEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "eventId");
          final int _cursorIndexOfEntityType = CursorUtil.getColumnIndexOrThrow(_cursor, "entityType");
          final int _cursorIndexOfEntityId = CursorUtil.getColumnIndexOrThrow(_cursor, "entityId");
          final int _cursorIndexOfActor = CursorUtil.getColumnIndexOrThrow(_cursor, "actor");
          final int _cursorIndexOfEvent = CursorUtil.getColumnIndexOrThrow(_cursor, "event");
          final int _cursorIndexOfOldState = CursorUtil.getColumnIndexOrThrow(_cursor, "oldState");
          final int _cursorIndexOfNewState = CursorUtil.getColumnIndexOrThrow(_cursor, "newState");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final List<AuditEventEntity> _result = new ArrayList<AuditEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AuditEventEntity _item;
            final String _tmpEventId;
            _tmpEventId = _cursor.getString(_cursorIndexOfEventId);
            final String _tmpEntityType;
            _tmpEntityType = _cursor.getString(_cursorIndexOfEntityType);
            final String _tmpEntityId;
            _tmpEntityId = _cursor.getString(_cursorIndexOfEntityId);
            final String _tmpActor;
            _tmpActor = _cursor.getString(_cursorIndexOfActor);
            final String _tmpEvent;
            _tmpEvent = _cursor.getString(_cursorIndexOfEvent);
            final String _tmpOldState;
            _tmpOldState = _cursor.getString(_cursorIndexOfOldState);
            final String _tmpNewState;
            _tmpNewState = _cursor.getString(_cursorIndexOfNewState);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            _item = new AuditEventEntity(_tmpEventId,_tmpEntityType,_tmpEntityId,_tmpActor,_tmpEvent,_tmpOldState,_tmpNewState,_tmpDescription,_tmpTimestamp,_tmpReference);
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
  public Flow<List<AuditEventEntity>> getRecentEventsByUser(final String userId, final int limit) {
    final String _sql = "SELECT * FROM audit_events WHERE actor = ? ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"audit_events"}, new Callable<List<AuditEventEntity>>() {
      @Override
      @NonNull
      public List<AuditEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "eventId");
          final int _cursorIndexOfEntityType = CursorUtil.getColumnIndexOrThrow(_cursor, "entityType");
          final int _cursorIndexOfEntityId = CursorUtil.getColumnIndexOrThrow(_cursor, "entityId");
          final int _cursorIndexOfActor = CursorUtil.getColumnIndexOrThrow(_cursor, "actor");
          final int _cursorIndexOfEvent = CursorUtil.getColumnIndexOrThrow(_cursor, "event");
          final int _cursorIndexOfOldState = CursorUtil.getColumnIndexOrThrow(_cursor, "oldState");
          final int _cursorIndexOfNewState = CursorUtil.getColumnIndexOrThrow(_cursor, "newState");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final List<AuditEventEntity> _result = new ArrayList<AuditEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AuditEventEntity _item;
            final String _tmpEventId;
            _tmpEventId = _cursor.getString(_cursorIndexOfEventId);
            final String _tmpEntityType;
            _tmpEntityType = _cursor.getString(_cursorIndexOfEntityType);
            final String _tmpEntityId;
            _tmpEntityId = _cursor.getString(_cursorIndexOfEntityId);
            final String _tmpActor;
            _tmpActor = _cursor.getString(_cursorIndexOfActor);
            final String _tmpEvent;
            _tmpEvent = _cursor.getString(_cursorIndexOfEvent);
            final String _tmpOldState;
            _tmpOldState = _cursor.getString(_cursorIndexOfOldState);
            final String _tmpNewState;
            _tmpNewState = _cursor.getString(_cursorIndexOfNewState);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            _item = new AuditEventEntity(_tmpEventId,_tmpEntityType,_tmpEntityId,_tmpActor,_tmpEvent,_tmpOldState,_tmpNewState,_tmpDescription,_tmpTimestamp,_tmpReference);
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
  public Flow<List<AuditEventEntity>> getRecentEvents(final int limit) {
    final String _sql = "SELECT * FROM audit_events ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"audit_events"}, new Callable<List<AuditEventEntity>>() {
      @Override
      @NonNull
      public List<AuditEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "eventId");
          final int _cursorIndexOfEntityType = CursorUtil.getColumnIndexOrThrow(_cursor, "entityType");
          final int _cursorIndexOfEntityId = CursorUtil.getColumnIndexOrThrow(_cursor, "entityId");
          final int _cursorIndexOfActor = CursorUtil.getColumnIndexOrThrow(_cursor, "actor");
          final int _cursorIndexOfEvent = CursorUtil.getColumnIndexOrThrow(_cursor, "event");
          final int _cursorIndexOfOldState = CursorUtil.getColumnIndexOrThrow(_cursor, "oldState");
          final int _cursorIndexOfNewState = CursorUtil.getColumnIndexOrThrow(_cursor, "newState");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final List<AuditEventEntity> _result = new ArrayList<AuditEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AuditEventEntity _item;
            final String _tmpEventId;
            _tmpEventId = _cursor.getString(_cursorIndexOfEventId);
            final String _tmpEntityType;
            _tmpEntityType = _cursor.getString(_cursorIndexOfEntityType);
            final String _tmpEntityId;
            _tmpEntityId = _cursor.getString(_cursorIndexOfEntityId);
            final String _tmpActor;
            _tmpActor = _cursor.getString(_cursorIndexOfActor);
            final String _tmpEvent;
            _tmpEvent = _cursor.getString(_cursorIndexOfEvent);
            final String _tmpOldState;
            _tmpOldState = _cursor.getString(_cursorIndexOfOldState);
            final String _tmpNewState;
            _tmpNewState = _cursor.getString(_cursorIndexOfNewState);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            _item = new AuditEventEntity(_tmpEventId,_tmpEntityType,_tmpEntityId,_tmpActor,_tmpEvent,_tmpOldState,_tmpNewState,_tmpDescription,_tmpTimestamp,_tmpReference);
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
