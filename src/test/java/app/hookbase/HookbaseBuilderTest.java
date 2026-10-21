package app.hookbase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HookbaseBuilderTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "https://api.hookbase.app", "https://api.hookbase.app/", "http://localhost:8787",
        "http://127.0.0.1:8787", "http://[::1]:8787", "http://api.localhost"
    })
    void acceptsHttpsAndLoopbackHttp(String url) {
        assertThatCode(() -> Hookbase.builder().apiKey("whr_x").baseUrl(url).build()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://api.hookbase.app", "ftp://api.hookbase.app", "api.hookbase.app",
        "https://user:pw@api.hookbase.app", "https://api.hookbase.app?x=1", "https://api.hookbase.app#f",
        "http://localhost.evil.com"
    })
    void rejectsCleartextOrOddBaseUrls(String url) {
        assertThatThrownBy(() -> Hookbase.builder().apiKey("whr_x").baseUrl(url).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnHttpClientThatFollowsRedirects() {
        HttpClient following = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        assertThatThrownBy(() -> Hookbase.builder().apiKey("whr_x").httpClient(following).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("redirects");
        assertThatCode(() -> Hookbase.builder().apiKey("whr_x").httpClient(HttpClient.newHttpClient()).build())
                .doesNotThrowAnyException();
    }
}
