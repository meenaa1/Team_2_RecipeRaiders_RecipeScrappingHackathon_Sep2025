package pages;

import commons.Recipe;

import org.openqa.selenium.By;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RecipeDetailsPage {

    private final WebDriver driver;
    private static final Logger logger = Logger.getLogger(RecipeDetailsPage.class.getName());

    // ----------------- Locators -----------------
    @FindBy(xpath = "//h1[@class='rec-heading']/span" )
    private WebElement recipeName;

    @FindBy(id = "ingredients")
    private List<WebElement> ingredientsList;

    @FindBy(xpath = "//div[h6[text()='Preparation Time']]/p")
    private WebElement preparationTime;

    @FindBy(xpath = "//div[contains(@class,'content-last')]//p/strong")
    private WebElement cookingTime;

    @FindBy(id = "aboutrecipe")
    private WebElement recipeDescription;

    @FindBy(id = "methods")
    private List<WebElement> prepSteps;

    @FindBy(xpath = "//figure[@class='table']")
    private WebElement nutrientRows;

    @FindBy(xpath = "//ul[@class='tags-list']/li/a")
    private List<WebElement> recipeCategory;

    @FindBy(css = "p:nth-child(1) span:nth-child(3) a:nth-child(1)")
    private WebElement foodCategory;

    @FindBy(xpath = "//ul[@class='tags-list']/li/a")
    private WebElement tag;

    @FindBy(xpath = "//div[h6[normalize-space(text())='Makes']]/p/strong")
    private WebElement servings;

    @FindBy(xpath = "//p/span[3]/a")
    private WebElement cuisine;

    // ----------------- Constructor -----------------
    public RecipeDetailsPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ----------------- Helper Methods -----------------
    private String getText(WebElement element, String fieldName) {
        if (element == null) {
            logger.warning("Element not found: " + fieldName);
            return "";
        }
        try {
            return element.getText().trim();
        } catch (Exception e) {
            logger.warning("Error reading field: " + fieldName);
            return "";
        }
    }

    private List<String> getTextList(List<WebElement> elements, String fieldName) {
        if (elements == null || elements.isEmpty()) {
            logger.warning("No elements found for: " + fieldName);
            return new ArrayList<>();
        }
        return elements.stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    private String joinTexts(List<WebElement> elements, String delimiter, String fieldName) {
        return String.join(delimiter, getTextList(elements, fieldName));
    }

    // Fetch nutrition table and join as a single string with delimiter
    // Fetch nutrition table and join as a single string with delimiter
    public String getNutritionValues(String delimiter) {
        StringBuilder nutritionBuilder = new StringBuilder();

        try {
            // Locate the nutrition table
            WebElement nutritionTable = nutrientRows;
            List<WebElement> rows = nutritionTable.findElements(By.tagName("tr"));

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() >= 2) {
                    String nutrient = cols.get(0).getText().trim();
                    String value = cols.get(1).getText().trim();
                    nutritionBuilder.append(nutrient)
                            .append(":")
                            .append(value)
                            .append(delimiter);
                }
            }

            // Remove trailing delimiter
            if (nutritionBuilder.length() > 0) {
                nutritionBuilder.setLength(nutritionBuilder.length() - delimiter.length());
            }

        } catch (Exception e) {
            logger.warning("Nutrition table not found or error occurred: " + e.getMessage());
        }

        return nutritionBuilder.toString();
    }




    // ----------------- Scraper -----------------
    public Recipe scrapeRecipe() {
        Recipe r = new Recipe();

        r.Recipe_Name        = getText(recipeName, "Recipe_Name");
        r.Ingredients        = getTextList(ingredientsList, "Ingredients");
        r.Preparation_Time   = getText(preparationTime, "Preparation_Time");
        r.Cooking_Time       = getText(cookingTime, "Cooking_Time");
        r.Recipe_Description = getText(recipeDescription, "Recipe_Description");
        r.Preparation_method = joinTexts(prepSteps, " ", "Preparation_method");
        r.Nutrient_values    = getNutritionValues("|");
        r.Recipe_URL         = driver.getCurrentUrl();
        r.Recipe_Category    = getRecipeCategories();
        r.Food_Category      = getText(foodCategory, "Food_Category");
        r.Tag                = getText(tag, "Tag");
        r.No_of_servings     = getText(servings, "No_of_servings");
        r.Cuisine_category   = getText(cuisine, "Cuisine_category");

        return r;
    }
    
    //Recipe category  
    private String getRecipeCategories() 
    {

        for (WebElement el : recipeCategory) {
            String text = el.getText().trim().toLowerCase();
            if (text.contains("breakfast")||text.contains("lunch")||text.contains("dinner")||text.contains("snack"))
            {
            	return text;
            }
        }
        return "other";
    }
  }

