package pages;

import commons.Recipe;

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

    @FindBy(id = "rcpnutrients")
    private List<WebElement> nutrientRows;

    @FindBy(xpath = "//ul[@class='tags-list']/li/a")
    private List<WebElement> recipeCategory;

    @FindBy(xpath = "//span[contains(@class,'food-category')]")
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

    // ----------------- Scraper -----------------
    public Recipe scrapeRecipe() {
        Recipe r = new Recipe();

        r.Recipe_Name        = getText(recipeName, "Recipe_Name");
        r.Ingredients        = getTextList(ingredientsList, "Ingredients");
        r.Preparation_Time   = getText(preparationTime, "Preparation_Time");
        r.Cooking_Time       = getText(cookingTime, "Cooking_Time");
        r.Recipe_Description = getText(recipeDescription, "Recipe_Description");
        r.Preparation_method = joinTexts(prepSteps, " ", "Preparation_method");
        r.Nutrient_values    = joinTexts(nutrientRows, " ", "Nutrient_values");
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

