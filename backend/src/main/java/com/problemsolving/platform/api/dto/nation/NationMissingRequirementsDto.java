package com.problemsolving.platform.api.dto.nation;

public record NationMissingRequirementsDto(
        Long xp,
        Integer mediumSolves,
        Integer hardSolves
) {
}
