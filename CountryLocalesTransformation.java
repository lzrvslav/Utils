package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CountryLocalesTransformation
 * Transform and export locale data for each country ISO code.
 * - Convert raw locale strings (e.g., "en-US") into standardized format (e.g., "en_AL").
 * - Generate transformed locales using provided country ISO suffix.
 * - Save locale transformation results to a CSV file.
 * - Include metadata like country name, default language, and title in output.
 * Ensure consistent formatting for locale-based navigation and testing.
 */

public class CountryLocalesTransformation {

    public void saveToCsv(Map<String, CountryLocaleInfo> localesMap, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("iso_code,country_name,default_language,title,transformed_locales\n");

            for (Map.Entry<String, CountryLocaleInfo> entry : localesMap.entrySet()) {
                String iso = entry.getKey();  // Country ISO code
                String name = entry.getValue().getCountryName();
                String defaultLang = entry.getValue().getDefaultLanguage();
                String title = entry.getValue().getTitle();

                // Transform locales using the ISO suffix
                List<String> transformed = transformLocales(entry.getValue().getLocales(), iso);

                writer.write(String.format(
                        "%s,%s,%s,%s,%s\n",
                        iso, name, defaultLang, title, String.join(";", transformed))
                );
            }

            System.out.println("CSV saved at: " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving CSV: " + e.getMessage());
        }
    }

    /**
     * Transforms a list of locale strings (e.g., "en-US", "sr-SP") into
     * a format like "en_AL", "sr_AL" where AL is the given country ISO code.
     *
     * @param locales List of original locales from the DB.
     * @param isoCode ISO code of the country to use as suffix.
     * @return List of transformed locales.
     */
    public List<String> transformLocales(List<String> locales, String isoCode) {
        return locales.stream()
                .map(locale -> {
                    String[] parts = locale.split("[-_]");
                    if (parts.length > 0) {
                        return parts[0] + "_" + isoCode;  // language + "_" + country ISO
                    }
                    return locale.replace("-", "_");
                })
                .collect(Collectors.toList());
    }
}