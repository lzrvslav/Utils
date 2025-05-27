import base.TestObject;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.CountryLocaleFetcher;
import utils.CountryLocaleInfo;
import utils.DatabaseConnectionAndCloser;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test Flow Summary
 *
 * Test 1: UrlsOpenForTransformedLocales
 * - Connects to DB, fetches country/locale pairs.
 * - Builds valid transformed URLs (e.g., en_DE).
 * - Loads each in WebDriver and verifies correct redirection.

 * Test 2: UrlsOpenForTransformedLocalesWithInvalidLocales
 * - Connects to DB, reuses locale data.
 * - Generates random invalid URL suffixes.
 * - Tests fallback behavior (e.g., /404) for each invalid URL.
 * - Captures final URL + HTTP response code.
 */

public class UrlAccessWithTransformedLocalesTests {

    private static final String CSV_REPORT_PATH = "src/test/resources/reports/URL_access_transformed_locale_report.csv";

    @Test(priority = 1, description = "Verifies accessibility of URLs constructed using transformed locales and logs results to a CSV report.")
    public void UrlsOpenForTransformedLocales() {
        // Step 1: Connect to database
        System.out.println("🟢 Step 1: Connecting to database...");
        Connection conn = DatabaseConnectionAndCloser.getConnection();

        if (conn != null) {
            // Step 2: Fetch locale data
            System.out.println("📥 Step 2: Fetching country and locale data...");
            CountryLocaleFetcher dbPage = new CountryLocaleFetcher();
            Map<String, CountryLocaleInfo> countryData = dbPage.getCountryLocales(conn);

            // Step 3: Initialize WebDriver
            System.out.println("🚗 Step 3: Initializing WebDriver...");
            TestObject base = new TestObject();
            base.setUpTest("chrome");
            WebDriver driver = base.getDriver();

            int success = 0;
            int failure = 0;

            // Step 4: Prepare CSV headers
            List<String[]> csvRows = new ArrayList<>();
            csvRows.add(new String[]{"Country Title", "Invalid Locale", "Checked URL", "Final URL", "Result", "Error Message"});

            System.out.println("🌍 Step 5: Beginning URL accessibility checks...\n");

            // Step 5: Iterate over all countries and locales
            for (CountryLocaleInfo entry : countryData.values()) {
                String countryTitle = entry.getTitle();
                String isoCode = entry.getIsoCode();
                List<String> locales = entry.getLocales();

                for (String locale : locales) {
                    String[] parts = locale.split("-|_");
                    if (parts.length > 0) {
                        // Step 6: Create transformed locale
                        String transformedLocale = parts[0] + "_" + isoCode;
                        String checkedUrl = "https://stage.kronospan.com/" + transformedLocale;

                        System.out.printf("🔗 Testing URL: %s\n", checkedUrl);

                        String result;
                        String finalUrl = "";
                        String errorMessage = "";

                        try {
                            // Step 7: Open the URL
                            driver.get(checkedUrl);
                            finalUrl = driver.getCurrentUrl();

                            System.out.printf("   ✅ Page loaded successfully\n");
                            System.out.printf("   ➤ Final URL after redirects: %s\n", finalUrl);

                            // Step 8: Assert that final URL contains expected transformed locale
                            Assert.assertTrue(finalUrl.contains(transformedLocale),
                                    "Expected final URL to contain transformed locale: " + transformedLocale);

                            result = "Success";
                            success++;
                        } catch (TimeoutException timeout) {
                            // Step 9: Handle timeout
                            System.err.printf("   ⏳ Timeout while loading: %s\n", checkedUrl);
                            result = "Timeout";
                            errorMessage = timeout.getMessage();
                            failure++;
                            Assert.fail("Timeout loading URL: " + checkedUrl);
                        } catch (Exception e) {
                            // Step 10: Handle general error
                            System.err.printf("   ❌ Error loading %s: %s\n", checkedUrl, e.getMessage());
                            result = "Error";
                            errorMessage = e.getMessage();
                            failure++;
                            Assert.fail("Error loading URL: " + checkedUrl + " — " + e.getMessage());
                        }

                        // Step 11: Record result
                        csvRows.add(new String[]{
                                countryTitle,
                                transformedLocale,
                                checkedUrl,
                                finalUrl,
                                result,
                                errorMessage
                        });

                        System.out.println("----------------------------------------------------\n");
                    }
                }
            }

            // Step 12: Write results to CSV
            writeCsvReport(csvRows);

            // Step 13: Summary and cleanup
            System.out.printf("🎯 Test completed — %d succeeded, %d failed.\n", success, failure);
            base.tearDownTest(null);
            DatabaseConnectionAndCloser.closeConnection(conn);
        } else {
            System.out.println("❌ Database connection failed.");
            Assert.fail("Could not connect to database.");
        }
    }

    @Test(priority = 2, description = "Open site with unsupported locale data and invalid combinations")
    public void UrlsOpenForTransformedLocalesWithInvalidLocales() {
        // Step 1: Establish connection to the database
        System.out.println("🟢 Connecting to database for invalid locale testing...");
        Connection conn = DatabaseConnectionAndCloser.getConnection();

        if (conn != null) {
            // Step 2: Fetch country and locale data
            CountryLocaleFetcher fetcher = new CountryLocaleFetcher();
            Map<String, CountryLocaleInfo> countryData = fetcher.getCountryLocales(conn);

            // ✅ Print invalid test scope
            int invalidsPerLocale = 10;
            int totalInvalidLocales = countryData.values().stream()
                    .mapToInt(info -> info.getLocales().size()).sum();
            int totalInvalidUrls = totalInvalidLocales * invalidsPerLocale;

            System.out.printf("📊 Fetched %d countries, %d locales → %d invalid URLs will be tested.\n\n",
                    countryData.size(), totalInvalidLocales, totalInvalidUrls);

            // Step 3: Initialize WebDriver
            TestObject base = new TestObject();
            base.setUpTest("chrome");
            WebDriver driver = base.getDriver();

            // Step 4: Prepare CSV report structure
            List<String[]> csvRows = new ArrayList<>();
            csvRows.add(new String[]{"Country", "Invalid Locale", "Tested URL", "Final URL", "Response Code", "Result", "Error Message"});

            int success = 0;
            int failure = 0;

            // Step 5: Iterate through countries and generate test URLs
            for (CountryLocaleInfo country : countryData.values()) {
                String isoCode = country.getIsoCode();
                List<String> locales = country.getLocales();

                for (String locale : locales) {
                    String langPart = locale.split("-|_")[0];
                    List<String> invalidParts = generateRandomInvalidParts(invalidsPerLocale);

                    for (String invalid : invalidParts) {
                        String transformedLocale = langPart + "_" + isoCode;
                        String invalidPath = transformedLocale + "/" + invalid;
                        String url = "https://stage.kronospan.com/" + invalidPath;

                        System.out.printf("🔗 Testing invalid locale URL: %s\n", url);

                        String currentUrl = "";
                        int responseCode = -1;
                        String result;
                        String errorMessage = "";

                        try {
                            // Step 6: Open the invalid URL in browser
                            driver.get(url);
                            currentUrl = driver.getCurrentUrl();

                            // Step 7: Capture HTTP response code
                            responseCode = getResponseCode(url);

                            // Step 8: Check for fallback (404 or redirect)
                            boolean isFallback = currentUrl.contains("/404") || !currentUrl.contains(invalidPath);

                            if (isFallback) {
                                result = "Fallback Confirmed";
                                success++;
                                System.out.printf("   ✅ Fallback confirmed\n      ➤ Final URL     : %s\n      ➤ Response Code: %d\n", currentUrl, responseCode);
                            } else {
                                result = "Unexpected Redirect";
                                failure++;
                                System.err.printf("   ⚠️ Unexpected redirect\n      ➤ Requested URL : %s\n      ➤ Final URL     : %s\n      ➤ Response Code: %d\n", url, currentUrl, responseCode);
                            }

                        } catch (Exception e) {
                            errorMessage = e.getMessage();
                            result = "Error";
                            failure++;
                            System.err.printf("   ❌ Error loading URL\n      ➤ Requested URL : %s\n      ➤ Error         : %s\n      ➤ Response Code: %d\n", url, errorMessage, responseCode);
                        }

                        // Step 9: Record the result in CSV data
                        csvRows.add(new String[]{
                                country.getTitle(),
                                invalidPath,
                                url,
                                currentUrl,
                                String.valueOf(responseCode),
                                result,
                                errorMessage
                        });

                        System.out.println("----------------------------------------------------");
                    }
                }
            }

            // Step 10: Write final CSV report
            writeCsvReport(csvRows, "src/test/resources/reports/Invalid_Locale_Fallback_Report.csv");

            // Step 11: Summary
            System.out.printf("🎯 Invalid locale test finished.\n✅ Passed: %d\n❌ Failed: %d\n", success, failure);

            // Step 12: Cleanup
            base.tearDownTest(null);
            DatabaseConnectionAndCloser.closeConnection(conn);
        } else {
            // If DB fails, halt
            System.out.println("❌ Database connection failed.");
            Assert.fail("Could not connect to database.");
        }
    }

    private List<String> generateRandomInvalidParts(int count) {
        List<String> parts = new ArrayList<>();
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+{}[]<>?/\\|~`.,;:'\"";

        for (int i = 0; i < count; i++) {
            int length = ThreadLocalRandom.current().nextInt(3, 12);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; j++) {
                char c = characters.charAt(ThreadLocalRandom.current().nextInt(characters.length()));
                sb.append(c);
            }
            parts.add(sb.toString());
        }
        return parts;
    }

    private void writeCsvReport(List<String[]> dataRows) {
        writeCsvReport(dataRows, CSV_REPORT_PATH);
    }

    private void writeCsvReport(List<String[]> dataRows, String filePath) {
        try (FileWriter csvWriter = new FileWriter(filePath)) {
            for (String[] row : dataRows) {
                for (int i = 0; i < row.length; i++) {
                    String field = row[i] != null ? row[i] : "";
                    field = field.replace("\"", "\"\""); // Escape quotes
                    csvWriter.append("\"").append(field).append("\"");
                    if (i < row.length - 1) csvWriter.append(",");
                }
                csvWriter.append("\n");
            }
            System.out.printf("📝 Clean CSV report generated at: %s\n", filePath);
        } catch (IOException e) {
            System.err.printf("⚠️ Failed to write cleaned CSV report: %s\n", e.getMessage());
        }
    }

    private int getResponseCode(String urlString) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(urlString).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(false);
            connection.connect();
            return connection.getResponseCode();
        } catch (IOException e) {
            System.err.printf("⚠️ Failed to get response code for %s: %s\n", urlString, e.getMessage());
            return -1;
        }
    }
}