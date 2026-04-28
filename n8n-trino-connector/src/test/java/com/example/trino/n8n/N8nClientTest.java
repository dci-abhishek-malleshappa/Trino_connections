package com.example.trino.n8n;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class N8nClientTest
{
    @Test
    void encodesQueryParametersAndUsesConfiguredPageSize() throws Exception
    {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("{\"data\":[],\"hasMore\":false}"));
            server.start();

            N8nClient client = new N8nClient(new N8nConfig(Map.of(
                    "n8n.base-url", server.url("/webhook").toString(),
                    "n8n.page-size", "250")));

            client.getDataPage("sales & ops", "orders/2026", 500);

            RecordedRequest request = server.takeRequest();
            String path = request.getPath();
            assertTrue(path.startsWith("/webhook/data?"));
            assertTrue(path.contains("schema=sales+%26+ops"));
            assertTrue(path.contains("table=orders%2F2026"));
            assertTrue(path.contains("offset=500"));
            assertTrue(path.contains("limit=250"));
        }
    }

    @Test
    void normalizesBaseUrlByRemovingTrailingSlash()
    {
        N8nConfig config = new N8nConfig(Map.of("n8n.base-url", "http://localhost:5678/webhook/"));
        assertEquals("http://localhost:5678/webhook", config.getBaseUrl());
    }

    @Test
    void canUsePostForDataRequests()
            throws Exception
    {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("{\"data\":[],\"hasMore\":false}"));
            server.start();

            N8nClient client = new N8nClient(new N8nConfig(Map.of(
                    "n8n.base-url", server.url("/webhook").toString(),
                    "n8n.data-method", "POST")));

            client.getDataPage("default", "ledgers", 0);

            RecordedRequest request = server.takeRequest();
            assertEquals("POST", request.getMethod());
            assertTrue(request.getPath().startsWith("/webhook/data?"));
        }
    }

    @Test
    void extractsTableNamesFromObjectMetadataResponses() throws Exception
    {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("""
                    {
                      "tables": [
                        {"schema": "default", "name": "ledgers"},
                        {"schema": "default", "table": "groups"}
                      ]
                    }
                    """));
            server.start();

            N8nClient client = new N8nClient(new N8nConfig(Map.of(
                    "n8n.base-url", server.url("/webhook/tally").toString())));

            assertEquals(List.of("ledgers", "groups"), client.getTables("default"));
        }
    }

    @Test
    void supportsLegacyEndpointConfigurationForDirectWebhookPayloads() throws Exception
    {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("""
                    {
                      "items": [
                        {"item_id": "1", "name": "Product A", "rate": 100, "active": true},
                        {"item_id": "2", "name": "Product B", "rate": 200, "active": false}
                      ]
                    }
                    """));
            server.enqueue(new MockResponse().setBody("""
                    {
                      "items": [
                        {"item_id": "1", "name": "Product A", "rate": 100, "active": true},
                        {"item_id": "2", "name": "Product B", "rate": 200, "active": false}
                      ]
                    }
                    """));
            server.enqueue(new MockResponse().setBody("""
                    {
                      "items": [
                        {"item_id": "1", "name": "Product A", "rate": 100, "active": true},
                        {"item_id": "2", "name": "Product B", "rate": 200, "active": false}
                      ]
                    }
                    """));
            server.start();

            N8nClient client = new N8nClient(new N8nConfig(Map.of(
                    "endpoint", server.url("/webhook/zoho-books").toString())));

            assertEquals(List.of("books"), client.getSchemas());
            assertEquals(List.of("items"), client.getTables("books"));
            assertEquals(4, client.getTableDefinition("books", "items").getColumns().size());
            assertEquals(2, client.getDataPage("books", "items", 0).data().size());
        }
    }

    @Test
    void fallsBackToFallbackEndpointWhenPrimaryEndpointFails() throws Exception
    {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
            server.enqueue(new MockResponse().setBody("""
                    {
                      "items": [
                        {"item_id": "1", "name": "Recovered"}
                      ]
                    }
                    """));
            server.start();

            String failing = server.url("/webhook/primary").toString();
            String fallback = server.url("/webhook-test/zoho-books").toString();
            N8nClient client = new N8nClient(new N8nConfig(Map.of(
                    "endpoint", failing,
                    "fallback-endpoint", fallback)));

            assertEquals(List.of("items"), client.getTables("books"));
        }
    }

    @Test
    void rejectsUnknownTableNamesInDirectEndpointMode() throws Exception
    {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("""
                    {
                      "items": [
                        {"item_id": "1"}
                      ]
                    }
                    """));
            server.start();

            N8nClient client = new N8nClient(new N8nConfig(Map.of(
                    "endpoint", server.url("/webhook/zoho-books").toString())));

            N8nClient.N8nHttpException error = assertThrows(
                    N8nClient.N8nHttpException.class,
                    () -> client.getTableDefinition("books", "unknown"));
            assertEquals(404, error.getStatusCode());
        }
    }
}
