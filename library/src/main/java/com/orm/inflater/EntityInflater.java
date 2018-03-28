package com.orm.inflater;

import android.database.Cursor;
import android.util.Log;

import com.orm.Id;
import com.orm.SchemaGenerator;
import com.orm.SugarRecord;
import com.orm.inflater.field.*;
import com.orm.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Created by Łukasz Wesołowski on 03.08.2016.
 */
public class EntityInflater {
    private Cursor cursor;
    private Object object;
    private Object relationObject;
    private String relationFieldName;
    private Map<Object, Object> entitiesMap;

    public EntityInflater withCursor(Cursor cursor) {
        this.cursor = cursor;
        return this;
    }

    public EntityInflater withObject(Object object) {
        this.object = object;
        return this;
    }

    public EntityInflater withRelationObject(Object relationObject) {
        this.relationObject = relationObject;
        return this;
    }

    public EntityInflater withRelationFieldName(String relationFieldName) {
        this.relationFieldName = relationFieldName;
        return this;
    }

    public EntityInflater withEntitiesMap(Map<Object, Object> entitiesMap) {
        this.entitiesMap = entitiesMap;
        return this;
    }

    private static final String LOG_TAG = "Sugar";

    public void inflate() {
        Log.d(LOG_TAG, "Inflating class: "+object.getClass().getSimpleName());
        List<Field> columns = ReflectionUtil.getTableFields(object.getClass());
        Field id = SchemaGenerator.findAnnotatedField(object.getClass(), Id.class);
        if(id != null){
            Log.d(LOG_TAG, "ID field: "+id.getName());
            Object objectId = null;
            if(id.getType() == Integer.class){
                Log.d(LOG_TAG, "Integer id field found");
                objectId = cursor.getInt(cursor.getColumnIndex(id.getName()));
            }else if(id.getType() == Long.class){
                Log.d(LOG_TAG, "Long  id field found");
                objectId = cursor.getLong(cursor.getColumnIndex(id.getName()));
            }else if(id.getType() == String.class){
                Log.d(LOG_TAG, "String id field found");
                objectId = cursor.getString(cursor.getColumnIndex(id.getName()));
            }
//            Long objectId = cursor.getLong(cursor.getColumnIndex(("ID")));
            if (!entitiesMap.containsKey(object)) {
                Log.d(LOG_TAG, "Entities map contains object");
                if(objectId != null) {
                    Log.d(LOG_TAG, "Added object id: "+objectId);
                    entitiesMap.put(object, objectId);
                }else{
                    Log.d(LOG_TAG, "Object id is null");
                }
            }else{
                Log.d(LOG_TAG, "Failed to add id field, not found in entities map");
            }
        }

        FieldInflater fieldInflater;

        for (Field field : columns) {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();

            if (SugarRecord.isSugarEntity(fieldType)) {
                if (field.getName().equals(relationFieldName)) {
                    fieldInflater = new RelationEntityFieldInflater(field, cursor, object, fieldType, relationObject);
                } else {
                    fieldInflater = new EntityFieldInflater(field, cursor, object, fieldType);
                }
            } else if (fieldType.equals(List.class)) {
                fieldInflater = new ListFieldInflater(field, cursor, object, fieldType);
            } else {
                fieldInflater = new DefaultFieldInflater(field, cursor, object, fieldType);
            }

            fieldInflater.inflate();
        }
    }
}
