import base.TestObject;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.CountryLocaleInfo;
import utils.CountryLocaleFetcher;
import utils.CountryLocalesTransformation;
import utils.DatabaseConnectionAndCloser;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * Scrapes **only** the Sustainability/Održivost wrapper — identified by the
 * subtitle link whose href contains the slug "/odr-ivost/" — from the 5‑th
 * mega‑submenu column of https://kronospan.com/{locale}/.
 *
 * It prints a formatted console report and exports a CSV to
 * TEST_RESOURCES_DIR/reports/.
 */

public class MegaMenuScrapeTest extends TestObject {

    /* --------------- constants --------------- */
    private static final String IMG_FILTER  = "/assets/sustainability/";
    private static final String FORMAT      = "[%-5s] %-5s | %-55s | %-80s | %s%n";
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /* --------------- runtime --------------- */
    private final List<String>   localeCodes = new ArrayList<>();
    private final List<String[]> csvRows     = new ArrayList<>();

    /* --------------- prepare locales --------------- */
    @BeforeClass(alwaysRun = true)
    public void prepareLocales() {
        Connection conn = DatabaseConnectionAndCloser.getConnection();
        Assert.assertNotNull(conn, "DB connection failed");

        CountryLocaleFetcher fetcher = new CountryLocaleFetcher();
        Map<String, CountryLocaleInfo> map = fetcher.getCountryLocales(conn);

        CountryLocalesTransformation transformer = new CountryLocalesTransformation();
        Set<String> dedup = new LinkedHashSet<>();

        for (CountryLocaleInfo info : map.values()) {
            List<String> transformed = transformer.transformLocales(info.getLocales(), info.getIsoCode());
            for (String lc : transformed) {
                if (!lc.toLowerCase().startsWith("en_")) dedup.add(lc);
            }
        }
        localeCodes.addAll(dedup);
        DatabaseConnectionAndCloser.closeConnection(conn);

        Assert.assertFalse(localeCodes.isEmpty(), "No locale codes produced; check DB/transform logic");
    }

    /* --------------- main test --------------- */
    @Test(priority = 1,
            description = "Scrape Sustainability wrapper for every locale → console + CSV (grouped by country)")
    public void scrapeSustainabilityWrappers() throws IOException {
        WebDriver driver = getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        String wrappersCss = "div.row.mega-submenu-section[class*='5-sub-menu'] " +
                "div.sub-items-container div.sub-item-class-wrapper";
        By wrapperSel = By.cssSelector(wrappersCss);

        csvRows.add(new String[]{"locale","country","type","text","href","title"});

        for (String locale : localeCodes) {
            String url = "https://kronospan.com/" + locale + "/";
            driver.get(url);

            wait.until(ExpectedConditions.presenceOfElementLocated(wrapperSel));
            List<WebElement> wrappers = driver.findElements(wrapperSel);
            if (wrappers.isEmpty()) continue;

            for (WebElement wrapper : wrappers) {
                // detect sustainability by img src containing the filter token
                boolean isSustainability;
                try {
                    String src = wrapper.findElement(By.cssSelector("img.sub-item-img"))
                            .getAttribute("src");
                    isSustainability = src != null && src.contains(IMG_FILTER);
                } catch (NoSuchElementException e) {
                    isSustainability = false;
                }
                if (!isSustainability) continue;

                // heading link
                safeLogAndSave(locale, "h5",
                        () -> wrapper.findElement(By.cssSelector("h5 > a.sub-item-subtitle")));

                // thumbnail parent <a>
                safeLogAndSave(locale, "thumb", () -> {
                    WebElement img = wrapper.findElement(By.cssSelector("a > img.sub-item-img"));
                    return img.findElement(By.xpath(".."));
                });

                // list items
                List<WebElement> liLinks = wrapper.findElements(By.cssSelector("ul.sub-item-list li a"));
                for (WebElement li : liLinks) logAndSave(locale, "li", li);
            }
        }
        sortAndWriteCsv();
    }

    /* --------------- helpers --------------- */
    private void safeLogAndSave(String locale, String type, Supplier<WebElement> supplier) {
        try { logAndSave(locale, type, supplier.get()); } catch (Exception ignored) {}
    }

    private void logAndSave(String locale, String type, WebElement aTag) {
        String text  = aTag.getText().trim().replaceAll("\\s+", " ");
        String href  = aTag.getAttribute("href");
        String title = aTag.getAttribute("title");
        String country = getCountryFromLocale(locale);

        System.out.printf(FORMAT, locale, type, abbreviate(text,55), abbreviate(href,80), title);
        csvRows.add(new String[]{locale, country, type, text, href, title});
    }

    private void sortAndWriteCsv() throws IOException {
        // sort rows (skip header at index 0) by country → locale
        csvRows.subList(1, csvRows.size()).sort(
                Comparator.<String[], String>comparing(r -> r[1])  // country
                        .thenComparing(r -> r[0]));              // locale

        String fileName = "sustainability_links_" + TS.format(LocalDateTime.now()) + ".csv";
        Path reportsDir = Paths.get(REPORTS_DIR);
        Files.createDirectories(reportsDir);
        Path csv = reportsDir.resolve(fileName);

        try (FileWriter fw = new FileWriter(csv.toFile())) {
            for (String[] row : csvRows) fw.write(toCsvLine(row));
        }
        System.out.println("\nCSV exported → " + csv.toAbsolutePath());
    }

    private static String getCountryFromLocale(String locale) {
        int idx = locale.indexOf('_');
        return (idx >= 0 && idx + 1 < locale.length()) ? locale.substring(idx + 1).toUpperCase() : locale.toUpperCase();
    }

    private String toCsvLine(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            sb.append('"').append(cells[i].replace("\"", "\"\""))
                    .append('"');
            if (i < cells.length - 1) sb.append(',');
        }
        return sb.append('\n').toString();
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "…";
    }
}