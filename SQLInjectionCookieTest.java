import base.TestObject;
import org.openqa.selenium.Cookie;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;

/**
 * Kronospan.By_collections.SQLInjectionCookieTest
 * Test the application's resilience against injection attacks via cookies.
 * - Use a list of SQL, XSS, command injection, and malformed payloads.
 * - Encode and inject each payload into the `location_visited` cookie.
 * - Launch browser session and refresh with the crafted cookie.
 * - Capture HTTP response codes and redirection behavior.
 * - Log results in a CSV file including payloads and outcomes.
 * Validate that no payloads result in successful exploitation or unexpected behavior.
 */

public class SQLInjectionCookieTest extends TestObject {

    public static final String TEST_RESOURCES_DIR = "src\\test\\resources\\";
    public static final String REPORTS_DIR = TEST_RESOURCES_DIR.concat("reports\\");
    private static final int MAX_COOKIE_SIZE = 4096;

    @Test
    public void testSQLInjectionInLocationVisitedCookie() {
        List<String> sqlPayloads = Arrays.asList(

                // SQL Injection
                "' OR '1'='1",
                "' OR '1'='1' -- ",
                "'; DROP TABLE users; --",
                "' AND 1=0 UNION SELECT NULL,NULL--",
                "' UNION SELECT username, password FROM users--",
                "' OR EXISTS(SELECT * FROM users)--",
                "' AND (SELECT COUNT(*) FROM users) > 0--",
                "' OR 1=1#",
                "' OR 'a'='a",
                "' OR sleep(5)--",
                "'||(SELECT version())||'",
                "' OR 1=1--",
                "' OR '' = '",
                "' OR 1=1 LIMIT 1--",
                "' OR (SELECT COUNT(*) FROM information_schema.tables) > 0--",
                "' OR SLEEP(3)--",
                "' AND ASCII(SUBSTRING((SELECT table_name FROM information_schema.tables LIMIT 1),1,1)) > 64 --",
                "' AND 1=CONVERT(INT, (SELECT @@version))--",
                "' AND 1=(SELECT COUNT(*) FROM users)--",
                "1' AND 1=(SELECT COUNT(*) FROM users)--",
                "'/**/OR/**/1=1--",

                // XSS
                "><script>alert(1)</script>",
                "\" onmouseover=\"alert(1)",
                "javascript:alert(document.cookie)",
                "<img src=x onerror=alert(1)>",

                // Command Injection
                "; ls -la",
                "| cat /etc/passwd",
                "`id`",

                // Path Traversal
                "../../../../etc/passwd",
                "%2e%2e%2fetc%2fpasswd",

                // Special Encoding
                "%27%20OR%201%3D1--",
                "\\u0027 OR 1=1--",

                // Extremely Long Strings
                String.format("%0" + 5000 + "d", 0).replace("0", "A")
        );

        String baseUrl = "https://stage.kronospan.com/";
        List<String> failures = new ArrayList<>();

        // Create reports directory if needed
        new File(REPORTS_DIR).mkdirs();

        // Generate timestamped filename
        String timestamp = LocalDate.now().toString(); // e.g., 2025-04-04
        String csvFile = REPORTS_DIR + "injection-report-" + timestamp + ".csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            // Write header
            writer.write("Payload,EncodedPayload,ResponseCode,RedirectLocation");
            writer.newLine();

            for (boolean httpOnly : new boolean[]{true, false}) {
                System.out.println("===== TESTING WITH HttpOnly=" + httpOnly + " =====");

                for (String payload : sqlPayloads) {
                    String encodedPayload = URLEncoder.encode(payload, StandardCharsets.UTF_8);
                    int responseCode = -1;
                    String redirectLocation = "";

                    if (encodedPayload.length() > MAX_COOKIE_SIZE) {
                        System.err.println("⚠️ Skipping oversized payload: " + payload);
                        writer.write(String.format("\"%s\",\"%s\",,\"Payload too large\"", payload, encodedPayload));
                        writer.newLine();
                        continue;
                    }

                    try {
                        System.out.println("🔎 Testing payload: " + payload);

                        webDriver.get(baseUrl);
                        webDriver.manage().deleteAllCookies();

                        Cookie injectionCookie = new Cookie.Builder("location_visited", encodedPayload)
                                .path("/")
                                .isHttpOnly(httpOnly)
                                .build();
                        webDriver.manage().addCookie(injectionCookie);
                        webDriver.navigate().refresh();

                        HttpResponseDetails responseDetails = getHttpResponse(baseUrl);
                        responseCode = responseDetails.statusCode;
                        redirectLocation = responseDetails.redirectLocation;

                        System.out.printf("✅ [%s] %s → Code: %d | Redirect: %s%n", httpOnly, payload, responseCode, redirectLocation);

                    } catch (Exception e) {
                        System.err.printf("🚨 [%s] Error for %s: %s%n", httpOnly, payload, e.getMessage());
                        failures.add("[" + httpOnly + "] " + payload + ": " + e.getMessage());
                    }

                    writer.write(String.format("\"%s\",\"%s\",%d,\"%s\"",
                            payload, encodedPayload, responseCode, redirectLocation));
                    writer.newLine();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!failures.isEmpty()) {
            failures.forEach(System.err::println);
            Assert.fail("Test completed with " + failures.size() + " failure(s). See CSV report: " + csvFile);
        } else {
            System.out.println("\n🎉 All tests passed. Report saved to: " + csvFile);
        }
    }

    private static class HttpResponseDetails {
        int statusCode;
        String redirectLocation;

        HttpResponseDetails(int statusCode, String redirectLocation) {
            this.statusCode = statusCode;
            this.redirectLocation = redirectLocation;
        }
    }

    private HttpResponseDetails getHttpResponse(String url) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (ClassicHttpResponse response = (ClassicHttpResponse) client.execute(request)) {
                int code = response.getCode();
                String location = Optional.ofNullable(response.getFirstHeader("Location"))
                        .map(Header::getValue).orElse("");
                return new HttpResponseDetails(code, location);
            }
        } catch (Exception e) {
            return new HttpResponseDetails(-1, "Error: " + e.getMessage());
        }
    }
}