package persistence.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jdbc.JdbcTemplate;
import jdbc.RowMapper;
import jdbc.RowMapperImpl;
import persistence.config.EntityMetaData;
import persistence.sql.Entity;
import persistence.sql.Id;
import persistence.sql.dml.DmlQueryBuilder;

public class MyEntityPersister implements EntityPersister {
    private final Map<Long, Entity> entitySnapshotsByKey = new ConcurrentHashMap<>();
    private final Map<Class<?> , RowMapper<?>> rowMappers;
    private final JdbcTemplate jdbcTemplate;
    private final Map<Class<?>, EntityMetaData<?>> entityMetaDataMap;

    public MyEntityPersister(JdbcTemplate jdbcTemplate, List<Class<?>> classes) {
        this.jdbcTemplate = jdbcTemplate;
        final HashMap<Class<?>, RowMapper<?>> rowMappers = new HashMap<>();
        this.entityMetaDataMap = classes.stream().map(EntityMetaData::new).collect(Collectors.toMap(EntityMetaData::getClazz, e -> e));
        this.rowMappers = rowMappers;
        classes.forEach(clazz -> rowMappers.put(clazz, new RowMapperImpl<>(clazz, entityMetaDataMap)));
    }

    @Override
    public Object getDatabaseSnapshot(Long id, Object entity) {
        final DmlQueryBuilder<?> queryBuilder = new DmlQueryBuilder<>(entity.getClass(), this);
        final String sql = queryBuilder.findById(id);
        Object instance = jdbcTemplate.queryForObject(sql, rowMappers.get(entity.getClass()));
        if (instance == null) {
            return null;
        }
        entitySnapshotsByKey.put(id, new Entity(instance));
        return instance;
    }

    @Override
    public Object getCachedDatabaseSnapshot(Long id) {
        Entity entity = entitySnapshotsByKey.get(id);
        if (entity == null) {
            return null;
        }
        return entity.getEntity();
    }

    @Override
    public Object load(Class<?> clazz, Long id) {
        Object instance = getCachedDatabaseSnapshot(id);
        if (instance != null) {
            return instance;
        }
        try {
            Constructor<?> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            instance = getDatabaseSnapshot(id, declaredConstructor.newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        if (instance == null) {
            throw new IllegalArgumentException("ObjectNotFoundException");
        }
        return instance;
    }

    @Override
    public void insert(Object entity) {
        final Class<?> clazz = entity.getClass();
        final DmlQueryBuilder<?> dmlQueryBuilder = new DmlQueryBuilder<>(clazz, this);
        final String sql = dmlQueryBuilder.insert(entity);

        Object key = jdbcTemplate.executeAndReturnKey(sql);
        entitySnapshotsByKey.put((Long) key, new Entity(entity));
    }

    @Override
    public void update(Object entity) {
        Entity snapshot = entitySnapshotsByKey.get((Long) new Id(entity).getValue());
        if (snapshot == null) {
            final Class<?> clazz = entity.getClass();
            final DmlQueryBuilder<?> dmlQueryBuilder = new DmlQueryBuilder<>(clazz, this);
            final String sql = dmlQueryBuilder.update(entity);
            Object key = jdbcTemplate.executeAndReturnKey(sql);
            entitySnapshotsByKey.put((Long) key, new Entity(entity));
        }

        Object snapshotEntity = Objects.requireNonNull(snapshot).getEntity();

        Map<Field, Object> valuesByColumnName = new HashMap<>();
        for (Entry<String, Field> entry : new Entity(entity).getColumns().entrySet()) {
            Field field = entry.getValue();
            field.setAccessible(true);
            Object entityValue;
            try {
                entityValue = field.get(entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            Object snapshotEntityValue;
            try {
                snapshotEntityValue = field.get(snapshotEntity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (entityValue != null && !entityValue.equals(snapshotEntityValue)) {
                valuesByColumnName.put(field, entityValue);
            }
        }
        if (!valuesByColumnName.isEmpty()) {
            final DmlQueryBuilder<?> dmlQueryBuilder = new DmlQueryBuilder<>(entity.getClass(), this);
            final String sql = dmlQueryBuilder.update(entity, valuesByColumnName);
            jdbcTemplate.execute(sql);
        }
    }

    @Override
    public void delete(Object entity) {
        entitySnapshotsByKey.remove((Long) new Id(entity).getValue());
        final String deleteSql = new DmlQueryBuilder<>(entity.getClass(), this).delete(entity);
        jdbcTemplate.execute(deleteSql);
    }

    @Override
    public Map<Class<?>, EntityMetaData<?>> getEntityMetaDataMap() {
        return entityMetaDataMap;
    }
}
