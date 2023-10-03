package jdbc;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import persistence.config.EntityMetaData;
import persistence.sql.EntityUtils;

public class RowMapperImpl<T> implements RowMapper<T> {
    private final Class<T> clazz;
    private final Map<Class<?>, EntityMetaData<?>> entityMetaDataMap;

    public RowMapperImpl(Class<?> clazz, Map<Class<?>, EntityMetaData<?>> entityMetaDataMap) {
        this.clazz = (Class<T>) clazz;
        this.entityMetaDataMap = entityMetaDataMap;
    }

    @Override
    public T mapRow(ResultSet resultSet) throws SQLException {
        List<List<Map<String, Object>>> results = new ArrayList<>();
        while (resultSet.next()) {
            ResultSetMetaData metaData = resultSet.getMetaData();

            int columnCount = metaData.getColumnCount();
            List<Map<String, Object>> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String tableName = metaData.getTableName(i).toLowerCase();
                String columnName = metaData.getColumnName(i).toLowerCase();
                String column = tableName + "." + columnName;
                Object value = resultSet.getObject(column);
                row.add(Map.of(column, value));
            }
            results.add(row);
        }

        if (results.isEmpty()) {
            return null;
        }

        EntityMetaData<T> entityMetaData = (EntityMetaData<T>) entityMetaDataMap.get(clazz);
        Map<String, Field> entityFieldByColumnName = entityMetaData.getDeclaredField();
        T result = EntityUtils.getInstance(clazz);

        List<Map<String, Object>> row1 = results.get(0);
        for (Map<String, Object> columns : row1) {
            for (Entry<String, Object> column : columns.entrySet()) {
                String columnName = column.getKey();
                Object columnValue = column.getValue();
                Field field = entityFieldByColumnName.get(columnName);
                if (field != null) {
                    EntityUtils.setField(field, result, columnValue);
                }
            }
        }

        if (results.size() != 1) {
            Field oneToManyField = EntityUtils.getOneToManyFields(clazz).get(0);
            Class<?> subClassType = EntityUtils.getClassOfGenericType(oneToManyField);
            EntityMetaData<?> subMetaData = entityMetaDataMap.get(subClassType);
            Map<String, Field> subDeclaredFields = subMetaData.getDeclaredField();

            List<Object> subTableResults = new ArrayList<>();
            for (List<Map<String, Object>> rows : results) {
                Object instance = EntityUtils.getInstance(subClassType);
                for (Map<String, Object> columns : rows) {
                    for (Entry<String, Object> column : columns.entrySet()) {
                        String columnName = column.getKey();
                        Object columnValue = column.getValue();
                        Field field = subDeclaredFields.get(columnName);
                        if (field != null) {
                            EntityUtils.setField(field, instance, columnValue);
                        }
                    }
                }
                subTableResults.add(instance);
            }
            EntityUtils.setField(oneToManyField, result, subTableResults);
        }
        return result;
    }
}
