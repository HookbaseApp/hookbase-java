package app.hookbase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VersionTest {
    @Test
    void versionMatchesGradleProperties() {
        assertThat(Hookbase.VERSION).isEqualTo(System.getProperty("hookbase.expectedVersion")).isNotEqualTo("unknown");
    }
}
