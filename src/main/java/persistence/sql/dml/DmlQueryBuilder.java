package persistence.sql.dml;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import persistence.config.EntityMetaData;
import persistence.entity.EntityPersister;
import persistence.sql.Entity;
import persistence.sql.EntityUtils;
import persistence.sql.Id;
import persistence.sql.QueryBuilder;

public class DmlQueryBuilder<T> extends QueryBuilder {
    private static final String INSERT_QUERY = "insert into %s (%s) values (%s)";
    private static final String UPDATE_QUERY = "update %s set %s ";
    private static final String DELETE_QUERY = "delete from %s %s";
    private static final String SELECT_CLAUSE = "select %s";
    private static final String FROM_CLAUSE = "from %s";
    private static final String INNER_JOIN_CLAUSE = "inner join %s on %s";
    private static final String LEFT_JOIN_CLAUSE = "left join %s on %s";
    private static final String WHERE_CLAUSE = "where %s";

    private final EntityPersister entityPersister;

    public DmlQueryBuilder(Class<T> entity, EntityPersister entityPersister) {
        super(entity);
        this.entityPersister = entityPersister;
    }

    public String insert(Object instance) {
        final String tableName = getTableName();
        final Entity target = new Entity(instance);
        return String.format(
                INSERT_QUERY,
                tableName,
                joinWithComma(columns.getColumnNames()),
                joinWithComma(target.getValues())
        );
    }

    public String update(Object entity) {
        final String tableName = getTableName();
        final Entity target = new Entity(entity);
        return String.format(
                UPDATE_QUERY,
                tableName,
                target.valuesByField().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(","))
        ) + whereClause(new Id(entity));
    }

    public String update(Object entity, Map<Field, Object> valuesByColumnName) {
        final String tableName = getTableName();
        return String.format(
                UPDATE_QUERY,
                tableName,
                valuesByColumnName.entrySet().stream()
                        .map(e -> EntityUtils.getColumnName(e.getKey()) + "=" + e.getValue())
                        .collect(Collectors.joining(","))
        ) + whereClause(new Id(entity));
    }

    public String findAll() {
        return selectClause() +
                BLANK +
                fromClause();
    }

    public String findById(Object value) {
        final Id id = new Id(this.id.getName(), value);
        Map<Class<?>, EntityMetaData<?>> entityMetaDataMap = entityPersister.getEntityMetaDataMap();
        EntityMetaData<?> entityMetaData = entityMetaDataMap.get(clazz);
        Map<String, FetchType> fetchTypes = entityMetaData.getFetchTypes();
        Map<String, Field> collectionFields = columns.getJoinColumns();

        for (Map.Entry<String, Field> entry : collectionFields.entrySet()) {
            FetchType fetchType = fetchTypes.get(entry.getKey());
            if (fetchType == FetchType.EAGER) {
                return selectClause(entityMetaData.getColumns().keySet()) + BLANK + fromClause(entry.getKey(), entry.getValue()) + BLANK + whereClause(id);
            }
        }
        return selectClause() + BLANK + fromClause() + BLANK + whereClause(id);
    }

    private String selectClause(Collection<String> columns) {
        return String.format(SELECT_CLAUSE, joinWithComma(columns));
    }

    private String find(String joinTableName, Id id) {
        return selectClause() + BLANK + fromClause() + whereClause(Map.of(joinTableName, id.getValue()));
    }

    public <T> String delete(T instance) {
        String tableName = getTableName();
        final Id id = new Id(instance);
        String whereClause = whereClause(id);
        return String.format(DELETE_QUERY, tableName, whereClause);
    }

    private String whereClause(Id id) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTableName())
                .append(".")
                .append(id.getName())
                .append("=")
                .append(id.getValue());
        return String.format(WHERE_CLAUSE, sb);
    }

    private String whereClause(Map<String, Object> whereConditions) {
        StringBuilder sb = new StringBuilder();
        String conditions = whereConditions.entrySet().stream()
                .map(e -> {
                    if (e.getValue() instanceof String) {
                        return getTableName() + "." + e.getKey() + "='" + e.getValue() + "'";
                    } else {
                        return getTableName() + "." + e.getKey() + "=" + e.getValue();
                    }
                }).collect(Collectors.joining(","));

        sb.append(conditions);
        return String.format(WHERE_CLAUSE, sb);
    }

    private String selectClause() {
        return String.format(SELECT_CLAUSE, joinWithComma(columns.getColumnNames()));
    }

    private String selectAsterisk() {
        return String.format(SELECT_CLAUSE, "*");
    }

    private String fromClause() {
        return String.format(FROM_CLAUSE, getTableName());
    }

    private String fromClause(String joinTable, Field joinColumnField) {
        JoinColumn annotation = joinColumnField.getAnnotation(JoinColumn.class);
        if (annotation != null) {
            String joinColumnName = annotation.name();
            String from = String.format(FROM_CLAUSE, getTableName());
            String join = String.format(INNER_JOIN_CLAUSE, joinTable, getTableName() + "." + id.getName() + "=" + joinTable + "." + joinColumnName);
            return from + BLANK + join;
        }
        return fromClause();
    }
}
