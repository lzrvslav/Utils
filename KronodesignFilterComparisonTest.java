package utils;

import base.TestObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.testng.annotations.Test;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * KronodesignFilterComparisonTest
 *
 * Compares filter lists between LIVE and STAGE environments for Kronodesign collections.
 * Outputs a detailed console report and saves a CSV report in the reports directory.
 */
public class KronodesignFilterComparisonTest extends TestObject {

    private static final List<String> COLLECTION_TITLES = Arrays.asList(
            "Collection", "Decor", "Texture", "Theme", "Product", "Application", "Material"
    );

    private int totalCollectionsTested = 0;
    private int collectionsWithIssues = 0;

    @Test(priority = 1, description = "Compare filters between LIVE and STAGE environments and export results to CSV.")
    public void compareFiltersBetweenLiveAndStage() throws IOException {
        File liveFile = new File("src/test/resources/Live filter-names DOM.txt");
        File stageFile = new File("src/test/resources/Stage filter-names DOM.txt");
        File csvFile = new File(REPORTS_DIR + "filter-comparison-report.csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            writer.write("Collection,Status,LiveFilters,StageFilters,MissingInStage,ExtraInStage,DuplicateInLive,DuplicateInStage");
            writer.newLine();

            Document liveDoc = Jsoup.parse(liveFile, "UTF-8");
            Document stageDoc = Jsoup.parse(stageFile, "UTF-8");

            System.out.println("\n======= KRONODESIGN FILTER COMPARISON TEST START =======\n");

            for (String collectionTitle : COLLECTION_TITLES) {
                totalCollectionsTested++;
                List<String> liveFilters = extractFiltersForCollection(liveDoc, collectionTitle);
                List<String> stageFilters = extractFiltersForCollection(stageDoc, collectionTitle);

                boolean hasIssues = compareFiltersAndWriteCsv(liveFilters, stageFilters, collectionTitle, writer);

                if (hasIssues) {
                    collectionsWithIssues++;
                }
            }

            System.out.println("\n======= TEST COMPLETE =======");
            System.out.println("Total collections tested: " + totalCollectionsTested);
            System.out.println("Collections with issues: " + collectionsWithIssues);
            System.out.println("CSV report saved at: " + csvFile.getAbsolutePath());
        }
    }

    private boolean compareFiltersAndWriteCsv(List<String> liveFilters, List<String> stageFilters, String collectionTitle, BufferedWriter writer) throws IOException {
        Set<String> liveSet = new HashSet<>(liveFilters);
        Set<String> stageSet = new HashSet<>(stageFilters);

        Set<String> missingInStage = new HashSet<>(liveSet);
        missingInStage.removeAll(stageSet);

        Set<String> extraInStage = new HashSet<>(stageSet);
        extraInStage.removeAll(liveSet);

        Map<String, Integer> duplicatesInLive = findDuplicatesWithCount(liveFilters);
        Map<String, Integer> duplicatesInStage = findDuplicatesWithCount(stageFilters);

        boolean hasIssues = !missingInStage.isEmpty() || !extraInStage.isEmpty() || !duplicatesInLive.isEmpty() || !duplicatesInStage.isEmpty();

        // Sort for readable console output
        List<String> sortedLiveFilters = new ArrayList<>(liveFilters);
        List<String> sortedStageFilters = new ArrayList<>(stageFilters);
        Collections.sort(sortedLiveFilters);
        Collections.sort(sortedStageFilters);

        List<String> sortedMissing = new ArrayList<>(missingInStage);
        List<String> sortedExtra = new ArrayList<>(extraInStage);
        Collections.sort(sortedMissing);
        Collections.sort(sortedExtra);

        String liveFiltersStr = String.join(";", liveFilters);
        String stageFiltersStr = String.join(";", stageFilters);
        String missingStr = String.join(";", sortedMissing);
        String extraStr = String.join(";", sortedExtra);
        String duplicatesLiveStr = duplicatesInLive.entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(";"));
        String duplicatesStageStr = duplicatesInStage.entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(";"));

        // Write CSV row
        writer.write(String.format("\"%s\",%s,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                collectionTitle,
                hasIssues ? "ISSUES" : "OK",
                liveFiltersStr,
                stageFiltersStr,
                missingStr,
                extraStr,
                duplicatesLiveStr,
                duplicatesStageStr));
        writer.newLine();

        // ======= DETAILED CONSOLE OUTPUT =======
        System.out.println("========== COLLECTION: \"" + collectionTitle + "\" ==========");
        System.out.println("LIVE filters (" + sortedLiveFilters.size() + "): " + sortedLiveFilters);
        System.out.println("STAGE filters (" + sortedStageFilters.size() + "): " + sortedStageFilters);

        List<String> matchingFilters = sortedLiveFilters.stream().filter(stageSet::contains).collect(Collectors.toList());
        System.out.println("\n✅ Matching filters (" + matchingFilters.size() + "): " + matchingFilters);

        System.out.println("❌ Missing in STAGE (" + sortedMissing.size() + "): " + sortedMissing);
        System.out.println("❌ Extra in STAGE (" + sortedExtra.size() + "): " + sortedExtra);

        if (!duplicatesInLive.isEmpty()) {
            System.out.println("⚠️ Duplicate filters in LIVE (" + duplicatesInLive.size() + "):");
            duplicatesInLive.forEach((filter, count) -> System.out.println("   - \"" + filter + "\" appears " + count + " times"));
        } else {
            System.out.println("⚠️ Duplicate filters in LIVE (0): None");
        }

        if (!duplicatesInStage.isEmpty()) {
            System.out.println("⚠️ Duplicate filters in STAGE (" + duplicatesInStage.size() + "):");
            duplicatesInStage.forEach((filter, count) -> System.out.println("   - \"" + filter + "\" appears " + count + " times"));
        } else {
            System.out.println("⚠️ Duplicate filters in STAGE (0): None");
        }

        System.out.println("\n📝 SUMMARY for \"" + collectionTitle + "\": " + (hasIssues ? "ISSUES FOUND" : "ALL OK"));
        System.out.println("----------------------------------------\n");

        return hasIssues;
    }

    private List<String> extractFiltersForCollection(Document doc, String collectionTitle) {
        List<String> filters = new ArrayList<>();
        Elements categories = doc.select(".filter-cat-name-wrap");

        for (Element category : categories) {
            String title = category.select(".filter-cat-name").text().trim();
            if (title.equalsIgnoreCase(collectionTitle)) {
                Elements items = category.select("ul li div");
                for (Element item : items) {
                    String filterName = item.text().trim();
                    if (!filterName.isEmpty()) {
                        filters.add(filterName);
                    }
                }
            }
        }
        return filters;
    }

    private Map<String, Integer> findDuplicatesWithCount(List<String> list) {
        Map<String, Integer> counts = new HashMap<>();
        for (String item : list) {
            counts.put(item, counts.getOrDefault(item, 0) + 1);
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
