package persistence.config;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdbc.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.dialect.H2DbDialect;
import persistence.entity.EntityManager;
import persistence.entity.EntityPersister;
import persistence.entity.MyEntityManager;
import persistence.entity.MyEntityPersister;
import persistence.sql.ddl.DdlQueryBuilder;
import persistence.sql.ddl.JavaToSqlColumnParser;

public class PersistenceConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PersistenceConfiguration.class);
    private final JavaToSqlColumnParser javaToSqlColumnParser;
    private final List<DdlQueryBuilder> ddlQueryBuilders;
    private final JdbcTemplate jdbcTemplate;
    private final InitSchema initSchema;
    private final Map<String, List<Map<String, Field>>> mappedFieldByTable;
    private final EntityPersister entityPersister;
    private final EntityManager entityManager;

    public PersistenceConfiguration(List<Class<?>> classes, JdbcTemplate jdbcTemplate, InitSchema initSchema) {
        this.javaToSqlColumnParser = new JavaToSqlColumnParser(new H2DbDialect());
        this.ddlQueryBuilders = classes.stream().map(e -> new DdlQueryBuilder(javaToSqlColumnParser, e)).toList();
        this.jdbcTemplate = jdbcTemplate;
        this.initSchema = initSchema;
        this.mappedFieldByTable = new HashMap<>();
        this.entityPersister = new MyEntityPersister(jdbcTemplate, classes);
        this.entityManager = new MyEntityManager(entityPersister);
        initMappedFields(ddlQueryBuilders);
    }

    private void initMappedFields(List<DdlQueryBuilder> ddlQueryBuilders) {
        for (DdlQueryBuilder ddlQueryBuilder : ddlQueryBuilders) {
            Class<?> clazz = ddlQueryBuilder.getClazz();
            for (Field declaredField : clazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(OneToMany.class)) {
                    OneToMany oneToMany = declaredField.getAnnotation(OneToMany.class);
                    if (oneToMany.mappedBy().isBlank()) {
                        if (declaredField.isAnnotationPresent(JoinColumn.class)) {
                            JoinColumn joinColumn = declaredField.getAnnotation(JoinColumn.class);
                            String joinColumnName = joinColumn.name();
                            Type genericFieldType = declaredField.getGenericType();
                            if (genericFieldType instanceof ParameterizedType parameterizedType) {
                                Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();
                                for (Type fieldArgType : fieldArgTypes) {
                                    Class<?> fieldArgClass = (Class<?>) fieldArgType;
                                    if (fieldArgClass.isAnnotationPresent(Table.class)) {
                                        Table table = fieldArgClass.getAnnotation(Table.class);
                                        mappedFieldByTable.put(table.name(), List.of(Map.of(joinColumnName, declaredField)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void initiateDatabase() {
        if (initSchema == InitSchema.NONE) {
            return;
        }
        if (initSchema == InitSchema.DROP_AND_CREATE) {
            for (DdlQueryBuilder ddlQueryBuilder : ddlQueryBuilders) {
                String dropTable = ddlQueryBuilder.dropTable();
                jdbcTemplate.execute(dropTable);
            }
        }
        for (DdlQueryBuilder ddlQueryBuilder : ddlQueryBuilders) {
            String createTable = ddlQueryBuilder.createTable(mappedFieldByTable);
            logger.info(createTable);
            jdbcTemplate.execute(createTable);
        }
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}
