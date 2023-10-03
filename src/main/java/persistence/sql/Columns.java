package persistence.sql;

import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Columns extends AbstractColumns {

    private final Map<String, Object> columnValues = new LinkedHashMap<>();

    public Columns(Class<?> clazz) {
        super(clazz);
    }

    public void addValues(Object instance) {
        if (instance == null) {
            throw new IllegalStateException("Entity is not set");
        }
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (addable(field)) {
                field.setAccessible(true);
                putColumnValue(field, instance);
            }
        }
    }

    public void addValue(Field field, Object instance) {
        if (addable(field)) {
            field.setAccessible(true);
            putColumnValue(field, instance);
        }
        super.addColumn(field);
    }

    private void putColumnValue(Field field, Object instance) {
        try {
            columnValues.put(field.getName(), field.get(instance));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected boolean addable(Field field) {
        return !field.isAnnotationPresent(Transient.class) && !field.isAnnotationPresent(OneToMany.class);
    }

    public Map<String, Object> getColumnValues() {
        return new LinkedHashMap<>(columnValues);
    }

    public Map<String, Field> getJoinColumns() {
        return allColumns.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isAnnotationPresent(OneToMany.class))
                .collect(Collectors.toMap(entry -> EntityUtils.getColumnName(entry.getValue()), Map.Entry::getValue));
    }
}
