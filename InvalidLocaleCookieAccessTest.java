import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.HarEntry;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import utils.CountryLocaleInfo;
import utils.CountryLocalesTransformation;
import utils.DatabaseFetch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Kronospan.By_collections.InvalidLocaleCookieAccessTest
 * Validate that locale-specific URLs reject mismatched location_visited cookies.
 * - Initialize proxy server and headless browser for network monitoring.
 * - Retrieve and transform locale data for all countries.
 * - Iterate through all mismatched cookie/URL combinations.
 * - For each invalid combination:
 *   - Set cookie and attempt navigation.
 *   - Capture HTTP response code.
 *   - Log and categorize results as PASS, FAIL, or ERROR.
 * - Export detailed CSV reports for passed and failed cases.
 * - Assert test result based on total detected mismatches.
 */

public class InvalidLocaleCookieAccessTest {

    private WebDriver driver;
    private BrowserMobProxy proxy;

    private final List<String[]> csvPassData = new ArrayList<>();
    private final List<String[]> csvFailData = new ArrayList<>();
    private final List<String> failedCases = new ArrayList<>();

    private static final String TEST_RESOURCES_DIR = "src/test/resources/";
    private static final String REPORTS_DIR = TEST_RESOURCES_DIR + "reports/";
    private static final String REPORT_PASS = REPORTS_DIR + "InvalidLocaleCookieAccess_PASS.csv";
    private static final String REPORT_FAIL = REPORTS_DIR + "InvalidLocaleCookieAccess_FAIL.csv";

    private final Map<String, String> localeToCountry = new HashMap<>();

    @BeforeClass
    public void setUp() {
        proxy = new BrowserMobProxyServer();
        proxy.start(0);

        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
        ChromeOptions options = new ChromeOptions();
        options.setProxy(seleniumProxy);
        options.setAcceptInsecureCerts(true);
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);

        new File(REPORTS_DIR).mkdirs();

        csvPassData.add(new String[]{"Target URL", "Cookie", "Country", "HTTP Code", "Expected", "Result"});
        csvFailData.add(new String[]{"Target URL", "Cookie", "Country", "HTTP Code", "Expected", "Result"});
    }

    @Test
    public void testAllUrlsWithAllOtherCookies() {
        long startTime = System.nanoTime();

        DatabaseFetch dbFetcher = new DatabaseFetch();
        Map<String, CountryLocaleInfo> countryData = dbFetcher.fetchCountryLocaleInfo();
        CountryLocalesTransformation transformer = new CountryLocalesTransformation();

        List<String> allLocales = new ArrayList<>();

        for (Map.Entry<String, CountryLocaleInfo> entry : countryData.entrySet()) {
            String iso = entry.getKey();
            String country = entry.getValue().getCountryName();
            List<String> transformed = transformer.transformLocales(entry.getValue().getLocales(), iso);

            for (String locale : transformed) {
                allLocales.add(locale);
                localeToCountry.put(locale, country);
            }
        }

        // Logger
        int totalLocales = allLocales.size();
        int totalChecks = totalLocales * (totalLocales - 1);
        System.out.println("🧪 STARTING Kronospan.By_collections.InvalidLocaleCookieAccessTest");
        System.out.println("🌍 Total Locales: " + totalLocales);
        System.out.println("🔁 Matrix Size: " + totalLocales + " x " + (totalLocales - 1));
        System.out.println("🔍 Total Invalid Checks Scheduled: " + totalChecks);
        System.out.println("🧵 Threads Available: 12");
        System.out.println("⏱  Start Time: " + LocalDateTime.now());
        System.out.println("--------------------------------------------------\n");

        for (String targetLocale : allLocales) {
            String url = "https://kronospan.com/" + targetLocale + "/";

            for (String cookieLocale : allLocales) {
                if (cookieLocale.equals(targetLocale)) continue;

                String cookieValue = cookieLocale + "/";
                int responseCode = 0;
                String result = "PASS";

                try {
                    driver.get("https://kronospan.com");

                    new WebDriverWait(driver, Duration.ofSeconds(10))
                            .until(webDriver -> ((JavascriptExecutor) webDriver)
                                    .executeScript("return document.readyState").equals("complete"));

                    driver.manage().addCookie(new Cookie("location_visited", cookieValue));
                    proxy.newHar();

                    driver.get(url);

                    List<HarEntry> entries = proxy.getHar().getLog().getEntries();
                    HarEntry finalEntry = entries.get(entries.size() - 1);
                    responseCode = finalEntry.getResponse().getStatus();

                    System.out.printf("🌐 [%s] with ❌ cookie [%s] → HTTP: %d\n", url, cookieValue, responseCode);

                    if (responseCode == 200) {
                        result = "FAIL";
                        String msg = "❌ FAIL: URL " + url + " loaded (200) with INVALID cookie: " + cookieValue;
                        failedCases.add(msg);
                        System.out.println("   " + msg + "\n");
                    } else {
                        System.out.println("   ✅ PASS: Blocked as expected.\n");
                    }

                } catch (Exception e) {
                    result = "ERROR";
                    String err = "❌ ERROR: " + url + " with cookie [" + cookieValue + "] → " + e.getMessage();
                    failedCases.add(err);
                    System.err.println(err);
                }

                String[] row = new String[]{
                        url,
                        cookieValue,
                        localeToCountry.getOrDefault(targetLocale, "N/A"),
                        String.valueOf(responseCode),
                        "Not 200",
                        result
                };

                if ("PASS".equals(result)) {
                    csvPassData.add(row);
                } else {
                    csvFailData.add(row);
                }
            }
        }

        writeCsvReport(REPORT_PASS, csvPassData, "PASS");
        writeCsvReport(REPORT_FAIL, csvFailData, "FAIL");

        long endTime = System.nanoTime();
        long durationMillis = (endTime - startTime) / 1_000_000;

        long seconds = (durationMillis / 1000) % 60;
        long minutes = (durationMillis / (1000 * 60)) % 60;
        long hours = (durationMillis / (1000 * 60 * 60));

        System.out.println("⏹  Finish Time: " + LocalDateTime.now());
        System.out.printf ("⏳ Total Time: %02dh:%02dm:%02ds\n", hours, minutes, seconds);
        System.out.println("--------------------------------------------------\n");

        if (!failedCases.isEmpty()) {
            System.out.println("❌ SUMMARY OF FAILURES:");
            failedCases.forEach(System.out::println);
            Assert.fail("Invalid cookie matrix found " + failedCases.size() + " issues.");
        } else {
            System.out.println("✅ All invalid combinations correctly blocked.");
        }
    }

    private void writeCsvReport(String path, List<String[]> data, String label) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (String[] row : data) {
                writer.write(String.join(",", escapeCsv(row)));
                writer.newLine();
            }
            System.out.printf("📄 %s CSV report saved: %s%n", label, path);
        } catch (Exception e) {
            System.err.println("❌ Failed to write " + label + " report: " + e.getMessage());
        }
    }

    private String[] escapeCsv(String[] fields) {
        return Arrays.stream(fields)
                .map(f -> f.contains(",") ? "\"" + f + "\"" : f)
                .toArray(String[]::new);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) driver.quit();
        if (proxy != null) proxy.stop();
    }
}