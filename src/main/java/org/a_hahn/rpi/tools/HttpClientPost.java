package org.a_hahn.rpi.tools;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
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
    public static int sendPostRequest(String baseUrl, String name, boolean ok, Collection<Integer> contacts) {

        final Logger log = LoggerFactory.getLogger(HttpClientPost.class);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();

        try {
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
                int ret = response.getStatusLine().getStatusCode();
                if (ret >= 100 && ret < 400) {
                    log.debug("Message sent to " + baseUrl );
                } else {
                    log.error("Error " + ret + " sending message to " + baseUrl);
                }
                return ret;
            }

        } catch (IOException ioex) {
            log.error("Could not send message to endpoint " + baseUrl + " " + ioex.getMessage());
            return -1;  // Return -1 to indicate failure
        }
    }

}