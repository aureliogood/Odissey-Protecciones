package es.odissey.protecciones.utils.upgrade;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import es.odissey.protecciones.PSConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Migracion puntual de los antiguos archivos TOML a YAML. Se parte de la plantilla YAML
 * incluida en el jar (para conservar comentarios y valores por defecto) y se sobrescriben
 * unicamente los valores presentes en el TOML del servidor.
 */
public final class TomlMigration {

    private TomlMigration() {
    }

    public static void migrateConfig(File tomlFile, File ymlFile) throws IOException {
        migrate(tomlFile, ymlFile, "/config.yml");
    }

    public static void migrateBlock(File tomlFile, File ymlFile) throws IOException {
        migrate(tomlFile, ymlFile, "/block1.yml");
    }

    private static void migrate(File tomlFile, File ymlFile, String template) throws IOException {
        YamlConfiguration yaml = loadTemplate(template);

        FileConfig toml = FileConfig.builder(tomlFile, TomlFormat.instance()).build();
        try {
            toml.load();
            copy(toml.valueMap(), "", yaml);
        } finally {
            toml.close();
        }

        yaml.save(ymlFile);
    }

    private static YamlConfiguration loadTemplate(String resource) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                PSConfig.class.getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    private static void copy(Map<String, Object> map, String prefix, ConfigurationSection target) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Config) {
                copy(((Config) value).valueMap(), path, target);
            } else if (value instanceof Map) {
                copy(castMap(value), path, target);
            } else {
                target.set(path, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
