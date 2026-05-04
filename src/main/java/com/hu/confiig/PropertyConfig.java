package com.hu.confiig;

import org.springframework.boot.system.ApplicationHome;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 读取配置文件
 */
public class PropertyConfig {
    private Properties properties = new Properties();
    public void init() {
        ApplicationHome applicationHome = new ApplicationHome(this.getClass());
        String path = applicationHome.getDir().getAbsolutePath();
        String realPath = path + "/src/main/resources/";
        try {
            File file = new File(realPath + "env.properties");
            InputStream inputStream = new FileInputStream(file);
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDeepSeekKey() {
        return properties.getProperty("DEEP_SEEK_API_KEY");
    }

    public String getEmbeddingKey() {
        return properties.getProperty("EMBEDDING_KEY");
    }

    public String getDeepSeekUrl() {
        return properties.getProperty("DEEP_SEEK_BASE_URL");
    }

    public String getEmbeddingUrl() {
        return properties.getProperty("EMBEDDING_BASE_URL");
    }

    public static void main(String[] args) {
        PropertyConfig propertyConfig = new PropertyConfig();
        propertyConfig.init();
    }
}
