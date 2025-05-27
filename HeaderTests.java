import base.TestObject;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Kronospan.By_collections.HeaderTests
 * Validate presence and correctness of all essential header elements.
 * - Load the target URL and initialize header component.
 * - Perform assertion-based checks for header structure and visibility.
 * - Log test status (PASS/FAIL/ERROR) with descriptive messages.
 * - Write detailed results with timestamps into a CSV report file.
 * Ensures consistent user experience by verifying header integrity.
 */

public class HeaderTests extends TestObject {

    private final File csvFile = new File(REPORTS_DIR + "header_test_report.csv");

    @Test
    public void testHeaderElementsPresence() {
        String status;
        String message;

        try {
            getDriver().get("https://stage.kronospan.com/en_EN/");
            Header header = new Header(getDriver());
            header.validateHeaderElements();
            status = "PASS";
            message = "All header elements validated successfully.";
            System.out.println("✅ " + message);
        } catch (AssertionError e) {
            status = "FAIL";
            message = e.getMessage();
            System.err.println("❌ " + message);
        } catch (Exception e) {
            status = "ERROR";
            message = e.getMessage();
            System.err.println("💥 Unexpected error: " + message);
        }

        writeCsvLog("testHeaderElementsPresence", status, message);
    }

    private void writeCsvLog(String testName, String status, String message) {
        boolean fileExists = csvFile.exists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, true))) {
            if (!fileExists) {
                writer.write("Timestamp,Test Name,Status,Message");
                writer.newLine();
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\"",
                    timestamp,
                    testName,
                    status,
                    message.replace("\"", "'")  // escape quotes for CSV
            ));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("⚠️ Could not write to report CSV: " + e.getMessage());
        }
    }
}