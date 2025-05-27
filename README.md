# Utils

A collection of Java utility classes and tests designed to streamline common development tasks, including locale management, web scraping, content extraction, and automated testing.

# Overview
The Utils repository encompasses a suite of tools and tests aimed at enhancing development efficiency. Key functionalities include:

Locale Management: Handling country and locale data for internationalization support.

Web Scraping: Extracting structured data from web pages.

Content Extraction: Parsing and retrieving specific content elements.

Automated Testing: Validating application components through unit and integration tests.

# Features
Locale Management
CountryLocaleFetcher: Retrieves locale information for supported countries.

CountryLocaleInfo: Encapsulates locale-specific data.

CountryLocalesTransformation: Transforms and maps locale data for application use.

Web Scraping
CarouselContentExtractor: Parses carousel components from web pages to extract image and text content.

Automated Testing
Comprehensive test suite covering:

Form Validation: Ensures contact forms handle payloads correctly.

Locale Consistency: Verifies consistent locale behavior across different scenarios.

Header Integrity: Checks for proper HTTP header configurations.

Security: Tests for vulnerabilities like SQL injection.

Visual Regression: Detects unintended UI changes through screenshot comparisons.
