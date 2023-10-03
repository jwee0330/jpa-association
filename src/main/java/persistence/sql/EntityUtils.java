package persistence.sql;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityUtils {
    public static String getName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            final Table table = clazz.getAnnotation(Table.class);
            if (table.name().isBlank()) {
                return clazz.getSimpleName().toLowerCase();
            }
            return table.name();
        }
        return clazz.getSimpleName().toLowerCase();
    }

    public static String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            final Column column = field.getAnnotation(Column.class);
            if (!column.name().isBlank()) {
                return column.name();
            }
        }
        if (field.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (!joinColumn.name().isBlank()) {
                Class<?> genericType = EntityUtils.getClassOfGenericType(field);
                if (genericType != null) {
                    Table table = genericType.getAnnotation(Table.class);
                    if (table != null) {
                        return table.name();
                    }
                }
            }
        }
        return field.getName().toLowerCase();
    }

    public static Class<?> getClassOfGenericType(Field field) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType parameterizedType) {
            Type fieldArgType = parameterizedType.getActualTypeArguments()[0];
            return (Class<?>) fieldArgType;
        }
        throw new RuntimeException("Can't get generic type of " + field);
    }

    public static <T> T getInstance(Class<T> clazz) {
        try {
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setField(Field field, Object instance, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> String getTableNameWithDot(Class<T> clazz) {
        String name = clazz.getAnnotation(Table.class).name();
        if (name.isBlank()) {
            return clazz.getSimpleName().toLowerCase() + ".";
        }
        return name + ".";
    }

    public static <T> List<Field> getOneToManyFields(Class<T> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(OneToMany.class))
                .collect(Collectors.toList());
    }
}
