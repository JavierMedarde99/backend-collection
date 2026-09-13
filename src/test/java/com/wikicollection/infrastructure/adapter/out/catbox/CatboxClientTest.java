package com.wikicollection.infrastructure.adapter.out.catbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicollection.application.exception.CatboxUploadException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class CatboxClientTest {

    private MockWebServer server;
    private CatboxClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder().baseUrl(server.url("").toString()).build();
        client = new CatboxClient(restClient, server.url("").toString(), "");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void upload_returnsUrlFromPlainTextResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("https://files.catbox.moe/abc123.png"));

        String url = client.upload(new byte[]{0x00}, "foto.png", "image/png");

        assertThat(url).isEqualTo("https://files.catbox.moe/abc123.png");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/user/api.php");
        assertThat(request.getHeader("Content-Type")).contains("multipart/form-data");
    }

    @Test
    void upload_throwsWhenResponseIsNotUrl() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("error: blocked"));

        assertThatThrownBy(() -> client.upload(new byte[]{0x00}, "foto.png", "image/png"))
                .isInstanceOf(CatboxUploadException.class);
    }

    @Test
    void upload_throwsWhenServerError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> client.upload(new byte[]{0x00}, "foto.png", "image/png"))
                .isInstanceOf(CatboxUploadException.class);
    }

    @Test
    void delete_sendsDeleteRequest() throws Exception {
        RestClient restClient = RestClient.builder().baseUrl(server.url("").toString()).build();
        CatboxClient keyed = new CatboxClient(restClient, server.url("").toString(), "hash123");
        server.enqueue(new MockResponse().setResponseCode(200).setBody("deleted"));

        keyed.delete("abc123.png");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/user/api.php");
        String body = request.getBody().readUtf8();
        assertThat(body).contains("deletefiles").contains("hash123").contains("abc123.png");
    }

    @Test
    void delete_rejectsBlankFilename() {
        RestClient restClient = RestClient.builder().baseUrl(server.url("").toString()).build();
        CatboxClient keyed = new CatboxClient(restClient, server.url("").toString(), "hash123");

        assertThatThrownBy(() -> keyed.delete("../evil.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(server.getRequestCount()).isEqualTo(0);
    }

    @Test
    void delete_requiresUserhash() {
        assertThatThrownBy(() -> client.delete("abc123.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userhash");
        assertThat(server.getRequestCount()).isEqualTo(0);
    }

    @Test
    void upload_includesUserhash_whenConfigured() throws Exception {
        RestClient restClient = RestClient.builder().baseUrl(server.url("").toString()).build();
        CatboxClient keyed = new CatboxClient(restClient, server.url("").toString(), "hash123");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("https://files.catbox.moe/abc123.png"));

        keyed.upload(new byte[]{0x00}, "foto.png", "image/png");

        assertThat(server.takeRequest().getBody().readUtf8()).contains("hash123");
    }
}
