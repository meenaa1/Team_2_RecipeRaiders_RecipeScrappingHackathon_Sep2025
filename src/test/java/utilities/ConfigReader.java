package utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {

	private static String browserType = null;

	public static Properties initializeprop() {
		Properties prop = new Properties();
		File profile = new File(System.getProperty("user.dir") + "/src/test/resources/Config.properties");

		try (FileInputStream fis = new FileInputStream(profile)) {
			prop.load(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prop;
	}

	public static void setBrowserType(String browser) {
		browserType = browser;
	}

	public static String getBrowserType() {
		if (browserType != null) {
			return browserType;
		} else {
			throw new RuntimeException(
					"Browser not specified. Please provide it in the testng.xml or as a Maven parameter.");
		}
	}

}
