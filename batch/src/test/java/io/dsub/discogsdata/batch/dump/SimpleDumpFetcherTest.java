package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SimpleDumpFetcherTest {

    private final SimpleDumpFetcher simpleDumpFetcher = new SimpleDumpFetcher();

    @BeforeAll
    static void assumeConnectionAvailable() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SimpleDumpFetcher.LAST_KNOWN_BUCKET_URL))
                .GET()
                .build();

        int respStatus = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofLines())
                .statusCode();

        assumeTrue(respStatus == 200);
    }

    @Test
    void getDiscogsDumps() {
        List<DiscogsDump> dumpList = simpleDumpFetcher.getDiscogsDumps();
        assertThat(dumpList)
                .isNotNull()
                .hasNoNullFieldsOrProperties();
    }
}