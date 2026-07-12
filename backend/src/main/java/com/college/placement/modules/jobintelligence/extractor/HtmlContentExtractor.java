package com.college.placement.modules.jobintelligence.extractor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * Reduces a fetched job page to the visible text an LLM should see. Scripts,
 * styles, navigation, headers/footers, cookie banners and other boilerplate are
 * removed; nothing from the page is ever executed. Output is plain text, capped.
 */
@Component
public class HtmlContentExtractor {

    /** Hard cap on prompt content — keeps token usage bounded on huge pages. */
    public static final int MAX_CHARS = 20_000;

    private static final String[] BOILERPLATE_SELECTORS = {
            "script", "style", "noscript", "iframe", "svg", "canvas", "template",
            "nav", "header", "footer", "aside", "form", "button", "input", "select",
            "[role=navigation]", "[role=banner]", "[role=contentinfo]", "[role=dialog]",
            "[aria-hidden=true]",
            "[class*=cookie]", "[id*=cookie]", "[class*=consent]", "[id*=consent]",
            "[class*=banner]", "[class*=advert]", "[id*=advert]", "[class*=popup]"
    };

    /** Extract readable text from raw HTML. Never returns markup. */
    public String extractVisibleText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document document = Jsoup.parse(html);
        for (String selector : BOILERPLATE_SELECTORS) {
            document.select(selector).remove();
        }
        // Prefer the semantic main/article region when present.
        var main = document.selectFirst("main, article, [role=main]");
        String text = (main != null ? main : document.body() != null ? document.body() : document)
                .text();
        String cleaned = text.replaceAll("\\s+", " ").trim();
        return cleaned.length() > MAX_CHARS ? cleaned.substring(0, MAX_CHARS) : cleaned;
    }

    /** Strip ALL markup from a string (used to sanitize every LLM output field). */
    public static String stripHtml(String value) {
        if (value == null) {
            return null;
        }
        // Parse-and-text drops every tag AND decodes entities ("&amp;" → "&"),
        // unlike Jsoup.clean which re-escapes them.
        return Jsoup.parse(value).text().trim();
    }
}
