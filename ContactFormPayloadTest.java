package utils;

import base.TestObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * ContactFormPayloadTest
 *
 * Test to verify direct HTTP POST submission to the Krono DC contact form endpoint.
 * It is intended to validate that the backend correctly accepts a full payload and returns a successful response (HTTP 200).
 *
 * TEST FLOW:
 * ------------------------------------------------------------------------------
 * 1. Initialize a timestamp and print the test start time.
 * 2. Prepare a CloseableHttpClient and configure an HttpPost with the form URL.
 * 3. Set required headers to mimic a real browser request:
 *        - Content-Type: application/x-www-form-urlencoded
 *        - Accept: application/json
 * 4. Prepare a full payload string with form parameters, including:
 *       - Name, email, message, agreement checkboxes, honeypot, and recaptcha tokens.
 * 5. Attach the payload to the request body using a StringEntity.
 * 6. Execute the POST request and receive the server response.
 * 7. Print and assert:
 *        - Status code (expected: 200 OK)
 *        - Reason phrase
 *        - Response headers
 *        - Response body
 * 8. Catch and handle any exceptions, logging the error and failing the test if needed.
 * 9. Print the test end time for clear visibility.
 *
 * CAPTCHA NOTE:
 * The test uses a static "g-recaptcha-response" token.
 * Normally, Google reCAPTCHA tokens are single-use and time-limited, and the server should validate them via Google APIs.
 * If the server responds with 200 OK, it may indicate:
 *    - CAPTCHA is not enforced in this environment
 *    - Validation is disabled or mocked
 *    - A honeypot or alternate mechanism bypassed reCAPTCHA
 */


public class ContactFormPayloadTest extends TestObject {

    @Test(priority = 1, description = "Submit contact form with valid payload and validate that HTTP 200 is returned")
    public void submitContactForm_shouldReturnHttp200() throws IOException {
        String url = "https://www.krono-dc.com/en/contact?ajax_form=1&_wrapper_format=drupal_ajax";
        LocalDateTime startTime = LocalDateTime.now();

        System.out.println("======================================");
        System.out.println("TEST: Payload Submission to Contact Form");
        System.out.println("Start Time: " + startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Step 1: Preparing HTTP client and POST request...");
        System.out.println("Target URL: " + url);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);

            System.out.println("Step 2: Setting request headers...");
            post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            post.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");

            String payload = "familiya_=Lazarov&ime_=Slav&imeyl_=slav@abv.bg&telefon_=+35988996633" +
                    "&tema_=Hi&vasheto_sobschenie=Test!" +
                    "&potvrzhdavam_che_imam_navrsheni_18_godini_i_sm_sglasen_s_obrabot=1" +
                    "&potvrzhdavam_i_priemam_obschi_usloviya_i_usloviya_na_polzvane_po=1" +
                    "&sglasen_sm_da_poluchavam_informaciya_za_novi_produkti_promocii_u=1" +
                    "&form_build_id=form-NIzofeoiQJvKJJokaYdNiA5rS0lPbxUASsTGrn6I2H0" +
                    "&form_id=webform_submission_kontakti_paragraph_1189_add_form" +
                    "&honeypot_time=Z9-MOt29-v8TdS0JWl3-chz-R1JZle5WbA3HD1dPX7Y" +
                    "&captcha_sid=1018780" +
                    "&captcha_token=d-DFCerGhYWOfsAdTfkYUa7m337xpJ2NbvGhIUfLuFI" +
                    "&captcha_response=Google%20no%20captcha" +
                    "&g-recaptcha-response=03AFcWeA5IbXajFYWlDCEvGm7PRMDTtvqtOddommUjwFygmFpiMZVve17HtX_aBdohb1LKYUqGmj7V_hun9IZ5vn0u1Qc-0MGCwfilhFZFuzTca_MpSSDGTDqn0v6Cg08wr1gOaLeN2Jyv6JIHZuGWSHObyTe_GfOxZsYAXdRsivh4n_-zDgx3Xx-L7zwsBqeqFaOhY9dbwLMSrlpYv7K9IC6TdY70Mz8EWjB9G9jhxOqNszaRLYF8cp8ddtZzKjLrMk1S0QGL85PoM2tEZo2ddc-hqBnskjHwmhsmAgIkDBlQ0uC9Ghmv2PjkXLMyh7OLORqfjZsf9Xrjy1yrF4ACa0VWLB9sdqP-4INuYZPXSLtm3s0s95CnMyreHGbIYL7nFL1IZIJZUG2UZ6YEE2H8z6ptKQJh8zB_vzBnukayMMiijEGsHZ1X4bKc8pTh4Vpa1iOetjHp6kc3DlqQ-ujyOCg-aAgBVAGlKTt8AGxMQYHi3v7FfXDkd8FSaCv8gNXEreeLU1SCQ2_nRb-A0NA2BGU4XUYgm3fdcVejf3AJwKuCA7OzIs0RM6h3dAPurf1EhBvrOuzQlq3YPCRE9ZlGJQ2PvZzlwGxQXKHCY45IewSjUC61J1agesc8YUH76dWyK52W-biUdckCHSzoTwqdMrFeLFClH7-BxH0pPh3yubc6uPdORWO3_6SDZdGLccLtx8LRGTj6RZykA8bz-aoiCyAq3BRdoNi5b9fszj4wojNfxjg0HvRf3ul6FO49wQl8Mtz-g7vwYdsn7Z3EfNPiPsw3naQMChPGl4qtlRqJ2J1JVlDLA8hepwtTuGUhlpBAHW0JgjLEH1ZQZXmpjQy4J0nuZGnbngvIyvR3JIdDGpp4JTXQ6_pbksXgQF21xJA4iVDAkfYHjGfSIlk-ZGKYp7xtA16pt_ptCumDoeDav4vyzaA4_Jzac2o" +
                    "&captcha_cacheable=1&_triggering_element_name=op" +
                    "&_triggering_element_value=Send&_drupal_ajax=1";

            System.out.println("Step 3: Attaching payload (length: " + payload.length() + " characters)");
            post.setEntity(new StringEntity(payload, ContentType.APPLICATION_FORM_URLENCODED));

            System.out.println("Step 4: Executing POST request...");
            try (CloseableHttpResponse response = client.execute(post)) {
                int statusCode = response.getCode();
                System.out.println("Step 5: Received Response");
                System.out.println(" - HTTP Status Code: " + statusCode);
                System.out.println(" - Reason Phrase: " + response.getReasonPhrase());

                // Print headers
                System.out.println(" - Response Headers:");
                for (Header header : response.getHeaders()) {
                    System.out.println("   " + header.getName() + ": " + header.getValue());
                }

                // Print body
                String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(" - Response Body:");
                System.out.println(responseBody);

                Assert.assertEquals(statusCode, 200, "❌ Payload submission failed.");
                System.out.println("✅ Test PASSED: Server responded with 200 OK");
            }

        } catch (Exception e) {
            System.err.println("❌ Test FAILED due to exception:");
            e.printStackTrace();
            Assert.fail("Test failed due to exception: " + e.getMessage());
        }

        LocalDateTime endTime = LocalDateTime.now();
        System.out.println("End Time: " + endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("======================================");
    }
}