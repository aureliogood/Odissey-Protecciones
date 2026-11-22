package es.odissey.protecciones.utils;

import java.util.Objects;

public class Objs {
    public static<T> T replaceNull(T obj, T replacementIfNull) {
        return obj != null ? obj : replacementIfNull;
    }
}
