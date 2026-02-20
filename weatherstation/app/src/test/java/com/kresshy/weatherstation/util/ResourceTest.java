package com.kresshy.weatherstation.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for the {@link Resource} wrapper. Verifies that the factory methods correctly set
 * status, data, and messages.
 */
public class ResourceTest {

    @Test
    public void success_CreatesSuccessResource() {
        String data = "some data";
        Resource<String> resource = Resource.success(data);

        assertEquals(Resource.Status.SUCCESS, resource.status);
        assertEquals(data, resource.data);
        assertNull(resource.message);
    }

    @Test
    public void error_CreatesErrorResource() {
        String data = "previous data";
        String message = "error message";
        Resource<String> resource = Resource.error(message, data);

        assertEquals(Resource.Status.ERROR, resource.status);
        assertEquals(data, resource.data);
        assertEquals(message, resource.message);
    }

    @Test
    public void loading_CreatesLoadingResource() {
        String data = "loading data";
        Resource<String> resource = Resource.loading(data);

        assertEquals(Resource.Status.LOADING, resource.status);
        assertEquals(data, resource.data);
        assertNull(resource.message);
    }
}
