package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.*;
import utilities.ElementsUtil;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeListingPage {

    private WebDriver driver;
    private ElementsUtil elementsUtil;

    @FindBy(xpath = ".//h5/a")
    private List<WebElement> recipeLinks;

    private final By recipeLinkLocator = By.xpath(".//h5/a");
    private final By nextBtnLocator = By.xpath("//a[contains(text(),'Next') and contains(@class,'page-link')]");

    public RecipeListingPage(WebDriver driver) {
        this.driver = driver;
        this.elementsUtil = new ElementsUtil(driver);
        PageFactory.initElements(driver, this);
    }

    public List<String> getRecipeUrls(List<String> visitedRecipes) {
        try {
            elementsUtil.waitForElementToBeVisible(recipeLinkLocator);
            return recipeLinks.stream()
                    .map(e -> e.getAttribute("href"))
                    .filter(url -> url != null && !visitedRecipes.contains(url))
                    .collect(Collectors.toList());
        } catch (TimeoutException e) {
            System.out.println("No recipe links found on this page.");
            return List.of();
        }
    }

    public boolean goToNextPage() {
        try {
            if (!elementsUtil.isElementDisplayed(nextBtnLocator)) {
                System.out.println("No Next button. Stopping pagination.");
                return false;
            }

            WebElement nextBtn = driver.findElement(nextBtnLocator);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextBtn);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextBtn);

            // Wait for the old "Next" button to disappear
            elementsUtil.waitForElementToDisappear(nextBtnLocator, 10);

            // Optional politeness pause
            Thread.sleep(1000);

            return true;

        } catch (Exception e) {
            System.out.println("Pagination error: " + e.getMessage());
            return false;
        }
    }
}
