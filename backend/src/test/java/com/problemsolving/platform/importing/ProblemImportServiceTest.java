package com.problemsolving.platform.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.problemsolving.platform.domain.entity.Problem;
import com.problemsolving.platform.importing.config.ProblemImportProperties;
import com.problemsolving.platform.importing.domain.ProblemImportState;
import com.problemsolving.platform.importing.domain.ProblemImportStateRepository;
import com.problemsolving.platform.importing.domain.ProblemImportStatus;
import com.problemsolving.platform.importing.hf.HfProblemRow;
import com.problemsolving.platform.importing.hf.HuggingFaceClientException;
import com.problemsolving.platform.importing.hf.HuggingFaceProblemClient;
import com.problemsolving.platform.importing.storage.DatabaseStorageService;
import com.problemsolving.platform.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemImportServiceTest {

    private static final long ID = ProblemImportService.STATE_ID;
    private static final String DATASET = "sigcp/hardtests_problems";

    @Mock
    private ProblemImportStateRepository stateRepository;
    @Mock
    private ProblemRepository problemRepository;
    @Mock
    private HuggingFaceProblemClient huggingFaceClient;
    @Mock
    private DatabaseStorageService storageService;

    private ProblemImportProperties properties;
    private ProblemImportService service;
    private ProblemImportState state;

    @BeforeEach
    void setUp() {
        properties = new ProblemImportProperties();
        properties.setImportCountPerRun(200);
        properties.setBatchSize(100);
        ProblemImportMapper mapper = new ProblemImportMapper(properties, new ObjectMapper());
        service = new ProblemImportService(properties, stateRepository, problemRepository,
                huggingFaceClient, storageService, mapper);

        state = newState(0, 0);
        lenient().when(stateRepository.findById(ID)).thenReturn(Optional.of(state));
        lenient().when(stateRepository.tryAcquire(eq(ID), eq(ProblemImportStatus.RUNNING), any())).thenReturn(1);
        // the real methods are @Modifying DB updates; simulate them on the in-memory state
        lenient().when(stateRepository.updateProgress(eq(ID), anyLong(), anyLong(), any())).thenAnswer(inv -> {
            state.setLastOffset(inv.getArgument(1));
            state.setImportedCount(inv.getArgument(2));
            return 1;
        });
        lenient().when(stateRepository.finish(eq(ID), any(), any(), anyLong(), anyLong(), any())).thenAnswer(inv -> {
            state.setStatus(inv.getArgument(1));
            state.setMessage(inv.getArgument(2));
            return 1;
        });
        lenient().when(problemRepository.findExistingExternalIds(anyList())).thenReturn(List.of());
        lenient().when(problemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(storageService.getCurrentUsageMb()).thenReturn(100.0);
        lenient().when(storageService.isStorageLimitError(any())).thenReturn(false);
    }

    private static ProblemImportState newState(long offset, long importedCount) {
        ProblemImportState s = new ProblemImportState();
        s.setId(ID);
        s.setLastOffset(offset);
        s.setImportedCount(importedCount);
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private static HfProblemRow row(String pid) {
        return new HfProblemRow(pid, "Title " + pid, "Description " + pid, "atcoder",
                List.of(new HfProblemRow.DifficultyRating("easy", 2, "luogu")),
                List.of(new HfProblemRow.PublicTestCase("1 2", "3", "stdin")),
                "2000 s", "1024.0 MB",
                List.of(new HfProblemRow.TagGroup(List.of("Greedy"), "luogu")),
                "https://example.com/" + pid);
    }

    private static List<HfProblemRow> rows(int from, int count) {
        return java.util.stream.IntStream.range(from, from + count)
                .mapToObj(i -> row("pid" + i))
                .toList();
    }

    @Test
    void importsBatchesAndAdvancesOffsetOnlyAfterEachSavedBatch() {
        when(huggingFaceClient.fetchRows(eq(0L), eq(100))).thenReturn(rows(0, 100));
        when(huggingFaceClient.fetchRows(eq(100L), eq(100))).thenReturn(rows(100, 100));

        ProblemImportService.ImportRunResult result = service.runImport("quarterly-schedule");

        assertTrue(result.started());
        verify(problemRepository, org.mockito.Mockito.times(2)).saveAll(anyList());
        verify(stateRepository).updateProgress(eq(ID), eq(100L), eq(100L), any());
        verify(stateRepository).updateProgress(eq(ID), eq(200L), eq(200L), any());
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.COMPLETED),
                contains("Imported 200"), eq(200L), eq(200L), any());
        assertEquals(ProblemImportStatus.COMPLETED, result.state().getStatus());
    }

    @Test
    void stopsAsStorageFullBeforeStartingWhenUsageReachesThreshold() {
        when(storageService.getCurrentUsageMb()).thenReturn(1500.0); // threshold = 2048 * 50% = 1024

        ProblemImportService.ImportRunResult result = service.runImport("quarterly-schedule");

        assertTrue(result.started());
        verify(huggingFaceClient, never()).fetchRows(anyLong(), anyInt());
        verify(problemRepository, never()).saveAll(anyList());
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.STORAGE_FULL),
                contains("1024.0"), eq(0L), eq(0L), any());
    }

    @Test
    void stopsAsStorageFullBetweenBatchesAndKeepsCommittedOffset() {
        // check before start, check before batch 1, check before batch 2 -> full
        when(storageService.getCurrentUsageMb()).thenReturn(100.0, 100.0, 2000.0);
        when(huggingFaceClient.fetchRows(eq(0L), eq(100))).thenReturn(rows(0, 100));
        when(huggingFaceClient.fetchRows(eq(100L), eq(100))).thenReturn(rows(100, 100));

        ProblemImportService.ImportRunResult result = service.runImport("quarterly-schedule");

        assertTrue(result.started());
        verify(problemRepository).saveAll(anyList()); // only the first batch was saved
        verify(stateRepository).updateProgress(eq(ID), eq(100L), eq(100L), any()); // offset of the saved batch persisted
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.STORAGE_FULL),
                contains("offset 100"), eq(100L), eq(100L), any());
    }

    @Test
    void marksStorageFullWhenInsertHitsTheDatabaseStorageLimit() {
        when(huggingFaceClient.fetchRows(eq(0L), eq(100))).thenReturn(rows(0, 100));
        when(problemRepository.saveAll(anyList()))
                .thenThrow(new DataAccessResourceFailureException("could not extend file: No space left on device"));
        when(storageService.isStorageLimitError(any())).thenReturn(true);

        ProblemImportService.ImportRunResult result = service.runImport("quarterly-schedule");

        assertTrue(result.started());
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.STORAGE_FULL),
                contains("storage limit"), eq(0L), eq(0L), any());
        // progress was not advanced because the batch did not commit
        verify(stateRepository, never()).updateProgress(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void marksFailedOnOtherDatabaseErrorsAndKeepsOffset() {
        when(huggingFaceClient.fetchRows(eq(0L), eq(100))).thenReturn(rows(0, 100));
        when(problemRepository.saveAll(anyList()))
                .thenThrow(new DuplicateKeyException("duplicate key value violates unique constraint"));

        ProblemImportService.ImportRunResult result = service.runImport("manual-admin-trigger");

        assertTrue(result.started());
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.FAILED),
                contains("duplicate key value"), eq(0L), eq(0L), any());
    }

    @Test
    void marksFailedWhenHuggingFaceApiFails() {
        when(huggingFaceClient.fetchRows(eq(0L), eq(100)))
                .thenThrow(new HuggingFaceClientException("Hugging Face rows API request failed: 502"));

        ProblemImportService.ImportRunResult result = service.runImport("quarterly-schedule");

        assertTrue(result.started());
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.FAILED),
                contains("Hugging Face rows API request failed"), eq(0L), eq(0L), any());
        verify(problemRepository, never()).saveAll(anyList());
    }

    @Test
    void ignoresTriggerWhenAnotherImportIsRunning() {
        when(stateRepository.tryAcquire(eq(ID), eq(ProblemImportStatus.RUNNING), any())).thenReturn(0);

        ProblemImportService.ImportRunResult result = service.runImport("manual-admin-trigger");

        assertFalse(result.started());
        verify(huggingFaceClient, never()).fetchRows(anyLong(), anyInt());
        verify(stateRepository, never()).finish(any(), any(), any(), anyLong(), anyLong(), any());
    }

    @Test
    void ignoresTriggerWhenImportSystemIsDisabled() {
        properties.setEnabled(false);

        ProblemImportService.ImportRunResult result = service.runImport("quarterly-schedule");

        assertFalse(result.started());
        verify(stateRepository, never()).tryAcquire(any(), any(), any());
        verify(huggingFaceClient, never()).fetchRows(anyLong(), anyInt());
    }

    @Test
    void completesWithoutAdvancingOffsetWhenDatasetIsExhausted() {
        when(huggingFaceClient.fetchRows(eq(0L), eq(100))).thenReturn(List.of());

        ProblemImportService.ImportRunResult result = service.runImport("quarterly-schedule");

        assertTrue(result.started());
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.COMPLETED),
                contains("Dataset exhausted"), eq(0L), eq(0L), any());
        verify(stateRepository, never()).updateProgress(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void skipsProblemsThatWereAlreadyImported() {
        when(huggingFaceClient.fetchRows(eq(0L), eq(100))).thenReturn(rows(0, 100));
        properties.setImportCountPerRun(100);
        // the first dataset row is already stored -> only 99 new problems in the batch
        when(problemRepository.findExistingExternalIds(anyList()))
                .thenReturn(List.of("hf:" + DATASET + ":pid0"));

        ProblemImportService.ImportRunResult result = service.runImport("quarterly-schedule");

        assertTrue(result.started());
        verify(problemRepository).saveAll(org.mockito.ArgumentMatchers
                .<Iterable<Problem>>argThat(batch -> batch != null && ((java.util.Collection<Problem>) batch).size() == 99));
        verify(stateRepository).updateProgress(eq(ID), eq(100L), eq(99L), any());
        // fetch count was advanced by the consumed rows even though one was a duplicate
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.COMPLETED),
                contains("Dataset exhausted"), eq(100L), eq(99L), any());
    }

    @Test
    void resumesFromPersistedOffsetOnNextRun() {
        state = newState(300, 300);
        when(stateRepository.findById(ID)).thenReturn(Optional.of(state));
        when(huggingFaceClient.fetchRows(eq(300L), eq(100))).thenReturn(rows(300, 100));
        when(huggingFaceClient.fetchRows(eq(400L), eq(100))).thenReturn(rows(400, 100));

        service.runImport("quarterly-schedule");

        verify(huggingFaceClient).fetchRows(eq(300L), eq(100));
        verify(stateRepository).updateProgress(eq(ID), eq(400L), eq(400L), any());
        verify(stateRepository).updateProgress(eq(ID), eq(500L), eq(500L), any());
        verify(stateRepository).finish(eq(ID), eq(ProblemImportStatus.COMPLETED),
                contains("Imported 200"), eq(500L), eq(500L), any());
    }

    @Test
    void initialImportPendingReflectsStateAndOffset() {
        assertTrue(service.initialImportPending());

        state.setStatus(ProblemImportStatus.COMPLETED);
        assertFalse(service.initialImportPending());

        state.setStatus(ProblemImportStatus.FAILED);
        state.setLastOffset(400);
        assertTrue(service.initialImportPending());

        state.setLastOffset(500);
        assertFalse(service.initialImportPending());

        state.setStatus(ProblemImportStatus.RUNNING);
        assertFalse(service.initialImportPending());
    }
}
