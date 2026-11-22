package es.odissey.protecciones.utils.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ruta de la clave en el archivo YAML (se admite notacion con puntos, ej. "region.x_radius").
 * Si no se indica, se usa el nombre del campo.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Path {
    String value();
}
