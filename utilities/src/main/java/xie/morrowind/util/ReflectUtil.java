package xie.morrowind.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Wrapper and extension of {@link java.lang.reflect}.<br/>
 * Features:<br/>
 * <ul>
 *     <li>Get all fields information of specified object.</li>
 *     <li>Get definition name string of specified class field.</li>
 * </ul>
 *
 * @version 1.0
 * @author morrowindxie
 */
public final class ReflectUtil {

    /**
     * Get object's fields information.
     * @param obj The object which you want to get all it's fields info.
     * @param includeFinal Whether include final fields.
     * @return The fields info string, each field occupies one line.
     */
    public static String getFieldsInfo(@NonNull Object obj, boolean includeFinal) {
        Class<?> cls = obj.getClass();
        Field[] fields = cls.getDeclaredFields();
        cls.getDeclaredMethods();

        Arrays.sort(fields, (lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Fields of [%s: ", cls.getSimpleName()));
        for(Field field : fields) {
            if(includeFinal || !Modifier.isFinal(field.getModifiers())) {
                sb.append("\n");
                sb.append("    ");
                Class<?> fieldClass = field.getType();
                String modifier = Modifier.toString(field.getModifiers());
                if(!modifier.isEmpty()) {
                    sb.append(modifier);
                    sb.append(" ");
                }
                sb.append(fieldClass.getSimpleName());
                if(List.class.isAssignableFrom(fieldClass)) { //Check if parents class of List.
                    Type type = field.getGenericType();
                    if(type instanceof ParameterizedType) { //Check if Generic class (泛型).
                        ParameterizedType pt = (ParameterizedType) type;
                        Class<?> parameterizedClass = (Class<?>)pt.getActualTypeArguments()[0]; //Get object of Generic class.
                        sb.append("<");
                        sb.append(parameterizedClass.getSimpleName());
                        sb.append(">");
                    }
                }
                sb.append(" ");
                sb.append(field.getName());
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    sb.append(" = ");
                    if(value == null) {
                        sb.append("null");
                    } else if(value instanceof Integer) {
                        Integer v = (Integer) value;
                        sb.append(v);
                        if(v >= 10 || v < -10) {
                            sb.append((" = 0x"));
                            sb.append(Integer.toHexString(v));
                        }
                    } else if(value instanceof Long) {
                        long v = (Long) value;
                        sb.append(v);
                        sb.append("L");
                        if(v >= 10 || v < -10) {
                            sb.append((" = 0x"));
                            sb.append(Long.toHexString(v));
                        }
                    } else if(value instanceof Float) {
                        sb.append(value);
                        sb.append("f");
                    } else if(value instanceof CharSequence) {
                        sb.append("\"");
                        sb.append(value);
                        sb.append("\"");
                    } else {
                        sb.append(value);
                    }
                } catch (IllegalArgumentException | IllegalAccessException | NullPointerException e) {
                    e.printStackTrace();
                }
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /**
     * Get the constant string field name which is decorated by "public static final".
     * @param cls Class who defines the field.
     * @param value Field value.
     * @param regex Regular expression help to match the field name, if null, the first found field will be returned.
     * @return The field name.
     */
    public static String getFieldName(@NonNull Class<?> cls, @NonNull String value, @Nullable String regex) {
        Pattern pattern = Pattern.compile(regex != null ? regex : ".*");
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if(!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                if (field.getType().equals(String.class)
                        && (Modifier.isPublic(field.getModifiers()))
                        && (Modifier.isStatic(field.getModifiers()))
                        && (Modifier.isFinal(field.getModifiers()))
                        && (Objects.equals(field.get(null), value))) {
                    String name = field.getName();
                    if (pattern.matcher(name).matches()) {
                        return name;
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return "UNKNOWN: " + value;
    }

    /**
     * Get the constant primitive int field name which is decorated by "public static final".
     * @param cls Class who defines the field.
     * @param value Field value.
     * @param regex Regular expression help to match the field name, if null, the first found field will be returned.
     * @return The field name.
     */
    public static String getFieldName(@NonNull Class<?> cls, int value, @Nullable String regex) {
        Pattern pattern = Pattern.compile(regex != null ? regex : ".*");
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if(!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                if (field.getType().equals(int.class)
                        && (Modifier.isPublic(field.getModifiers()))
                        && (Modifier.isStatic(field.getModifiers()))
                        && (Modifier.isFinal(field.getModifiers()))
                        && (field.getInt(null) == value)) {
                    String name = field.getName();
                    if (pattern.matcher(name).matches()) {
                        return name;
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return "UNKNOWN: " + value;
    }

    /**
     * Get the constant primitive long field name which is decorated by "public static final".
     * @param cls Class who defines the field.
     * @param value Field value.
     * @param regex Regular expression help to match the field name, if null, the first found field will be returned.
     * @return The field name.
     */
    public static String getFieldName(@NonNull Class<?> cls, long value, @Nullable String regex) {
        Pattern pattern = Pattern.compile(regex != null ? regex : ".*");
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if(!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                if (field.getType().equals(long.class)
                        && (Modifier.isPublic(field.getModifiers()))
                        && (Modifier.isStatic(field.getModifiers()))
                        && (Modifier.isFinal(field.getModifiers()))
                        && (field.getLong(null) == value)) {
                    String name = field.getName();
                    if (pattern.matcher(name).matches()) {
                        return name;
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return "UNKNOWN: " + value;
    }

    /**
     * Get the constant primitive float field name which is decorated by "public static final".
     * @param cls Class who defines the field.
     * @param value Field value.
     * @param regex Regular expression help to match the field name, if null, the first found field will be returned.
     * @return The field name.
     */
    public static String getFieldName(@NonNull Class<?> cls, float value, @Nullable String regex) {
        Pattern pattern = Pattern.compile(regex != null ? regex : ".*");
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if(!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                if (field.getType().equals(float.class)
                        && (Modifier.isPublic(field.getModifiers()))
                        && (Modifier.isStatic(field.getModifiers()))
                        && (Modifier.isFinal(field.getModifiers()))
                        && (field.getFloat(null) == value)) {
                    String name = field.getName();
                    if (pattern.matcher(name).matches()) {
                        return name;
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return "UNKNOWN: " + value;
    }

    /**
     * Get the constant primitive double field name which is decorated by "public static final".
     * @param cls Class who defines the field.
     * @param value Field value.
     * @param regex Regular expression help to match the field name, if null, the first found field will be returned.
     * @return The field name.
     */
    public static String getFieldName(@NonNull Class<?> cls, double value, @Nullable String regex) {
        Pattern pattern = Pattern.compile(regex != null ? regex : ".*");
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if(!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                if (field.getType().equals(double.class)
                        && (Modifier.isPublic(field.getModifiers()))
                        && (Modifier.isStatic(field.getModifiers()))
                        && (Modifier.isFinal(field.getModifiers()))
                        && (field.getDouble(null) == value)) {
                    String name = field.getName();
                    if (pattern.matcher(name).matches()) {
                        return name;
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return "UNKNOWN: " + value;
    }


    public static <T> T getPublicStaticFieldValue(@NonNull Class<?> objCls, @NonNull Class<T> fieldCls, @NonNull String fieldName) {
        try {
            Field field = objCls.getField(fieldName);
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                if (field.getType().equals(fieldCls)) {
                    if (byte.class.equals(fieldCls) || Byte.class.equals(fieldCls)) {
                        return (T) Byte.valueOf(field.getByte(null));
                    } else if (short.class.equals(fieldCls) || Short.class.equals(fieldCls)) {
                        return (T) Short.valueOf(field.getShort(null));
                    } else if (int.class.equals(fieldCls) || Integer.class.equals(fieldCls)) {
                        return (T) Integer.valueOf(field.getInt(null));
                    } else if (long.class.equals(fieldCls) || Long.class.equals(fieldCls)) {
                        return (T) Long.valueOf(field.getLong(null));
                    } else if (float.class.equals(fieldCls) || Float.class.equals(fieldCls)) {
                        return (T) Float.valueOf(field.getFloat(null));
                    } else if (double.class.equals(fieldCls) || Double.class.equals(fieldCls)) {
                        return (T) Double.valueOf(field.getDouble(null));
                    } else if (boolean.class.equals(fieldCls) || Boolean.class.equals(fieldCls)) {
                        return (T) Boolean.valueOf(field.getBoolean(null));
                    } else if (char.class.equals(fieldCls) || Character.class.equals(fieldCls)) {
                        return (T) Character.valueOf(field.getChar(null));
                    } else {
                        return (T) field.get(null);
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | NullPointerException | ExceptionInInitializerError e) {
            e.printStackTrace();
        }
        return null;
    }

}
