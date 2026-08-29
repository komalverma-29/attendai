package com.attendai.core.notification.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders notification templates by substituting {@code {{variable}}} placeholders
 * with values from the provided variable map.
 *
 * <p>Missing variables are replaced with an empty string (lenient mode).
 * This avoids rendering failures for optional variables while ensuring
 * required variables should be populated by the caller before dispatch.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{(\\w+)\\}\\}");

    /**
     * Renders the template body by substituting all {@code {{key}}} placeholders.
     *
     * @param template  the raw template string with placeholders
     * @param variables the variable values; missing keys resolve to ""
     * @return the rendered string
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isBlank()) {
            return "";
        }
        Map<String, String> vars = (variables != null) ? variables : Map.of();

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key         = matcher.group(1);
            String replacement = vars.getOrDefault(key, "");
            // Escape $ and \ in replacement to avoid Matcher.appendReplacement issues
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
