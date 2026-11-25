package com.liquilabs.vankoo.iam.infrastructure.mail.acs;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcsConnectionStringTest {

    @Test
    void readsTheEndpointAndDecodesTheKey() {
        var connection = AcsConnectionString.parse(
                "endpoint=https://vankoo-acs.unitedstates.communication.azure.com/;AccessKey=YS10ZXN0LWFjY2Vzcy1rZXk=;");

        assertThat(connection.endpoint()).hasHost("vankoo-acs.unitedstates.communication.azure.com");
        assertThat(new String(connection.accessKey(), StandardCharsets.UTF_8)).isEqualTo("a-test-access-key");
    }

    @Test
    void rejectsAStringWithoutKey() {
        assertThatThrownBy(() -> AcsConnectionString.parse("endpoint=https://x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accesskey");
        assertThatThrownBy(() -> AcsConnectionString.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
