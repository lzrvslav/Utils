import base.TestObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;

import javax.imageio.ImageIO;
import java.io.File;
import java.time.Duration;

public class VisualRegressionTest extends TestObject {

    private final String liveUrl = "https://kronospan.com/en_EN/products/by_category/kronobuild/";
    private final String stageUrl = "https://stage.kronospan.com/en_EN/products/by_category/kronobuild/";

    private final By cookiePopup = By.cssSelector(".cky-consent-container");
    private final By rejectButton = By.cssSelector(".cky-btn-reject");

    private Screenshot capturePageScreenshot(String url, String fileName) throws Exception {
        webDriver.get(url);

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        try {
            WebElement popup = wait.until(ExpectedConditions.presenceOfElementLocated(cookiePopup));
            WebElement reject = popup.findElement(rejectButton);
            reject.click();
            wait.until(ExpectedConditions.invisibilityOf(popup));
        } catch (Exception ignored) {
            // Popup may not appear – ignore
        }

        Screenshot screenshot = new AShot()
                .coordsProvider(new WebDriverCoordsProvider())
                .shootingStrategy(ru.yandex.qatools.ashot.shooting.ShootingStrategies.viewportPasting(100))
                .takeScreenshot(webDriver);

        ImageIO.write(screenshot.getImage(), "PNG", new File(SCREENSHOT_DIR + fileName));
        return screenshot;
    }

    @Test
    public void compareLiveAndStageVisuals() throws Exception {
        Screenshot liveScreenshot = capturePageScreenshot(liveUrl, "live.png");
        Screenshot stageScreenshot = capturePageScreenshot(stageUrl, "stage.png");

        ImageDiffer imgDiff = new ImageDiffer();
        ImageDiff diff = imgDiff.makeDiff(liveScreenshot, stageScreenshot);

        if (diff.hasDiff()) {
            ImageIO.write(diff.getMarkedImage(), "PNG", new File(SCREENSHOT_DIR + "difference.png"));
        }

        Assert.assertFalse(diff.hasDiff(), "Stage and Live pages differ visually. See " + SCREENSHOT_DIR + "difference.png");
    }
}