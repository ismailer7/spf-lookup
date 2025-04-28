package com.spf.config;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found in classpath");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Error loading config.properties", ex);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

}
