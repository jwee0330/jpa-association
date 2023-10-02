package persistence.sql.ddl;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import persistence.dialect.collection.IdGeneratedValueStrategyMap;
import persistence.sql.QueryBuilder;
public class DdlQueryBuilder extends QueryBuilder {
    private static final String CREATE_TABLE = "create table %s (%s);";
    private final IdGeneratedValueStrategyMap idGeneratedValueStrategyMap;
    private final JavaToSqlColumnParser javaToSqlColumnParser;


    public DdlQueryBuilder(JavaToSqlColumnParser javaToSqlColumnParser, Class<?> entity) {
        super(entity);
        this.idGeneratedValueStrategyMap = new IdGeneratedValueStrategyMap();
        this.javaToSqlColumnParser = javaToSqlColumnParser;
    }

    public Class<?> getClazz() {
        return super.clazz;
    }

    public String createTable(Map<String, List<Map<String, Field>>> mappedFieldByTable) {
        final String tableName = getTableName();

        final List<Map<String, Field>> mappedFields = mappedFieldByTable.get(tableName);
        if (mappedFields != null) {
            for (Map<String, Field> mappedField : mappedFields) {
                mappedField.forEach(columns::addColumn);
            }
        }
        String allColumns = addColumns(columns.getColumns()) + addConstraint(mappedFields);
        return String.format(CREATE_TABLE, tableName, allColumns);
    }

    public String dropTable() {
        final String tableName = getTableName();
        return String.format("drop table if exists %s;", tableName);
    }

    private String addConstraint(List<Map<String, Field>> mappedFields) {
        StringBuilder constraint = new StringBuilder();
        if (mappedFields != null) {
            for (Map<String, Field> mappedField : mappedFields) {
                mappedField.forEach((k, v) -> {
                    constraint.append("constraint fk_" + getTableName() + "_" + k.toLowerCase() +
                            " foreign key (" + k.toLowerCase() + ") references orders (id),");
                });
            }
        }
        constraint.append(" constraint pk_" +
                getTableName().toLowerCase() +
                " primary key (" +
                id.getName() +
                ")");
        return constraint.toString();
    }

    private String addColumns(Map<String, Field> columns) {
        StringBuilder sb = new StringBuilder();
        columns.forEach((key, value) -> {
            sb.append(key);
            sb.append(BLANK);
            sb.append(covertToSqlType(value));
            sb.append(COMMA);
        });
        return sb.toString();
    }

    private String covertToSqlType(Field field) {
        StringBuilder sb = new StringBuilder();
        final Class<?> fieldType = field.getType();
        sb.append(javaToSqlColumnParser.parse(fieldType));

        if (field.isAnnotationPresent(Column.class)) {
            final Column column = field.getAnnotation(Column.class);
            if (isStringType(fieldType)) {
                sb.append("(").append(column.length()).append(")");
            }
            if (column.nullable()) {
                sb.append(" null");
                return sb.toString();
            }
            sb.append(" not null");
        }
        if (field.isAnnotationPresent(GeneratedValue.class)) {
            final GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
            sb.append(BLANK).append(idGeneratedValueStrategyMap.get(generatedValue.strategy()));
        }
        return sb.toString();
    }

    private boolean isStringType(Class<?> type) {
        return type.getTypeName().equals("java.lang.String");
    }
}
