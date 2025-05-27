package utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class CountryLocaleFetcher {

    // Main method to fetch country and locale information from the database
    public Map<String, CountryLocaleInfo> getCountryLocales(Connection conn) {
        // This map will store the result where key is ISO country code and value is utls.CountryLocaleInfo
        Map<String, CountryLocaleInfo> countryLocalesMap = new LinkedHashMap<>();

        // SQL query to retrieve country and locale data
        String query = "SELECT c.iso_code, cl.locale, c.title, c.default_language " +
                "FROM countries c " +
                "INNER JOIN countries_languages cl_map ON cl_map.country_id = c.id " +
                "INNER JOIN languages cl ON cl.id = cl_map.language_id " +
                "WHERE c.is_active = 1 " +
                "GROUP BY c.id, cl.locale " +
                "ORDER BY c.id = 1 DESC, c.title ASC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Iterate through each row of the result set
            while (rs.next()) {
                // Read values from the current row
                String isoCode = rs.getString("iso_code");
                String locale = rs.getString("locale");
                String title = rs.getString("title");
                String defaultLang = rs.getString("default_language");

                // Get the existing utls.CountryLocaleInfo object if present, or create a new one
                CountryLocaleInfo info = countryLocalesMap.getOrDefault(isoCode, new CountryLocaleInfo());

                // Populate the utls.CountryLocaleInfo object with data
                info.setIsoCode(isoCode);
                info.setCountryName(title); // Fallback value for country name
                info.setTitle(title); // Possibly a more display-friendly title
                info.setDefaultLanguage(defaultLang);

                // Manage the list of locales for the country
                List<String> locales = info.getLocales();
                if (locales == null) locales = new ArrayList<>();
                if (!locales.contains(locale)) locales.add(locale); // Avoid duplicates
                info.setLocales(locales);

                // Add or update the map with the current country info
                countryLocalesMap.put(isoCode, info);
            }

        } catch (SQLException e) {
            // Handle SQL exceptions and print the error message
            System.err.println("SQL error while fetching country locales: " + e.getMessage());
        }

        // Return the populated map
        return countryLocalesMap;
    }
}