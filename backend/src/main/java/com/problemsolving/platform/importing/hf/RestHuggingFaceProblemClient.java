package com.problemsolving.platform.importing.hf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.problemsolving.platform.importing.config.ProblemImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves dataset rows through the Hugging Face datasets-server rows API
 * (https://{api-base-url}/rows?dataset=...&config=...&split=...&offset=...&length=...).
 * Nothing is downloaded locally - each request returns at most 100 rows and this client
 * pages internally so callers can request batches of any size.
 */
@Component
public class RestHuggingFaceProblemClient implements HuggingFaceProblemClient {

    private static final Logger log = LoggerFactory.getLogger(RestHuggingFaceProblemClient.class);

    /** Hard limit of the datasets-server /rows endpoint. */
    private static final int MAX_ROWS_PER_REQUEST = 100;

    private final ProblemImportProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RestHuggingFaceProblemClient(ProblemImportProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .requestFactory(requestFactory)
                .build();

        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public List<HfProblemRow> fetchRows(long offset, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<HfProblemRow> result = new ArrayList<>();
        long cursor = offset;
        while (result.size() < limit) {
            int length = Math.min(MAX_ROWS_PER_REQUEST, limit - result.size());
            List<RowWrapper> page = fetchPage(cursor, length);

            if (page.isEmpty()) {
                break; // end of the split reached
            }

            for (RowWrapper wrapper : page) {
                if (wrapper != null && wrapper.row() != null) {
                    result.add(wrapper.row());
                }
            }

            cursor += page.size();
            if (page.size() < length) {
                break; // short page -> last page of the split
            }
        }

        log.debug("Fetched {} dataset rows at offset {} (requested {})", result.size(), offset, limit);
        return result;
    }

    private List<RowWrapper> fetchPage(long offset, int length) {
        java.net.URI uri = UriComponentsBuilder.fromPath("/rows")
                .queryParam("dataset", properties.getDataset())
                .queryParam("config", properties.getDatasetConfig())
                .queryParam("split", properties.getSplit())
                .queryParam("offset", offset)
                .queryParam("length", length)
                .encode()
                .build()
                .toUri();

        String body;
        try {
            body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            throw new HuggingFaceClientException(
                    "Hugging Face rows API request failed (offset=" + offset + ", length=" + length + "): " + ex.getMessage(), ex);
        }

        if (body == null || body.isBlank()) {
            throw new HuggingFaceClientException(
                    "Hugging Face rows API returned an empty response (offset=" + offset + ", length=" + length + ")");
        }

        try {
            RowsResponse response = objectMapper.readValue(body, RowsResponse.class);
            return response.rows() == null ? List.of() : response.rows();
        } catch (Exception ex) {
            throw new HuggingFaceClientException("Could not parse the Hugging Face rows API response: " + ex.getMessage(), ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RowsResponse(List<RowWrapper> rows) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RowWrapper(HfProblemRow row) {
    }
}
