package org.a_hahn.rpi.tools;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class HttpClientPost {

    /**
     * Sends a POST request with PHP-style array syntax for contacts
     * @param baseUrl Base URL without parameters
     * @param name Path variable
     * @param ok Boolean path variable
     * @param contacts Array of integers (sent as contacts[])
     * @return HTTP status code or -1 if request failed
     */
    public static int sendPostRequest(String baseUrl, String name, boolean ok, int[] contacts) {

        final Logger log = LoggerFactory.getLogger(HttpClientPost.class);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Construct URL with path variables
            String url = String.format("%s/%s/%s",
                    baseUrl, URLEncoder.encode(name, "UTF-8"), ok);

            HttpPost httpPost = new HttpPost(url);

            // Using PHP-style array syntax for contacts
            List<NameValuePair> params = new ArrayList<>();
            for (int contact : contacts) {
                params.add(new BasicNameValuePair("contacts[]", String.valueOf(contact)));
            }

            httpPost.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return response.getStatusLine().getStatusCode();
            }

        } catch (IOException e) {
            log.error("Request failed: " + e.getMessage());
            return -1;  // Return -1 to indicate failure
        }
    }

}