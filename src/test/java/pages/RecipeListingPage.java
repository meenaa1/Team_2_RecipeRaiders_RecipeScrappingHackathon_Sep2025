package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeListingPage {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(xpath = ".//h5/a")
    private List<WebElement> recipeLinks;

    @FindBy(xpath = "//a[contains(text(),'Next') and contains(@class,'page-link')]")
    private List<WebElement> nextButtons;

    public RecipeListingPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(driver, this);
    }

    // Get all recipe URLs on the current page that haven't been visited
    public List<String> getRecipeUrls(List<String> visitedRecipes) {
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(".//h5/a")));
        return recipeLinks.stream()
                .map(e -> e.getAttribute("href"))
                .filter(url -> !visitedRecipes.contains(url))
                .collect(Collectors.toList());
    }

    // Navigate to next page if available
    public boolean goToNextPage() {
        if (!nextButtons.isEmpty()) {
            WebElement nextBtn = nextButtons.get(0);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextBtn);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextBtn);
            wait.until(ExpectedConditions.stalenessOf(nextBtn));
            return true;
        }
        return false;
    }


}
