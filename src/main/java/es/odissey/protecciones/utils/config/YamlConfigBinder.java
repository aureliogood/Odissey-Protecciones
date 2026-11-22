package es.odissey.protecciones.utils.config;

import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Convierte una seccion YAML en un objeto usando las anotaciones {@link Path},
 * {@link SpecIntInRange} y {@link SpecDoubleInRange}. Las claves ausentes dejan el valor
 * por defecto del objeto intacto.
 */
public final class YamlConfigBinder {

    private YamlConfigBinder() {
    }

    public static <T> T bind(ConfigurationSection section, Supplier<T> factory) {
        T obj = factory.get();
        bindInto(obj, section);
        return obj;
    }

    public static void bindInto(Object obj, ConfigurationSection section) {
        if (obj == null || section == null) return;

        for (Field field : obj.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            String key = keyOf(field);
            if (key == null || key.isEmpty() || !section.isSet(key)) continue;

            Object value = read(section, key, field);
            if (value == null) continue;

            value = applySpecs(field, value);
            try {
                field.set(obj, value);
            } catch (IllegalAccessException | IllegalArgumentException ignored) {
            }
        }
    }

    private static String keyOf(Field field) {
        Path path = field.getAnnotation(Path.class);
        return path != null ? path.value() : field.getName();
    }

    private static Object read(ConfigurationSection section, String key, Field field) {
        Class<?> type = field.getType();

        if (List.class.isAssignableFrom(type)) {
            return readList(section, key, field);
        }

        Object raw = section.get(key);
        if (raw == null) return null;

        if (type == String.class) return String.valueOf(raw);
        if (type == int.class || type == Integer.class) return toInt(raw);
        if (type == long.class || type == Long.class) return toLong(raw);
        if (type == double.class || type == Double.class) return toDouble(raw);
        if (type == float.class || type == Float.class) return toFloat(raw);
        if (type == boolean.class || type == Boolean.class) return toBoolean(raw);
        return null;
    }

    private static Object readList(ConfigurationSection section, String key, Field field) {
        List<?> raw = section.getList(key);
        if (raw == null) return null;

        List<Object> out = new ArrayList<>();
        if (isNestedList(field)) {
            for (Object element : raw) {
                List<String> inner = new ArrayList<>();
                if (element instanceof List) {
                    for (Object value : (List<?>) element) {
                        inner.add(value == null ? "" : String.valueOf(value));
                    }
                } else if (element != null) {
                    inner.add(String.valueOf(element));
                }
                out.add(inner);
            }
        } else {
            Class<?> elementType = listElementType(field);
            for (Object element : raw) {
                Object converted = convertListElement(element, elementType);
                if (converted != null) out.add(converted);
            }
        }
        return out;
    }

    private static Class<?> listElementType(Field field) {
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType)) return String.class;
        Type[] arguments = ((ParameterizedType) generic).getActualTypeArguments();
        return arguments.length == 1 && arguments[0] instanceof Class<?>
                ? (Class<?>) arguments[0] : String.class;
    }

    private static Object convertListElement(Object value, Class<?> elementType) {
        if (value == null) return elementType == String.class ? "" : null;
        if (elementType == String.class) return String.valueOf(value);
        if (elementType == Integer.class || elementType == int.class) return toInt(value);
        if (elementType == Long.class || elementType == long.class) return toLong(value);
        if (elementType == Double.class || elementType == double.class) return toDouble(value);
        if (elementType == Float.class || elementType == float.class) return toFloat(value);
        if (elementType == Boolean.class || elementType == boolean.class) return toBoolean(value);
        return value;
    }
    private static boolean isNestedList(Field field) {
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType)) return false;

        Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
        if (args.length != 1 || !(args[0] instanceof ParameterizedType)) return false;

        Type raw = ((ParameterizedType) args[0]).getRawType();
        return raw instanceof Class && List.class.isAssignableFrom((Class<?>) raw);
    }

    private static Object applySpecs(Field field, Object value) {
        SpecIntInRange intRange = field.getAnnotation(SpecIntInRange.class);
        if (intRange != null && value instanceof Number) {
            int v = ((Number) value).intValue();
            return Math.max(intRange.min(), Math.min(intRange.max(), v));
        }

        SpecDoubleInRange doubleRange = field.getAnnotation(SpecDoubleInRange.class);
        if (doubleRange != null && value instanceof Number) {
            double v = ((Number) value).doubleValue();
            return Math.max(doubleRange.min(), Math.min(doubleRange.max(), v));
        }

        return value;
    }

    private static Integer toInt(Object raw) {
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw instanceof Boolean) return ((Boolean) raw) ? 1 : 0;
        if (raw instanceof String) {
            try {
                return (int) Double.parseDouble((String) raw);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long toLong(Object raw) {
        if (raw instanceof Number) return ((Number) raw).longValue();
        if (raw instanceof Boolean) return ((Boolean) raw) ? 1L : 0L;
        if (raw instanceof String) {
            try {
                return (long) Double.parseDouble((String) raw);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double toDouble(Object raw) {
        if (raw instanceof Number) return ((Number) raw).doubleValue();
        if (raw instanceof Boolean) return ((Boolean) raw) ? 1.0 : 0.0;
        if (raw instanceof String) {
            try {
                return Double.parseDouble((String) raw);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Float toFloat(Object raw) {
        Double value = toDouble(raw);
        return value == null ? null : value.floatValue();
    }

    private static Boolean toBoolean(Object raw) {
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        if (raw instanceof String) return Boolean.parseBoolean((String) raw);
        return null;
    }
}
