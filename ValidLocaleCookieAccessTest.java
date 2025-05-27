import base.TestObject;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.CountryLocaleInfo;
import utils.CountryLocalesTransformation;
import utils.DatabaseFetch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Kronospan.By_collections.ValidLocaleCookieAccessTest
 * Retrieve country-locale data from the database.
 * Transform locales into correct URL format (language_ISO).
 * For each transformed locale:
 *  - Set location_visited cookie with expected locale.
 *  - Navigate to the corresponding locale URL.
 *  - Verify URL contains expected locale and cookie value matches.
 *  - Log and assert the validation results.
 * Compile and save detailed CSV report with URL, cookie validation status, and test outcomes.
 */

public class ValidLocaleCookieAccessTest extends TestObject {

    private static final String CSV_PATH = "src/test/resources/reports/LocaleCookieValidationReport.csv";
    private final List<String[]> csvData = new ArrayList<>();

    @Test(groups = {"region-based-verification"})
    public void verifyLocaleUrlAndCookieMatch() {
        DatabaseFetch dbFetcher = new DatabaseFetch();
        Map<String, CountryLocaleInfo> countryData = dbFetcher.fetchCountryLocaleInfo();
        CountryLocalesTransformation transformer = new CountryLocalesTransformation();

        assert countryData != null;

        System.out.println("========== Locale → URL → Cookie Verification ==========\n");

        // Add CSV header
        csvData.add(new String[]{
                "Country", "Locale", "URL", "Expected Cookie", "Actual Cookie", "URL Match", "Cookie Match", "Result"
        });

        for (Map.Entry<String, CountryLocaleInfo> entry : countryData.entrySet()) {
            String isoCode = entry.getKey();
            CountryLocaleInfo country = entry.getValue();
            String countryName = country.getCountryName();
            List<String> transformedLocales = transformer.transformLocales(country.getLocales(), isoCode);

            System.out.printf("🌍 %s (ISO: %s) | Locales: %d\n", countryName, isoCode, transformedLocales.size());

            for (String locale : transformedLocales) {
                String expectedCookieValue = locale + "/";
                String testUrl = "https://stage.kronospan.com/" + locale + "/";
                String actualCookieValue = "null";
                boolean urlValid = false;
                boolean cookieValid = false;
                boolean passed = false;

                try {
                    // Set cookie
                    webDriver.get("https://stage.kronospan.com");
                    Cookie cookie = new Cookie("location_visited", expectedCookieValue);
                    webDriver.manage().addCookie(cookie);

                    // Navigate to test URL
                    webDriver.get(testUrl);

                    // Validate cookie and URL
                    Cookie actualCookie = webDriver.manage().getCookieNamed("location_visited");
                    actualCookieValue = actualCookie != null ? actualCookie.getValue() : "null";
                    urlValid = webDriver.getCurrentUrl().contains(locale);
                    cookieValid = actualCookie != null && expectedCookieValue.equals(actualCookieValue);
                    passed = urlValid && cookieValid;

                    // Logging
                    System.out.printf("   └─ Locale: %-10s\n", locale);
                    System.out.printf("      • Cookie Set  : location_visited = %-20s\n", expectedCookieValue);
                    System.out.printf("      • URL         : %-45s\n", testUrl);
                    System.out.printf("      • Cookie Read : %-20s\n", actualCookieValue);
                    System.out.printf("      • URL Match   : %s\n", urlValid ? "✔" : "✘");
                    System.out.printf("      • Cookie Match: %s\n", cookieValid ? "✔" : "✘");
                    System.out.printf("      → RESULT      : %s\n\n", passed ? "✅ PASS" : "❌ FAIL");

                    // Assertions
                    Assert.assertTrue(urlValid, "URL does not contain expected locale: " + locale);
                    Assert.assertNotNull(actualCookie, "Cookie 'location_visited' was not found.");
                    Assert.assertEquals(actualCookieValue, expectedCookieValue, "Cookie value mismatch for " + locale);

                } catch (Exception e) {
                    takeScreenshot("FAIL_" + locale + "_" + isoCode);
                    System.err.printf("❌ ERROR for locale %s: %s\n\n", locale, e.getMessage());
                    Assert.fail("Test failed for locale: " + locale, e);
                }

                // Save to CSV
                csvData.add(new String[]{
                        countryName, locale, testUrl, expectedCookieValue, actualCookieValue,
                        urlValid ? "yes" : "no", cookieValid ? "yes" : "no", passed ? "PASS" : "FAIL"
                });
            }

            System.out.println("--------------------------------------------------------\n");
        }

        writeCsvReport();
        System.out.println("✅ All countries/locale combinations have been tested.");
        System.out.println("========== Test Completed ==========\n");
    }

    private void writeCsvReport() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH))) {
            for (String[] line : csvData) {
                writer.write(String.join(",", escapeCsv(line)));
                writer.newLine();
            }
            System.out.println("📄 CSV report saved to: " + CSV_PATH);
        } catch (Exception e) {
            System.err.println("❌ Failed to write CSV: " + e.getMessage());
        }
    }

    private String[] escapeCsv(String[] fields) {
        return Arrays.stream(fields)
                .map(f -> f.contains(",") ? "\"" + f + "\"" : f)
                .toArray(String[]::new);
    }

    private void takeScreenshot(String name) {
        try {
            File screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File dest = new File(SCREENSHOT_DIR + name + "_" + timestamp + ".png");
            org.apache.commons.io.FileUtils.copyFile(screenshot, dest);
            System.out.println("🖼 Screenshot saved: " + dest.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("⚠ Could not save screenshot: " + e.getMessage());
        }
    }
}