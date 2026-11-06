package com.problemsolving.platform.api.dto.cosmetics;

import java.util.List;

/** Request body for the flag part of PUT /api/nation/me/profile. Everything is validated before anything changes. */
public class FlagRequest {

    private String pattern;
    private List<String> colors;
    /** Optional: null or empty means "no emblem". */
    private String emblem;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    public String getEmblem() {
        return emblem;
    }

    public void setEmblem(String emblem) {
        this.emblem = emblem;
    }
}
