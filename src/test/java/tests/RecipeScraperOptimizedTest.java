package tests;

import baseClass.BaseTest;
import commons.BrowserFactory;
import commons.ExcelUtils;
import commons.Recipe;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import pages.RecipeDetailsPage;
import pages.RecipeListingPage;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class RecipeScraperOptimizedTest extends BaseTest {

    private static final Logger log = Logger.getLogger(RecipeDetailsPage.class.getName());
    private ExcelUtils.DietRules lchfRules, lfvRules;
    private Set<String> visitedRecipes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicInteger scrapedCount = new AtomicInteger(0);

    private enum ScraperMode { ALL, FIRST_N, KEYWORD, SINGLE_URL }
    private ScraperMode mode;

    @BeforeClass
    public void setup() throws Exception {
        lchfRules = ExcelUtils.loadDietRules("Final list for LCHFElimination");
        lfvRules = ExcelUtils.loadDietRules("Final list for LFV Elimination");

        // Reset all tables
        db.resetTable("LCHF_add");
        db.resetTable("LCHF_elimination");
        db.resetTable("LFV_add");
        db.resetTable("LFV_elimination");

        // Load mode from config
        try {
            mode = ScraperMode.valueOf(cfg.scraperMode);
        } catch (IllegalArgumentException e) {
            mode = ScraperMode.ALL; // fallback
        }
    }

    @DataProvider(name = "dietData", parallel = true)
    public Object[][] dietData() {
        return new Object[][]{
                {"LCHF", cfg.scraperLCHFStartUrl, lchfRules},
                {"LFV", cfg.scraperLFVStartUrl, lfvRules}
        };
    }

    @Test(dataProvider = "dietData")
    public void scrapeDiet(String dietType, String startUrl, ExcelUtils.DietRules rules)
            throws SQLException, ExecutionException, InterruptedException {

        visitedRecipes.clear();

        switch (mode) {
            case ALL -> runScraperAllParallel(startUrl, rules, dietType);
            case FIRST_N -> runScraperLimitedParallel(startUrl, rules, dietType, cfg.scraperLimit);
            case KEYWORD -> runScraperByKeywordParallel(startUrl, rules, dietType, cfg.scraperKeyword);
            case SINGLE_URL -> runScraperForSingleRecipe(cfg.scraperSingleRecipeUrl, rules, dietType);
        }

        String addTable = dietType.equals("LCHF") ? "LCHF_add" : "LFV_add";
        String elimTable = dietType.equals("LCHF") ? "LCHF_elimination" : "LFV_elimination";

        System.out.println("[" + dietType + "] Summary:");
        System.out.println(addTable + ": " + db.getRowCount(addTable));
        System.out.println(elimTable + ": " + db.getRowCount(elimTable));
    }

    // ---------------- Single Recipe ----------------
    private void runScraperForSingleRecipe(String recipeUrl, ExcelUtils.DietRules rules, String dietType) throws SQLException {
        WebDriver driver = BrowserFactory.createDriver(cfg.headless);
        try {
            driver.get(recipeUrl);
            RecipeDetailsPage detailsPage = new RecipeDetailsPage(driver);
            Recipe r = detailsPage.scrapeRecipe();
            classifyAndStore(r, rules, dietType);
        } finally {
            BrowserFactory.quitDriver();
        }
    }

    // ---------------- Parallel FIRST_N Mode ----------------
    private void runScraperLimitedParallel(String startUrl, ExcelUtils.DietRules rules, String dietType, int limit)
            throws SQLException, InterruptedException, ExecutionException {

        WebDriver listingDriver = BrowserFactory.createDriver(cfg.headless);
        listingDriver.get(startUrl);
        RecipeListingPage listingPage = new RecipeListingPage(listingDriver);
        List<String> recipeUrls = listingPage.getRecipeUrls(new ArrayList<>(visitedRecipes));
        BrowserFactory.quitDriver();

        ExecutorService executor = Executors.newFixedThreadPool(cfg.threadPoolSize);
        List<Future<?>> futures = new ArrayList<>();

        int count = 0;
        for (String url : recipeUrls) {
            if (count >= limit) break;
            if (visitedRecipes.contains(url)) continue;
            visitedRecipes.add(url);
            count++;

            String finalUrl = url;
            futures.add(executor.submit(() -> {
                try {
                    scrapeRecipeTask(finalUrl, rules, dietType);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));
        }

        for (Future<?> f : futures) f.get();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
    }

    // ---------------- Parallel ALL Mode ----------------
    private void runScraperAllParallel(String startUrl, ExcelUtils.DietRules rules, String dietType)
            throws SQLException, InterruptedException, ExecutionException {

        WebDriver listingDriver = BrowserFactory.createDriver(cfg.headless);
        listingDriver.get(startUrl);
        RecipeListingPage listingPage = new RecipeListingPage(listingDriver);

        boolean hasNextPage = true;
        List<String> allRecipeUrls = new ArrayList<>();
        while (hasNextPage) {
            List<String> recipeUrls = listingPage.getRecipeUrls(new ArrayList<>(visitedRecipes));
            for (String url : recipeUrls) {
                if (visitedRecipes.contains(url)) continue;
                visitedRecipes.add(url);
                allRecipeUrls.add(url);
            }
            hasNextPage = listingPage.goToNextPage();
        }
        BrowserFactory.quitDriver();

        ExecutorService executor = Executors.newFixedThreadPool(cfg.threadPoolSize);
        List<Future<?>> futures = new ArrayList<>();
        for (String url : allRecipeUrls) {
            futures.add(executor.submit(() -> {
                try {
                    scrapeRecipeTask(url, rules, dietType);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));
        }

        for (Future<?> f : futures) f.get();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
    }

    // ---------------- Parallel KEYWORD Mode ----------------
    private void runScraperByKeywordParallel(String startUrl, ExcelUtils.DietRules rules, String dietType, String keyword)
            throws SQLException, InterruptedException, ExecutionException {

        WebDriver listingDriver = BrowserFactory.createDriver(cfg.headless);
        listingDriver.get(startUrl);
        RecipeListingPage listingPage = new RecipeListingPage(listingDriver);

        boolean hasNextPage = true;
        List<String> keywordUrls = new ArrayList<>();
        while (hasNextPage) {
            List<String> recipeUrls = listingPage.getRecipeUrls(new ArrayList<>(visitedRecipes));
            for (String url : recipeUrls) {
                if (!url.toLowerCase().contains(keyword.toLowerCase())) continue;
                if (visitedRecipes.contains(url)) continue;
                visitedRecipes.add(url);
                keywordUrls.add(url);
            }
            hasNextPage = listingPage.goToNextPage();
        }
        BrowserFactory.quitDriver();

        ExecutorService executor = Executors.newFixedThreadPool(cfg.threadPoolSize);
        List<Future<?>> futures = new ArrayList<>();
        for (String url : keywordUrls) {
            futures.add(executor.submit(() -> {
                try {
                    scrapeRecipeTask(url, rules, dietType);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));
        }

        for (Future<?> f : futures) f.get();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
    }

    // ---------------- Scraping Task (Optimized Single-Tab) ----------------
    private void scrapeRecipeTask(String url, ExcelUtils.DietRules rules, String dietType) throws SQLException {
        WebDriver driver = BrowserFactory.createDriver(cfg.headless); // per-task driver
        try {
            log.info("[" + dietType + "] Scraping recipe: " + url);
            driver.get(url); // single-tab navigation

            RecipeDetailsPage detailsPage = new RecipeDetailsPage(driver);
            Recipe r = detailsPage.scrapeRecipe();
            classifyAndStore(r, rules, dietType);

            int count = scrapedCount.incrementAndGet();
            log.info("[" + dietType + "] Scraped recipes so far: " + count);

        } finally {
            BrowserFactory.quitDriver(); // always clean up
        }
    }

    // ---------------- Recipe Classification ----------------
    private void classifyAndStore(Recipe r, ExcelUtils.DietRules rules, String dietType) throws SQLException {
        Set<String> ingSet = new HashSet<>();
        if (r.Ingredients != null)
            for (String ing : r.Ingredients) ingSet.add(ing.toLowerCase().trim());

        boolean hasElimination = rules.eliminate.stream().anyMatch(ingSet::contains);
        boolean hasAdd = rules.add.stream().anyMatch(ingSet::contains);

        if (!hasElimination && hasAdd) {
            String addTable = dietType.equals("LCHF") ? "LCHF_add" : "LFV_add";
            db.insertRecipe(addTable, r);
            log.info("[" + dietType + "] Added to table " + addTable + ": " + r.Recipe_Name);
        } else {
            String elimTable = dietType.equals("LCHF") ? "LCHF_elimination" : "LFV_elimination";
            db.insertRecipe(elimTable, r);
            log.info("[" + dietType + "] Added to table " + elimTable + ": " + r.Recipe_Name);
        }
    }
}
