package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.FileWriter;
import java.io.IOException;

import java.time.Duration;
import java.util.*;

public class CarouselContentExtractor {
    private final List<String[]> errorLog = new ArrayList<>();
    private final WebDriver driver;
    private final WebDriverWait wait;

    public CarouselContentExtractor(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.driver.get("https://stage.kronospan.com/en_EN/");
    }

    public List<Map<String, String>> extractCarouselContent() {
        List<Map<String, String>> extractedItems = new ArrayList<>();
        try {
            // Wait for carousel to be present
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(".carousel-inner .carousel-item")));
            List<WebElement> slides = driver.findElements(By.cssSelector(".carousel-inner .carousel-item"));

            // Try to get next button for navigation
            WebElement nextButton = null;
            try {
                nextButton = driver.findElement(By.cssSelector(".carousel-control-next"));
            } catch (Exception e) {
                System.out.println("[WARN] ⚠️ Next button not found. Carousel might not scroll.");
            }

            Set<String> seenTitles = new HashSet<>();

            for (int i = 0; i < slides.size(); i++) {
                WebElement activeSlide = driver.findElement(By.cssSelector(".carousel-inner .carousel-item.active"));

                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", activeSlide);
                wait.until(ExpectedConditions.visibilityOf(activeSlide));

                Map<String, String> data = new HashMap<>();

                // Extract <h1> title
                try {
                    WebElement h1 = activeSlide.findElement(By.xpath(".//h1"));
                    data.put("title", h1.getText().trim());
                } catch (Exception e) {
                    data.put("title", "[MISSING]");
                }

                // Extract <p> paragraph
                try {
                    WebElement p = activeSlide.findElement(By.xpath(".//p"));
                    data.put("paragraph", p.getText().trim());
                } catch (Exception e) {
                    data.put("paragraph", "[MISSING]");
                }

                // Extract <a> href
                try {
                    WebElement a = activeSlide.findElement(By.xpath(".//a[1]"));
                    data.put("href", a.getAttribute("href"));
                } catch (Exception e) {
                    data.put("href", "[MISSING]");
                }

                // Extract background image name
                try {
                    WebElement bg = activeSlide.findElement(By.cssSelector(".slider-img"));
                    String style = bg.getAttribute("style");
                    if (style != null && style.contains("background-image")) {
                        String imageUrl = style.replaceAll(".*url\\(['\"]?(.*?)['\"]?\\).*", "$1");
                        String imageName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                        data.put("imageName", imageName);
                    } else {
                        data.put("imageName", "[MISSING]");
                    }
                } catch (Exception e) {
                    data.put("imageName", "[MISSING]");
                }

                // Avoid duplicates
                if (!seenTitles.contains(data.get("title"))) {
                    seenTitles.add(data.get("title"));
                    extractedItems.add(data);
                }

                // Click next for next slide
                if (nextButton != null && i < slides.size() - 1) {
                    nextButton.click();
                    Thread.sleep(50); // let animation play
                }
            }

        } catch (Exception e) {
            System.out.println("[ERROR] ❌ Exception during carousel content extraction: " + e.getMessage());
        }

        return extractedItems;
    }

    public void printCarouselContent() {
        List<Map<String, String>> items = extractCarouselContent();

        System.out.println("\n============================================================");
        System.out.println("📊 EXTRACTED CAROUSEL DATA FROM https://stage.kronospan.com/en_EN/");
        System.out.println("============================================================");

        for (int i = 0; i < items.size(); i++) {
            Map<String, String> item = items.get(i);
            System.out.println("\n------------------------------------------------------------");
            System.out.println("🖼️  SLIDE #" + i);
            System.out.println("------------------------------------------------------------");

            String title = item.getOrDefault("title", "[MISSING]");
            String paragraph = item.getOrDefault("paragraph", "[MISSING]");
            String href = item.getOrDefault("href", "[MISSING]");
            String imageName = item.getOrDefault("imageName", "[MISSING]");

            System.out.println("🔹 Title       : " + (title.equals("[MISSING]") || title.isBlank() ? "is missing" : title));
            System.out.println("🔹 Paragraph   : " + (paragraph.equals("[MISSING]") || paragraph.isBlank() ? "is missing" : paragraph));
            System.out.println("🔹 Link (href) : " + (href.equals("[MISSING]") || href.isBlank() ? "is missing" : href));
            System.out.println("🔹 Image Name  : " + (imageName.equals("[MISSING]") || imageName.isBlank() ? "is missing" : imageName));
        }

        System.out.println("\n============================================================");
        System.out.println("✅ TOTAL SLIDES EXTRACTED: " + items.size());
        System.out.println("============================================================");
    }


    public void exportToCsv(List<Map<String, String>> items, String filePath) {
        try (FileWriter csvWriter = new FileWriter(filePath)) {
            csvWriter.append("Locale,Title,Paragraph,Href,ImageName,isImageMissing\n");

            for (Map<String, String> item : items) {
                String imageName = item.getOrDefault("imageName", "").isBlank() ? "image-name-missing" : item.get("imageName");
                String isImageMissing = imageName.equals("image-name-missing") ? "yes" : "no";

                csvWriter.append("\"").append(item.getOrDefault("locale", "")).append("\",")
                        .append("\"").append(item.getOrDefault("title", "")).append("\",")
                        .append("\"").append(item.getOrDefault("paragraph", "")).append("\",")
                        .append("\"").append(item.getOrDefault("href", "")).append("\",")
                        .append("\"").append(imageName).append("\",")
                        .append(isImageMissing).append("\n");
            }

            System.out.println("✅ Combined carousel content exported to CSV: " + filePath);
        } catch (IOException e) {
            System.out.println("❌ Failed to export combined carousel content to CSV.");
            e.printStackTrace();
        }
    }


    public List<Map<String, String>> extractCarouselContentFromUrl(String url) {
        try {
            driver.get(url);
        } catch (Exception e) {
            System.out.println("[ERROR] ❌ Failed to load URL: " + url);
            return Collections.emptyList();
        }
        return extractCarouselContent();
    }

    public void exportErrorLog(String filePath) {
        if (errorLog.isEmpty()) return;

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("URL,Reason,Exception,Message,Timestamp\n");
            for (String[] row : errorLog) {
                writer.write(String.join(",", Arrays.stream(row)
                        .map(s -> "\"" + s.replace("\"", "\"\"") + "\"")
                        .toArray(String[]::new)) + "\n");
            }
            System.out.println("⚠️ Error log written to CSV: " + filePath);
        } catch (IOException e) {
            System.out.println("❌ Failed to export error log.");
            e.printStackTrace();
        }
    }
}