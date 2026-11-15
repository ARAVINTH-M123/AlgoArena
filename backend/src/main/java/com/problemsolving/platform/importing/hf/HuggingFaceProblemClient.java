package com.problemsolving.platform.importing.hf;

import java.util.List;

/**
 * Read-only access to the problem rows of a Hugging Face dataset. The importer never
 * downloads the dataset locally - rows are always retrieved through the dataset API.
 */
public interface HuggingFaceProblemClient {

    /**
     * Returns up to {@code limit} consecutive dataset rows starting at {@code offset}.
     * Implementations page internally (the datasets-server /rows API caps a single
     * request at 100 rows), so any limit is safe.
     *
     * @throws HuggingFaceClientException on any API/transport/parsing failure
     */
    List<HfProblemRow> fetchRows(long offset, int limit);
}
