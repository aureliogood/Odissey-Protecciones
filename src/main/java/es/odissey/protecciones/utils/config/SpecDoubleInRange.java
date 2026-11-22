package es.odissey.protecciones.utils.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limita un decimal cargado desde la config al rango [min, max].
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SpecDoubleInRange {
    double min() default -Double.MAX_VALUE;

    double max() default Double.MAX_VALUE;
}
