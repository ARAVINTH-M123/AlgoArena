package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationTrackState;
import com.problemsolving.platform.domain.enums.NationTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NationTrackStateRepository extends JpaRepository<NationTrackState, Long> {
    Optional<NationTrackState> findByNationIdAndTrack(Long nationId, NationTrack track);
    List<NationTrackState> findByNationId(Long nationId);
}
