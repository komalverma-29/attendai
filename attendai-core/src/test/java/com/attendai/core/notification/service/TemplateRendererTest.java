package com.attendai.core.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private TemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TemplateRenderer();
    }

    @Test
    void render_shouldSubstituteAllVariables() {
        String template = "Hello {{firstName}}, click {{resetLink}} to reset your password.";
        Map<String, String> vars = Map.of(
                "firstName",  "John",
                "resetLink",  "https://example.com/reset");

        String result = renderer.render(template, vars);

        assertThat(result).isEqualTo("Hello John, click https://example.com/reset to reset your password.");
    }

    @Test
    void render_shouldReplaceWithEmpty_whenVariableMissing() {
        String template = "Hello {{firstName}}, your code is {{code}}.";

        String result = renderer.render(template, Map.of("firstName", "Jane"));

        assertThat(result).isEqualTo("Hello Jane, your code is .");
    }

    @Test
    void render_shouldHandleNullVariablesMap() {
        String template = "Hello {{firstName}}.";

        String result = renderer.render(template, null);

        assertThat(result).isEqualTo("Hello .");
    }

    @Test
    void render_shouldReturnEmpty_forNullTemplate() {
        assertThat(renderer.render(null, Map.of())).isEqualTo("");
    }

    @Test
    void render_shouldReturnEmpty_forBlankTemplate() {
        assertThat(renderer.render("   ", Map.of())).isEqualTo("");
    }

    @Test
    void render_shouldHandleNoPlaceholders() {
        String template = "No variables here.";
        assertThat(renderer.render(template, Map.of("key", "value"))).isEqualTo("No variables here.");
    }

    @Test
    void render_shouldHandleDollarSignInReplacementWithoutError() {
        // Ensures Matcher.quoteReplacement is applied correctly
        String template = "Amount: {{amount}}";
        String result = renderer.render(template, Map.of("amount", "$100.00"));
        assertThat(result).isEqualTo("Amount: $100.00");
    }

    @Test
    void render_shouldSubstituteMultipleOccurrences() {
        String template = "{{name}} joined. Welcome {{name}}!";
        String result = renderer.render(template, Map.of("name", "Alice"));
        assertThat(result).isEqualTo("Alice joined. Welcome Alice!");
    }
}
