package baseClass;

import commons.BrowserFactory;
import commons.ConfigReader;
import commons.DbManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import utilities.BaseLogger;

import java.sql.SQLException;

public class BaseTest {

    protected static DbManager db;       
    protected static ConfigReader cfg;    

    @BeforeClass(alwaysRun = true)
    public void setUpClass() throws Exception {
        cfg = ConfigReader.load();
        db = new DbManager(cfg);
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        if (db != null) {
            try { db.close(); } catch (SQLException ignored) {}
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpMethod() {
        BrowserFactory.createDriver(cfg.headless);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownMethod() {
        BrowserFactory.quitDriver();
    }

    protected WebDriver getDriver() {
        return BrowserFactory.getDriver();
    }
}