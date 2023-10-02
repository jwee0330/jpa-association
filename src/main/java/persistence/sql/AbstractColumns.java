package persistence.sql;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import static persistence.sql.EntityUtils.getColumnName;

public abstract class AbstractColumns {
    protected final Map<String, Field> columns = new LinkedHashMap<>();

    public AbstractColumns(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            this.addColumn(field);
        }
    }

    public void addColumn(Field field) {
        if (addable(field)) {
            columns.put(getColumnName(field), field);
        }
    }

    public void addColumn(String columnName, Field field) {
        columns.put(columnName, field);
    }

    protected abstract boolean addable(Field field);

    public Set<String> getColumnNames() {
        return columns.keySet();
    }

    public Map<String, Field> getColumns() {
        return new LinkedHashMap<>(columns);
    }
}
