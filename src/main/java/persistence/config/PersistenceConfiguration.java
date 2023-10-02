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
import persistence.sql.ddl.DdlQueryBuilder;

public class PersistenceConfiguration {
    private final List<DdlQueryBuilder> ddlQueryBuilders;
    private final JdbcTemplate jdbcTemplate;
    private final InitSchema initSchema;
    private final Map<String, List<Map<String, Field>>> mappedFieldByTable;

    public PersistenceConfiguration(List<DdlQueryBuilder> ddlQueryBuilders, JdbcTemplate jdbcTemplate, InitSchema initSchema) {
        this.ddlQueryBuilders = ddlQueryBuilders;
        this.jdbcTemplate = jdbcTemplate;
        this.initSchema = initSchema;
        this.mappedFieldByTable = new HashMap<>();
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
                                    System.out.println("Type: " + fieldArgClass.getName());
                                    if (fieldArgClass.isAnnotationPresent(Table.class)) {
                                        Table table = fieldArgClass.getAnnotation(Table.class);
                                        String name = table.name();
                                        System.out.println("table name: " + name);
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
            System.out.println(createTable);
            jdbcTemplate.execute(createTable);
        }
    }
}
