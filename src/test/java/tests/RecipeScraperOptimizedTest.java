package tests;

import baseClass.BaseTest;
import commons.BrowserFactory;
import commons.ExcelUtils;
import commons.Recipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import pages.RecipeDetailsPage;
import pages.RecipeListingPage;
import utilities.ElementsUtil;

import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RecipeScraperOptimizedTest extends BaseTest {
    private static final Logger log = LogManager.getLogger(RecipeScraperOptimizedTest.class.getName());

    private ExcelUtils.DietRules lchfRules, lfvRules;

    //  Separate visited sets for each diet
    private Set<String> visitedRecipesLCHF = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Set<String> visitedRecipesLFV = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final AtomicInteger scrapedCount = new AtomicInteger(0);

    private enum ScraperMode { ALL, FIRST_N, KEYWORD, SINGLE_URL }
    private ScraperMode mode;

    @BeforeClass
    public void setup() throws Exception {
        // Load diet rules
        lchfRules = ExcelUtils.loadDietRules("Final list for LCHFElimination");
        lfvRules = ExcelUtils.loadDietRules("Final list for LFV Elimination");

        // Reset DB tables
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
                {"LCHF", cfg.scraperLCHFStartUrl, lchfRules, visitedRecipesLCHF},
                {"LFV", cfg.scraperLFVStartUrl, lfvRules, visitedRecipesLFV}
        };
    }

    @Test(dataProvider = "dietData")
    public void scrapeDiet(String dietType, String startUrl, ExcelUtils.DietRules rules, Set<String> visitedRecipes)
            throws SQLException, ExecutionException, InterruptedException {

        visitedRecipes.clear();

        switch (mode) {
            case ALL -> runScraperAllParallel(startUrl, rules, dietType, visitedRecipes);
            case FIRST_N -> runScraperLimitedParallel(startUrl, rules, dietType, cfg.scraperLimit, visitedRecipes);
            case KEYWORD -> runScraperByKeywordParallel(startUrl, rules, dietType, cfg.scraperKeyword, visitedRecipes);
            case SINGLE_URL -> runScraperForSingleRecipe(cfg.scraperSingleRecipeUrl, rules, dietType);
        }

        // Summary
        String addTable = dietType.equals("LCHF") ? "LCHF_add" : "LFV_add";
        String elimTable = dietType.equals("LCHF") ? "LCHF_elimination" : "LFV_elimination";
        System.out.println("[" + dietType + "] Summary:");
        System.out.println(addTable + ": " + db.getRowCount(addTable));
        System.out.println(elimTable + ": " + db.getRowCount(elimTable));
    }

    // ---------------- Single Recipe Mode ----------------
    private void runScraperForSingleRecipe(String recipeUrl, ExcelUtils.DietRules rules, String dietType)
            throws SQLException {
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

    // ---------------- FIRST_N Mode with Pagination ----------------
    private void runScraperLimitedParallel(String startUrl, ExcelUtils.DietRules rules, String dietType, int limit,
                                           Set<String> visitedRecipes)
            throws SQLException, InterruptedException, ExecutionException {

        WebDriver listingDriver = BrowserFactory.createDriver(cfg.headless);
        listingDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(40));
        listingDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        listingDriver.get(startUrl);
        RecipeListingPage listingPage = new RecipeListingPage(listingDriver);

        boolean hasNextPage = true;
        int totalScraped = 0;

        while (hasNextPage && totalScraped < limit) {
            // Get recipes for this page
            List<String> recipeUrls = listingPage.getRecipeUrls(new ArrayList<>(visitedRecipes));

            if (!recipeUrls.isEmpty()) {
                // Keep only up to "remaining" recipes
                int remaining = limit - totalScraped;
                List<String> batch = recipeUrls.size() > remaining
                        ? recipeUrls.subList(0, remaining)
                        : recipeUrls;

                System.out.println("Found " + batch.size() + " recipes on this page...");
                totalScraped += runScrapeBatch(batch, rules, dietType, visitedRecipes);
                System.out.println("Total scraped so far = " + totalScraped);
            }

            // Stop if reached limit
            if (totalScraped >= limit) break;

            // Go to next page
            hasNextPage = listingPage.goToNextPage();
        }

        listingDriver.quit();
        log.info("✅ Finished FIRST_N mode. Total recipes scraped = " + totalScraped);
    }

    // ---------------- ALL Mode ----------------
    private void runScraperAllParallel(String startUrl, ExcelUtils.DietRules rules, String dietType,
                                       Set<String> visitedRecipes)
            throws SQLException, InterruptedException {

        WebDriver listingDriver = BrowserFactory.createDriver(cfg.headless);
        listingDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(40));
        listingDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        ElementsUtil util = new ElementsUtil(listingDriver);
        listingDriver.get(startUrl);
        RecipeListingPage listingPage = new RecipeListingPage(listingDriver);

        boolean hasNextPage = true;
        int totalScraped = 0;

        while (hasNextPage) {
            List<String> recipeUrls = listingPage.getRecipeUrls(new ArrayList<>(visitedRecipes));
            if (!recipeUrls.isEmpty()) {
                System.out.println("Found " + recipeUrls.size() + " recipes on this page...");
                totalScraped += runScrapeBatch(recipeUrls, rules, dietType, visitedRecipes);
                System.out.println("Total scraped so far = " + totalScraped);
            }
            hasNextPage = listingPage.goToNextPage();
        }

        listingDriver.quit();
       log.info("✅ Finished ALL mode. Total recipes scraped = " + totalScraped);
    }

    // ---------------- Keyword Mode ----------------
    private void runScraperByKeywordParallel(String startUrl, ExcelUtils.DietRules rules, String dietType,
                                             String keyword, Set<String> visitedRecipes)
            throws SQLException, InterruptedException, ExecutionException {

        WebDriver listingDriver = BrowserFactory.createDriver(cfg.headless);
        listingDriver.get(startUrl);
        RecipeListingPage listingPage = new RecipeListingPage(listingDriver);

        boolean hasNextPage = true;
        List<String> keywordUrls = new ArrayList<>();
        while (hasNextPage) {
            List<String> recipeUrls = listingPage.getRecipeUrls(new ArrayList<>(visitedRecipes));
            for (String url : recipeUrls) {
                if (url.toLowerCase().contains(keyword.toLowerCase()) && visitedRecipes.add(url)) {
                    keywordUrls.add(url);
                }
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

    // ---------------- Scrape Task ----------------
    private void scrapeRecipeTask(String url, ExcelUtils.DietRules rules, String dietType) throws SQLException {
        WebDriver driver = BrowserFactory.createDriver(cfg.headless);
        try {
            log.info("[" + dietType + "] Scraping recipe: " + url);
            driver.get(url);

            RecipeDetailsPage detailsPage = new RecipeDetailsPage(driver);
            Recipe r = detailsPage.scrapeRecipe();
            classifyAndStore(r, rules, dietType);

            int count = scrapedCount.incrementAndGet();
            log.info("[" + dietType + "] Scraped recipes so far: " + count);

        } finally {
            BrowserFactory.quitDriver();
        }
    }

    // ---------------- Classification ----------------
    private void classifyAndStore(Recipe r, ExcelUtils.DietRules rules, String dietType) throws SQLException {
        Set<String> ingSet = new HashSet<>();
        if (r.Ingredients != null)
            r.Ingredients.forEach(ing -> ingSet.add(ing.toLowerCase().trim()));

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

    // ---------------- Run batch scraper ----------------
    private int runScrapeBatch(List<String> urls, ExcelUtils.DietRules rules, String dietType,
                               Set<String> visitedRecipes)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(cfg.threadPoolSize);
        AtomicInteger batchCount = new AtomicInteger(0);

        for (String url : urls) {
            if (visitedRecipes.add(url)) {
                executor.submit(() -> {
                    try {
                        scrapeRecipeTask(url, rules, dietType);
                        batchCount.incrementAndGet();
                        System.out.println("Scraped: " + url);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);
        return batchCount.get();
    }
}
