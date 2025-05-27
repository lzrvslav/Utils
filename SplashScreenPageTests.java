import base.TestObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.CountryLocaleFetcher;
import utils.CountryLocaleInfo;
import utils.DatabaseConnectionAndCloser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.time.Duration;
import java.util.*;

/**
 * Kronospan.By_collections.SplashScreenPageTests
 * Validate visual content and link integrity of the splash screen page.
 * - Check presence of logo, welcome message, and regional sections.
 * - Assert visibility of country selection and all expected elements.
 * - Retrieve locale data from the database and map to UI elements.
 * - Verify that each country link points to the correct localized URL.
 * - Open and validate each link for accurate navigation and response code.
 * - Log results into a CSV report with status, HTTP code, and error details.
 * Ensures accurate locale redirection and complete splash page rendering.
 */

public class SplashScreenPageTests extends TestObject {

    @Test(priority = 1, description = "Verify Splash Screen Page Content")
    public void verifySplashScreenPageContent() {
        try {
            // Step 1: Navigate to splash screen URL
            System.out.println("Navigating to splash screen page https://stage.kronospan.com");
            webDriver.get("https://stage.kronospan.com");

            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.urlToBe("https://stage.kronospan.com/"));
            System.out.println("URL loaded successfully.");

            SplashScreenPage homePage = new SplashScreenPage(webDriver);

            // Step 2: Verify logo presence
            wait.until(ExpectedConditions.visibilityOf(homePage.logo));
            Assert.assertTrue(homePage.isLogoDisplayed(), "Logo is not displayed");
            System.out.println("Logo is displayed ✔");

            // Step 3: Check welcome text visibility
            wait.until(ExpectedConditions.visibilityOf(homePage.welcomeText));
            Assert.assertTrue(homePage.isWelcomeTextDisplayed(), "Welcome text is not visible");
            System.out.println("Welcome text is visible ✔");

            // Step 4: Check "Select Country" label visibility
            wait.until(ExpectedConditions.visibilityOf(homePage.selectCountryText));
            Assert.assertTrue(homePage.isSelectCountryTextDisplayed(), "Select country text is missing");
            System.out.println("'Select country' text is visible ✔");

            // Step 5: Check region section presence
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//p[normalize-space()='Global']")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//p[normalize-space()='Africa']")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//p[normalize-space()='Americas']")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//p[normalize-space()='Asia']")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//p[normalize-space()='Europe']")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//p[normalize-space()='Oceania']")));
            System.out.println("All region sections are present in the DOM.");

            // Step 6: Validate all region containers
            Assert.assertTrue(homePage.areAllRegionsPresent(), "One or more region sections are missing");
            System.out.println("All region sections are displayed ✔");

            System.out.println("✅ All content verified successfully on stage.kronospan.com");

        } catch (Exception e) {
            System.out.println("❌ Test failed due to exception: " + e.getMessage());

            Assert.fail("Test failed due to exception: " + e.getMessage());
        }
    }

    @Test(priority = 2, description = "Verify Splash Screen Page Content URLs")
    public void verifySplashScreenPageContentURLs() {
        int passed = 0;
        int failed = 0;

        try (Connection conn = DatabaseConnectionAndCloser.getConnection()) {
            // Step 1: Retrieve country locale info from DB
            System.out.println("\n📦 Fetching country locale data from DB in real-time...");
            CountryLocaleFetcher fetcher = new CountryLocaleFetcher();
            Map<String, CountryLocaleInfo> dbLocales = fetcher.getCountryLocales(conn);
            System.out.printf("📊 Retrieved %d entries from DB.%n", dbLocales.size());

            // Step 2: Open splash screen
            System.out.println("\n🚀 Navigating to splash screen...");
            webDriver.get("https://stage.kronospan.com");

            SplashScreenPage splashScreen = new SplashScreenPage(webDriver);
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.visibilityOf(splashScreen.selectCountryText));
            System.out.println("✅ Splash screen loaded.");

            // Step 3: Get all anchor links for countries
            List<WebElement> countryLinks = splashScreen.getAllCountryLinks();
            System.out.printf("🌍 Found %d country links.%n", countryLinks.size());

            for (int i = 0; i < countryLinks.size(); i++) {
                try {
                    countryLinks = splashScreen.getAllCountryLinks();
                    WebElement link = countryLinks.get(i);

                    String rawHref = link.getAttribute("href").trim();
                    String href = rawHref.split("#")[0];
                    String anchorText = link.getText().trim();

                    CountryLocaleInfo info = dbLocales.values()
                            .stream()
                            .filter(v -> v.getCountryName().equalsIgnoreCase(anchorText))
                            .findFirst()
                            .orElse(null);

                    if (info == null) {
                        System.out.printf("   ⚠️  Skipping '%s' — no DB mapping found.%n", anchorText);
                        continue;
                    }

                    String expectedLang = info.getDefaultLanguage();
                    System.out.printf("\n🔗 [%02d] %s\n", (i + 1), anchorText);
                    System.out.printf("   ➤ DB locale: %s%n", expectedLang);
                    System.out.printf("   ➤ Expected href suffix: %s%n", expectedLang);
                    System.out.println("   ➤ Original href: " + rawHref);
                    System.out.println("   ➤ Cleaned URL:   " + href);

                    Assert.assertTrue(href.contains(expectedLang),
                            "❌ URL does not match expected DB locale: " + expectedLang);
                    System.out.println("   ✅ Href matches DB locale ✔");

                    clickCountryLink(link, anchorText);

                    ((JavascriptExecutor) webDriver).executeScript("window.open(arguments[0], '_blank');", href);
                    List<String> tabs = new ArrayList<>(webDriver.getWindowHandles());
                    webDriver.switchTo().window(tabs.get(1));

                    wait.until(ExpectedConditions.urlContains(href));
                    String currentUrl = webDriver.getCurrentUrl().replaceAll("/$", "");
                    String expectedUrl = href.replaceAll("/$", "");
                    Assert.assertEquals(currentUrl, expectedUrl, "❌ Navigation mismatch.");
                    System.out.println("   ✔ Navigation OK");

                    int statusCode = getResponseCode(href);
                    System.out.println("   📡 HTTP Code: " + statusCode);
                    if (statusCode < 400) {
                        System.out.println("   ✅ Response code is below 400 ✔");
                    } else {
                        System.out.println("   ❌ Response code is 400 or higher ⚠");
                    }

                    Assert.assertTrue(statusCode < 400, "❌ Response code >= 400");
                    passed++;
                    writeCsvLine("verifySplashScreenPageContentURLs", anchorText, href, "-", "PASS", statusCode, "");

                } catch (Exception e) {
                    failed++;
                    System.out.println("   ❌ Error: " + e.getMessage());
                    writeCsvLine("verifySplashScreenPageContentURLs", "-", "-", "-", "FAIL", 0, e.getMessage());

                } finally {
                    List<String> tabs = new ArrayList<>(webDriver.getWindowHandles());
                    if (tabs.size() > 1) {
                        webDriver.close();
                        webDriver.switchTo().window(tabs.get(0));
                    }
                }
            }

            System.out.printf("%n🎯 Summary: %d Passed, %d Failed%n", passed, failed);
            Assert.assertEquals(failed, 0, "❌ Some links failed validation!");

        } catch (Exception e) {
            System.out.println("❌ Test failed due to unexpected exception: " + e.getMessage());
            Assert.fail("Test failed: " + e.getMessage());
        }
    }

    public void clickCountryLink(WebElement link, String country) {
        Assert.assertTrue(link.isDisplayed(), "Link is not displayed");
        Assert.assertTrue(link.isEnabled(), "Link is not clickable");
    }

    public int getResponseCode(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            return connection.getResponseCode();
        } catch (Exception e) {
            System.out.println("⚠️ Unable to get response code for: " + urlString + " - " + e.getMessage());
            return 0;
        }
    }

    private synchronized void writeCsvLine(String testName, String country, String href, String title, String status, int httpCode, String error) {
        String reportFile = "src/test/resources/reports/splash_report.csv"; // Direct path
        File file = new File(reportFile);
        boolean fileExists = file.exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile, true))) {
            if (!fileExists) {
                writer.write("\"Country\",\"Href\",\"Status\",\"HTTP Code\",\"Is HTTP < 400\",\"Error\"\n");
            }

            String isValidCode = String.valueOf(httpCode < 400);
            writer.write(String.format("\"%s\",\"%s\",\"%s\",%d,%s,\"%s\"\n",
                    country, href, status, httpCode, isValidCode, error.replace("\"", "'")));
        } catch (IOException e) {
            System.out.println("⚠️ Failed to write to CSV: " + e.getMessage());
        }
    }
}