package tests;

import baseClass.BaseTest;
import commons.ExcelUtils;
import commons.Recipe;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import pages.RecipeDetailsPage;
import pages.RecipeListingPage;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeScraperTest extends BaseTest {

    private ExcelUtils.DietRules lchfRules, lfvRules;
    private Set<String> visitedRecipes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @BeforeClass
    public void setup() throws Exception {
        lchfRules = ExcelUtils.loadDietRules("Final list for LCHFElimination");
        lfvRules = ExcelUtils.loadDietRules("Final list for LFV Elimination");

        db.createTableIfNotExists("LCHF_add");
        db.createTableIfNotExists("LCHF_elimination");
        db.createTableIfNotExists("LFV_add");
        db.createTableIfNotExists("LFV_elimination");
    }

    @DataProvider(name = "dietData", parallel = true)
    public Object[][] dietData() {
        return new Object[][]{
                {"LCHF", cfg.scraperLCHFStartUrl, lchfRules},
                {"LFV", cfg.scraperLFVStartUrl, lfvRules}
        };
    }

    @Test(dataProvider = "dietData", threadPoolSize = 2)
    public void scrapeDiet(String dietType, String startUrl, ExcelUtils.DietRules rules) throws SQLException {
        visitedRecipes.clear();
        runScraper(startUrl, rules, dietType);

        String addTable = dietType.equals("LCHF") ? "LCHF_add" : "LFV_add";
        String elimTable = dietType.equals("LCHF") ? "LCHF_elimination" : "LFV_elimination";

        System.out.println("[" + dietType + "] Summary:");
        System.out.println(addTable + ": " + db.getRowCount(addTable));
        System.out.println(elimTable + ": " + db.getRowCount(elimTable));
    }

    private void runScraper(String startUrl, ExcelUtils.DietRules rules, String dietType) throws SQLException {
        WebDriver driver = getDriver();
        driver.get(startUrl);

        RecipeListingPage listingPage = new RecipeListingPage(driver);
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<String> recipeUrls = listingPage.getRecipeUrls(new ArrayList<>(visitedRecipes));

            for (String url : recipeUrls) {
                visitedRecipes.add(url);

                ((JavascriptExecutor) driver).executeScript("window.open(arguments[0]);", url);
                ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
                driver.switchTo().window(tabs.get(1));

                RecipeDetailsPage detailsPage = new RecipeDetailsPage(driver);
                Recipe r = detailsPage.scrapeRecipe();

                // Classification logic directly in test
                classifyAndStore(r, rules, dietType);

                driver.close();
                driver.switchTo().window(tabs.get(0));
            }

            hasNextPage = listingPage.goToNextPage();
        }
    }

    private void classifyAndStore(Recipe r, ExcelUtils.DietRules rules, String dietType) throws SQLException {
        Set<String> ingSet = new HashSet<>();
        if (r.Ingredients != null)
            for (String ing : r.Ingredients) ingSet.add(ing.toLowerCase().trim());

        boolean hasElimination = rules.eliminate.stream().anyMatch(ingSet::contains);
        boolean hasAdd = rules.add.stream().anyMatch(ingSet::contains);

        if (!hasElimination && hasAdd) {
            String addTable = dietType.equals("LCHF") ? "LCHF_add" : "LFV_add";
            db.insertRecipe(addTable, r);
        } else {
            String elimTable = dietType.equals("LCHF") ? "LCHF_elimination" : "LFV_elimination";
            db.insertRecipe(elimTable, r);
        }
    }
}
