package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void openApi_documentsClientSideLogout() {
        var openApi = new OpenApiConfig().openAPI();

        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openApi.getInfo().getDescription()).containsIgnoringCase("logout");
    }
}
