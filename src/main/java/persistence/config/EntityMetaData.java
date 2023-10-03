package persistence.config;

import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import persistence.sql.EntityUtils;

public class EntityMetaData<T> {
    private final Class<T> clazz;
    private final LinkedHashMap<String, Class<T>> columns;
    private final LinkedHashMap<String, List<String>> columnMappings;
    private final Map<String, Class<T>> tableMappings;
    private final boolean needJoinTable;
    private final Map<String, FetchType> fetchTypes;

    public EntityMetaData(Class<T> clazz) {
        this.clazz = clazz;
        this.columns = checkAndSetColumns(clazz);;
        this.columnMappings = checkColumnMappings(clazz);
        this.needJoinTable = checkHasJoinTable(clazz);
        this.fetchTypes = checkFetchType(clazz);
        this.tableMappings = checkAndSetTableMappings(clazz);
    }

    private Map<String, Class<T>> checkAndSetTableMappings(Class<T> clazz) {
        String tableName = clazz.getAnnotation(Table.class).name();
        if (tableName.isBlank()) {
            tableName = clazz.getSimpleName().toLowerCase();
        }
        return Map.of(tableName, clazz);
    }

    private LinkedHashMap<String, List<String>> checkColumnMappings(Class<T> clazz) {
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        for (var field : clazz.getDeclaredFields()) {
            String tableName = clazz.getAnnotation(Table.class).name();
            if (!tableName.isBlank()) {
                tableName = tableName + ".";
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                String joinColumnName = field.getAnnotation(JoinColumn.class).name();
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                if (!oneToMany.mappedBy().isBlank()) {
                    continue;
                }
                Class<?> genericType = EntityUtils.getClassOfGenericType(field);
                if (genericType == null) {
                    continue;
                }
                String subTableName = genericType.getAnnotation(Table.class).name();
                if (!subTableName.isBlank()) {
                    subTableName = subTableName + ".";
                }
                List<String> subTableColumns = result.getOrDefault(field.getName(), new ArrayList<>());
                Field[] genericTypeDeclaredFields = genericType.getDeclaredFields();
                for (Field genericTypeDeclaredField : genericTypeDeclaredFields) {
                    if (genericTypeDeclaredField.isAnnotationPresent(Id.class)) {
                        subTableColumns.add(subTableName + joinColumnName);
                    } else {
                        subTableColumns.add(subTableName + EntityUtils.getColumnName(genericTypeDeclaredField));
                    }
                }
                result.put(field.getName(), subTableColumns);
            } else {
                result.put(field.getName(), new ArrayList<>(List.of(tableName + EntityUtils.getColumnName(field))));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> LinkedHashMap<String, Class<T>> checkAndSetColumns(Class<T> clazz) {
        LinkedHashMap<String, Class<T>> result = new LinkedHashMap<>();
        for (var field : clazz.getDeclaredFields()) {
            String tableName = clazz.getAnnotation(Table.class).name();
            if (!tableName.isBlank()) {
                tableName = tableName + ".";
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                String joinColumnName = field.getAnnotation(JoinColumn.class).name();
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                if (!oneToMany.mappedBy().isBlank()) {
                    continue;
                }
                Class<?> genericType = EntityUtils.getClassOfGenericType(field);
                if (genericType == null) {
                    continue;
                }
                String subTableName = genericType.getAnnotation(Table.class).name();
                if (!subTableName.isBlank()) {
                    subTableName = subTableName + ".";
                }
                Field[] genericTypeDeclaredFields = genericType.getDeclaredFields();
                for (Field genericTypeDeclaredField : genericTypeDeclaredFields) {
                    if (genericTypeDeclaredField.isAnnotationPresent(Id.class)) {
                        result.put(subTableName + joinColumnName, (Class<T>) genericTypeDeclaredField.getType());
                    }
                    result.put(subTableName + EntityUtils.getColumnName(genericTypeDeclaredField), (Class<T>) genericTypeDeclaredField.getType());
                }
            } else {
                result.put(tableName + EntityUtils.getColumnName(field), (Class<T>) field.getType());
            }
        }
        return result;
    }

    private Map<String, FetchType> checkFetchType(Class<T> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(OneToMany.class))
                .collect(Collectors.toMap(EntityUtils::getColumnName, field -> field.getAnnotation(OneToMany.class).fetch()));
    }

    private boolean checkHasJoinTable(Class<T> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(OneToMany.class));
    }

    public LinkedHashMap<String, Class<T>> getColumns() {
        return columns;
    }

    public LinkedHashMap<String, List<String>> getColumnMapping() {
        return columnMappings;
    }

    public Map<String, Class<T>> getTableMappings() {
        return tableMappings;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public boolean isNeedJoinTable() {
        return needJoinTable;
    }

    public Map<String, FetchType> getFetchTypes() {
        return fetchTypes;
    }

    public Map<String, Field> getDeclaredField() {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isAnnotationPresent(OneToMany.class))
                .collect(Collectors.toMap(field -> EntityUtils.getTableNameWithDot(clazz) + EntityUtils.getColumnName(field), field -> field));
    }
}
