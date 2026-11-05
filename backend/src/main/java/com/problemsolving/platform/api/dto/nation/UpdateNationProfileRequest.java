package com.problemsolving.platform.api.dto.nation;

/** Fields are optional; only the ones that are present get changed. */
public class UpdateNationProfileRequest {

    private String name;
    private String timeZone;
    private Boolean appearOffline;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Boolean getAppearOffline() {
        return appearOffline;
    }

    public void setAppearOffline(Boolean appearOffline) {
        this.appearOffline = appearOffline;
    }
}
