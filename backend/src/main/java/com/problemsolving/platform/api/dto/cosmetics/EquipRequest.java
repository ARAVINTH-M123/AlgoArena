package com.problemsolving.platform.api.dto.cosmetics;

/** key may be null, empty or "auto" to go back to the automatic cosmetic of the current rank. */
public class EquipRequest {

    private String category;
    private String key;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
