package com.helpdesk.ai.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

/** Validated result of /ticket/analyze - the LLM's JSON, checked against the allowed values. */
public record TicketAnalysis(String sentiment, String category) {

    static final Set<String> SENTIMENTS = Set.of("POSITIVE", "NEUTRAL", "NEGATIVE");
    static final Set<String> CATEGORIES = Set.of("BUG", "BILLING", "ACCESS", "HOW_TO", "FEATURE_REQUEST");

    /** @throws IllegalArgumentException if the text is not JSON with allowed sentiment/category */
    public static TicketAnalysis parse(String raw, ObjectMapper mapper) {
        if (raw == null) {
            throw new IllegalArgumentException("Empty model response");
        }
        // Models often wrap JSON in markdown code fences.
        String json = raw.strip()
                .replaceAll("^`{3}(?:json)?\\s*", "")
                .replaceAll("\\s*`{3}$", "");
        try {
            TicketAnalysis a = mapper.readValue(json, TicketAnalysis.class);
            String s = a.sentiment() == null ? "" : a.sentiment().toUpperCase();
            String c = a.category() == null ? "" : a.category().toUpperCase();
            if (!SENTIMENTS.contains(s) || !CATEGORIES.contains(c)) {
                throw new IllegalArgumentException("Model returned unsupported values: " + a);
            }
            return new TicketAnalysis(s, c);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Model response was not valid JSON", e);
        }
    }
}
