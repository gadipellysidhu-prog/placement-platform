package com.college.placement.jobintelligence;

import com.college.placement.modules.jobintelligence.extractor.HtmlContentExtractor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlContentExtractorTest {

    private final HtmlContentExtractor extractor = new HtmlContentExtractor();

    @Test
    void removesScriptsNavigationAndCookieBanners() {
        String html = """
                <html><head><script>steal()</script><style>.x{}</style></head>
                <body>
                  <nav>Home | About</nav>
                  <div class="cookie-banner">Accept cookies</div>
                  <main><h1>Backend Engineer</h1><p>We need Java and SQL.</p></main>
                  <footer>© Corp</footer>
                </body></html>
                """;

        String text = extractor.extractVisibleText(html);

        assertThat(text).contains("Backend Engineer", "Java and SQL");
        assertThat(text).doesNotContain("steal", "Accept cookies", "Home | About", "© Corp");
    }

    @Test
    void prefersMainContentRegionWhenPresent() {
        String html = "<body><div>sidebar junk</div><article>The real job text</article></body>";

        assertThat(extractor.extractVisibleText(html)).isEqualTo("The real job text");
    }

    @Test
    void capsOutputLength() {
        String html = "<body><main>" + "word ".repeat(10_000) + "</main></body>";

        assertThat(extractor.extractVisibleText(html).length())
                .isLessThanOrEqualTo(HtmlContentExtractor.MAX_CHARS);
    }

    @Test
    void emptyAndNullInputYieldEmptyString() {
        assertThat(extractor.extractVisibleText(null)).isEmpty();
        assertThat(extractor.extractVisibleText("  ")).isEmpty();
    }

    @Test
    void stripHtmlRemovesEveryTag() {
        assertThat(HtmlContentExtractor.stripHtml("<b>Java</b> &amp; <i>SQL</i>"))
                .isEqualTo("Java & SQL");
        assertThat(HtmlContentExtractor.stripHtml(null)).isNull();
    }
}
