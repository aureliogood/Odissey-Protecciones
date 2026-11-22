package es.odissey.protecciones.utils.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limita un entero cargado desde la config al rango [min, max].
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SpecIntInRange {
    int min() default Integer.MIN_VALUE;

    int max() default Integer.MAX_VALUE;
}
