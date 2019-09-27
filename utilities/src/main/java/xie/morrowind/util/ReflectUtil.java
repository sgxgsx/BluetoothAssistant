/*
 * Copyright (C) 2013 <MorrowindXie@gmail.com>.
 * This module is cloned from https://github.com/morrowind/ReflectUtil
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package xie.morrowind.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Wrapper and extension of {@link java.lang.reflect}.<br/>
 * Features:<br/>
 * <ul>
 *     <li>Get all fields information of specified object.</li>
 *     <li>Get definition name string of specified class field.</li>
 * </ul>
 *
 * @author morrowindxie
 * @version 1.0
 */
public final class ReflectUtil {

    /**
     * Get object's fields information, not include final fields.
     * @param obj The object which you want to get all it's fields info.
     * @return The fields info string, each field occupies one line.
     */
    public static String getFieldsInfo(Object obj) {
        return getFieldsInfo(obj, false);
    }
    
    /**
     * Get object's fields information.
     * @param obj The object which you want to get all it's fields info.
     * @param includeFinal Whether include final fields.
     * @return The fields info string, each field occupies one line.
     */
    public static String getFieldsInfo(Object obj, boolean includeFinal) {
        Class<?> cls = obj.getClass();
        Field[] fields = cls.getDeclaredFields();
        cls.getDeclaredMethods();

        Arrays.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field lhs, Field rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
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
                        Long v = (Long) value;
                        sb.append(v);
                        sb.append("L");
                        if(v >= 10 || v < -10) {
                            sb.append((" = 0x"));
                            sb.append(Long.toHexString(v));
                        }
                    } else if(value instanceof Float) {
                        sb.append(value.toString());
                        sb.append("f");
                    } else if(value instanceof CharSequence) {
                        sb.append("\"");
                        sb.append(value.toString());
                        sb.append("\"");
                    } else {
                        sb.append(value.toString());
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /**
     * Get definition name string of an integer class field.
     * @param cls Class who defines the field.
     * @param value Field value.
     * @param regex Regular expression for matching the field name.
     * @return The field name string.
     */
    public static String getFieldName(Class<?> cls, int value, String regex) {
        Field[] fields = cls.getDeclaredFields();
        for (Field f : fields) {
            if(!f.isAccessible()) {
                f.setAccessible(true);
            }
            try {
                if (f.getType().equals(int.class)
                        && (f.getModifiers() & Modifier.PUBLIC) != 0
                        && (f.getModifiers() & Modifier.STATIC) != 0 
                        && (f.getModifiers() & Modifier.FINAL) != 0
                        && (f.getInt(null) == value)) {
                    String name = f.getName();
                    if(regex == null || regex.isEmpty()) {
                        return name;
                    } else {
                        Pattern p = Pattern.compile(regex);
                        if(p.matcher(name).matches()) {
                            return name;
                        }
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return "UNKNOWN: " + value;
    }

    /**
     * Get definition name string of an integer class field.
     * @param cls Class who defines the field.
     * @param value Field value.
     * @return The field name string.
     */
    public static String getFieldName(Class<?> cls, int value) {
        return getFieldName(cls, value, null);
    }

    /**
     * Get definition name string of a long class field.
     * @param cls Class who defines the field.
     * @param value Field value.
     * @param regex Regular expression for matching the field name.
     * @return The field name string.
     */
    public static String getFieldName(Class<?> cls, long value, String regex) {
        Field[] fields = cls.getDeclaredFields();
        for (Field f : fields) {
            if(!f.isAccessible()) {
                f.setAccessible(true);
            }
            try {
                if (f.getType().equals(long.class)
                        && (f.getModifiers() & Modifier.PUBLIC) != 0
                        && (f.getModifiers() & Modifier.STATIC) != 0 
                        && (f.getModifiers() & Modifier.FINAL) != 0
                        && (f.getLong(null) == value)) {
                    String name = f.getName();
                    if(regex == null || regex.isEmpty()) {
                        return name;
                    } else {
                        Pattern p = Pattern.compile(regex);
                        if(p.matcher(name).matches()) {
                            return name;
                        }
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return "UNKNOWN: " + value;
    }

    /**
     * Get definition name string of a long class field.
     * @param cls Class who defines the field.
     * @param value Field value.
     * @return The field name string.
     */
    public static String getFieldName(Class<?> cls, long value) {
        return getFieldName(cls, value, null);
    }

    /**
     * Get definition name string of a string class field.
     * @param cls Class who defines the field.
     * @param value Field value.
     * @param regex Regular expression for matching the field name.
     * @return The field name string.
     */
    public static String getFieldName(Class<?> cls, String value, String regex) {
        Field[] fields = cls.getDeclaredFields();
        for (Field f : fields) {
            if(!f.isAccessible()) {
                f.setAccessible(true);
            }
            try {
                if (f.getType().equals(String.class)
                        && (f.getModifiers() & Modifier.PUBLIC) != 0
                        && (f.getModifiers() & Modifier.STATIC) != 0 
                        && (f.getModifiers() & Modifier.FINAL) != 0
                        && (f.get(null).equals(value))) {
                    String name = f.getName();
                    if(regex == null || regex.isEmpty()) {
                        return name;
                    } else {
                        Pattern p = Pattern.compile(regex);
                        if(p.matcher(name).matches()) {
                            return name;
                        }
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return "UNKNOWN: " + value;
    }

    /**
     * Get definition name string of a string class field.
     * @param cls Class who defines the field.
     * @param value Field value.
     * @return The field name string.
     */
    public static String getFieldName(Class<?> cls, String value) {
        return getFieldName(cls, value, null);
    }

}
