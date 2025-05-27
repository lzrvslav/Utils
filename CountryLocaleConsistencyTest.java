import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.*;
import utils.CountryLocaleInfo;
import utils.DatabaseFetch;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Optional;

/**
 * Kronospan.By_collections.CountryLocaleConsistencyTest
 * Navigate to the staging site (https://stage.kronospan.com).
 * Extract all available country links.
 * For each country link:
 *  - Retrieve actual locale from URL.
 *  - Fetch expected locale and title from database.
 *  - Validate that:
 *      - UI title matches the database title.
 *      - URL locale matches expected database locale.
 *  - Generate report entries with validation results.
 *  - Save results in CSV reports and take screenshots if mismatches occur.
 */

public class CountryLocaleConsistencyTest {

    private WebDriver driver;
    private final String stageUrl = "https://stage.kronospan.com";
    private Map<String, CountryLocaleInfo> dbCountryData;

    private static final String TEST_RESOURCES_DIR = "src\\test\\resources\\";
    private static final String REPORTS_DIR = TEST_RESOURCES_DIR + "reports\\";
    private static final String SCREENSHOT_DIR = TEST_RESOURCES_DIR + "screenshot\\";
    private static final String REPORT_PATH = REPORTS_DIR + "CountryLocaleValidationReport.csv";

    private List<String[]> reportData = new ArrayList<>();

    @BeforeClass
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get(stageUrl);

        new File(REPORTS_DIR).mkdirs();
        new File(SCREENSHOT_DIR).mkdirs();

        DatabaseFetch dbFetch = new DatabaseFetch();
        dbCountryData = dbFetch.fetchCountryLocaleInfo();

        reportData.add(new String[]{
                "Expected Default Language", "Actual Locale in URL", "DB Title", "UI Title", "URL", "Title Match", "URL Match"
        });
    }

    @Test
    public void validateCountriesFromDBAndUI() {
        List<WebElement> countryLinks = driver.findElements(By.xpath("//a[starts-with(@href, 'https://stage.kronospan.com/')]"));
        int checked = 0;

        System.out.println("\n=== Country Locale Validation ===\n");

        for (WebElement link : countryLinks) {
            String href = link.getAttribute("href").trim();
            String uiTitle = link.getText().trim();
            String[] parts = href.split("/");
            String actualLocaleFromUrl = parts[parts.length - 1]; // e.g., fr_TG

            Optional<CountryLocaleInfo> dbMatch = dbCountryData.values().stream()
                    .filter(info -> actualLocaleFromUrl.equalsIgnoreCase(info.getDefaultLanguage()))
                    .findFirst();

            if (dbMatch.isPresent()) {
                CountryLocaleInfo dbInfo = dbMatch.get();
                String expectedLang = dbInfo.getDefaultLanguage();
                String dbTitle = dbInfo.getTitle();

                boolean titleMatch = uiTitle.equals(dbTitle);
                boolean urlMatch = href.endsWith(expectedLang);

                System.out.println("→ Validating Locale: " + expectedLang);
                System.out.println("   • Expected Default Language : " + expectedLang);
                System.out.println("   • Actual Locale in URL      : " + actualLocaleFromUrl);
                System.out.println("   • Expected Title (DB)       : " + dbTitle);
                System.out.println("   • Actual Title (UI)         : " + uiTitle);
                System.out.println("   • Full URL                  : " + href);
                System.out.println("   ✓ Title Match               : " + (titleMatch ? "✔" : "✘"));
                System.out.println("   ✓ URL Match                 : " + (urlMatch ? "✔" : "✘"));
                System.out.println();

                reportData.add(new String[]{
                        expectedLang, actualLocaleFromUrl, dbTitle, uiTitle, href,
                        titleMatch ? "yes" : "no", urlMatch ? "yes" : "no"
                });

                try {
                    Assert.assertTrue(titleMatch, "✘ Title mismatch for locale " + expectedLang +
                            "\nExpected: " + dbTitle + "\nActual  : " + uiTitle);

                    Assert.assertTrue(urlMatch, "✘ URL mismatch for locale " + expectedLang +
                            "\nExpected ending: " + expectedLang + "\nActual         : " + href);
                } catch (AssertionError e) {
                    takeScreenshot("Locale_" + expectedLang);
                    throw e;
                }

                checked++;
            } else {
                System.out.println("⚠ No DB entry found for locale in URL: " + actualLocaleFromUrl + "\n");
            }
        }

        System.out.println("✅ Total Countries Checked: " + checked);
        writeCsvReport();
    }

    private void takeScreenshot(String namePrefix) {
        try {
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File destFile = new File(SCREENSHOT_DIR + namePrefix + "_" + timestamp + ".png");
            try (InputStream in = new FileInputStream(srcFile);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            System.out.println("🖼 Screenshot saved: " + destFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("❌ Failed to save screenshot: " + e.getMessage());
        }
    }

    private void writeCsvReport() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_PATH))) {
            for (String[] line : reportData) {
                writer.write(String.join(",", escapeCsvFields(line)));
                writer.newLine();
            }
            System.out.println("\n📄 Report saved to: " + REPORT_PATH + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] escapeCsvFields(String[] fields) {
        return Arrays.stream(fields)
                .map(f -> f.contains(",") ? "\"" + f + "\"" : f)
                .toArray(String[]::new);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}