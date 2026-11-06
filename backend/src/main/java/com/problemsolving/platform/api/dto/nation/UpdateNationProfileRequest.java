package com.problemsolving.platform.api.dto.nation;

import com.problemsolving.platform.api.dto.cosmetics.FlagRequest;

/** Fields are optional; only the ones that are present get changed. */
public class UpdateNationProfileRequest {

    private String name;
    private String timeZone;
    private Boolean appearOffline;
    private FlagRequest flag;

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

    /** Optional flag change (pattern, 2-3 colors, optional emblem). Validated before anything is applied. */
    public FlagRequest getFlag() {
        return flag;
    }

    public void setFlag(FlagRequest flag) {
        this.flag = flag;
    }
}
