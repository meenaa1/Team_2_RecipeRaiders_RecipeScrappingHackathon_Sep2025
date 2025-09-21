package commons;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    public String dbHost, dbName, dbUser, dbPassword, excelPath;
    public int dbPort;
    public String scraperLCHFStartUrl, scraperLFVStartUrl;
    public boolean headless; 

    public static ConfigReader load() throws IOException {
        Properties prop = new Properties();
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) throw new IOException("config.properties not found in resources!");
            prop.load(in);
        }

        ConfigReader cfg = new ConfigReader();
        cfg.dbHost = prop.getProperty("db.host");
        cfg.dbPort = Integer.parseInt(prop.getProperty("db.port", "5432"));
        cfg.dbName = prop.getProperty("db.name");
        cfg.dbUser = prop.getProperty("db.user");
        cfg.dbPassword = prop.getProperty("db.password");
        cfg.excelPath = prop.getProperty("ExcelPath");

        cfg.scraperLCHFStartUrl = prop.getProperty("scraperLCHFStartUrl");
        cfg.scraperLFVStartUrl = prop.getProperty("scraperLFVStartUrl");
       
        // Read headless mode, default = false
        cfg.headless = Boolean.parseBoolean(prop.getProperty("headless", "false"));

        return cfg;
    }
}