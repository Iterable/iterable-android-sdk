package com.iterable.iterableapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IterableWebViewBaseUrlTest extends BaseTest {

    @Before
    public void setUp() {
        IterableTestUtils.createIterableApiNew();
    }

    @After
    public void tearDown() {
        IterableTestUtils.resetIterableApi();
    }

    @Test
    public void testGetWebViewBaseUrl_DefaultConfiguration() {
        // Test: When webViewBaseUrl is not configured, should return empty string
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Default webViewBaseUrl should be empty string", "", baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_CustomConfiguration() {
        // Test: When webViewBaseUrl is configured, should return the configured value
        String customBaseUrl = "https://app.iterable.com";
        
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(customBaseUrl)
                .build();
        
        IterableApi.initialize(getContext(), "test-api-key", config);
        
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Custom webViewBaseUrl should be returned", customBaseUrl, baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_EUConfiguration() {
        // Test: EU region configuration
        String euBaseUrl = "https://app.eu.iterable.com";
        
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(euBaseUrl)
                .build();
        
        IterableApi.initialize(getContext(), "test-api-key", config);
        
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("EU webViewBaseUrl should be returned", euBaseUrl, baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_NullConfiguration() {
        // Test: When webViewBaseUrl is explicitly set to null, should return empty string
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(null)
                .build();
        
        IterableApi.initialize(getContext(), "test-api-key", config);
        
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Null webViewBaseUrl should return empty string", "", baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_EmptyStringConfiguration() {
        // Test: When webViewBaseUrl is explicitly set to empty string, should return empty string
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl("")
                .build();
        
        IterableApi.initialize(getContext(), "test-api-key", config);
        
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Empty webViewBaseUrl should return empty string", "", baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_CustomDomainConfiguration() {
        // Test: Custom domain configuration (for customers with their own domains)
        String customDomain = "https://custom.example.com";
        
        IterableConfig config = new IterableConfig.Builder()
                .setWebViewBaseUrl(customDomain)
                .build();
        
        IterableApi.initialize(getContext(), "test-api-key", config);
        
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Custom domain webViewBaseUrl should be returned", customDomain, baseUrl);
    }

    @Test
    public void testGetWebViewBaseUrl_ExceptionHandling() {
        // Test: Exception handling when SDK is not initialized properly
        IterableTestUtils.resetIterableApi();
        
        // This should not throw an exception and should return empty string
        String baseUrl = IterableUtil.getWebViewBaseUrl();
        assertEquals("Exception case should return empty string", "", baseUrl);
    }
}
