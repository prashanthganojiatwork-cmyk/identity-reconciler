package com.prashanthganojiatwork.reconciler.blocking;

import com.prashanthganojiatwork.reconciler.model.BlockingResult;
import com.prashanthganojiatwork.reconciler.model.BlockingStatistics;
import com.prashanthganojiatwork.reconciler.model.CandidatePair;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory blocking engine that uses an inverted index approach to reduce
 * the comparison space from O(n*m) to a fraction of exhaustive pairwise comparisons.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Generate blocking keys for all records using registered BlockingKeyGenerator instances</li>
 *   <li>Build an inverted index for sourceB (blocking key to list of records)</li>
 *   <li>For each sourceA record, look up matching sourceB records via shared blocking keys</li>
 *   <li>Deduplicate candidate pairs (merge shared keys for same record pair)</li>
 *   <li>Apply block size cap (skip blocks with more than 100 sourceB records)</li>
 *   <li>Apply fallback block for records with no blocking keys</li>
 * </ol>
 */
@Service
public class InMemoryBlockingEngine implements BlockingEngine {

    private static final Logger log = LoggerFactory.getLogger(InMemoryBlockingEngine.class);
    private static final int MAX_BLOCK_SIZE = 100;

    private final List<BlockingKeyGenerator> keyGenerators;

    public InMemoryBlockingEngine(List<BlockingKeyGenerator> keyGenerators) {
        this.keyGenerators = keyGenerators;
    }

    @Override
    public BlockingResult generateCandidatePairs(List<NormalizedRecord> sourceA, List<NormalizedRecord> sourceB) {
        // Step 1: Build inverted index for sourceB
        Map<String, List<NormalizedRecord>> invertedIndex = buildInvertedIndex(sourceB);

        // Track blocks that are too large (>100 records from sourceB) - skip these
        Set<String> oversizedBlocks = new HashSet<>();
        for (Map.Entry<String, List<NormalizedRecord>> entry : invertedIndex.entrySet()) {
            if (entry.getValue().size() > MAX_BLOCK_SIZE) {
                oversizedBlocks.add(entry.getKey());
                log.warn("Skipping oversized block '{}' with {} sourceB records (cap: {})",
                        entry.getKey(), entry.getValue().size(), MAX_BLOCK_SIZE);
            }
        }

        // Step 2: For each sourceA record, find candidate pairs via blocking keys
        // Use a map keyed by (recordA.id, recordB.id) to deduplicate and merge shared keys
        Map<String, CandidatePairBuilder> candidateMap = new LinkedHashMap<>();

        for (NormalizedRecord recordA : sourceA) {
            Set<String> keysA = generateAllKeys(recordA);

            if (keysA.isEmpty()) {
                // Fallback block: compare against ALL sourceB records
                log.warn("Record '{}' has no blocking keys - using fallback block (comparing against all {} sourceB records)",
                        recordA.id(), sourceB.size());
                for (NormalizedRecord recordB : sourceB) {
                    String pairKey = recordA.id() + "|" + recordB.id();
                    candidateMap.computeIfAbsent(pairKey, k -> new CandidatePairBuilder(recordA, recordB))
                            .addSharedKey("FALLBACK");
                }
                continue;
            }

            for (String key : keysA) {
                // Skip oversized blocks
                if (oversizedBlocks.contains(key)) {
                    continue;
                }

                List<NormalizedRecord> matchingRecordsB = invertedIndex.get(key);
                if (matchingRecordsB == null || matchingRecordsB.isEmpty()) {
                    continue;
                }

                for (NormalizedRecord recordB : matchingRecordsB) {
                    String pairKey = recordA.id() + "|" + recordB.id();
                    candidateMap.computeIfAbsent(pairKey, k -> new CandidatePairBuilder(recordA, recordB))
                            .addSharedKey(key);
                }
            }
        }

        // Step 3: Build final candidate pairs
        List<CandidatePair> candidatePairs = candidateMap.values().stream()
                .map(CandidatePairBuilder::build)
                .collect(Collectors.toList());

        // Step 4: Compute blocking statistics
        // totalBlocks = distinct blocking keys that had at least one record from each source
        Set<String> sourceAKeys = new HashSet<>();
        for (NormalizedRecord recordA : sourceA) {
            sourceAKeys.addAll(generateAllKeys(recordA));
        }

        int totalBlocksWithBothSources = 0;
        int maxBlockSize = 0;
        long totalBlockSizeSum = 0;

        for (Map.Entry<String, List<NormalizedRecord>> entry : invertedIndex.entrySet()) {
            String key = entry.getKey();
            if (!oversizedBlocks.contains(key) && sourceAKeys.contains(key) && !entry.getValue().isEmpty()) {
                totalBlocksWithBothSources++;
                int blockSize = entry.getValue().size();
                totalBlockSizeSum += blockSize;
                if (blockSize > maxBlockSize) {
                    maxBlockSize = blockSize;
                }
            }
        }

        double averageBlockSize = totalBlocksWithBothSources > 0
                ? (double) totalBlockSizeSum / totalBlocksWithBothSources
                : 0.0;

        long comparisonsPerformed = candidatePairs.size();
        long exhaustiveCount = (long) sourceA.size() * (long) sourceB.size();
        double reductionPercentage = exhaustiveCount > 0
                ? (1.0 - (double) comparisonsPerformed / exhaustiveCount) * 100.0
                : 0.0;

        BlockingStatistics statistics = new BlockingStatistics(
                totalBlocksWithBothSources,
                averageBlockSize,
                maxBlockSize,
                comparisonsPerformed,
                exhaustiveCount,
                reductionPercentage
        );

        log.info("Blocking statistics: totalBlocks={}, avgBlockSize={}, maxBlockSize={}, comparisons={}, exhaustive={}, reduction={}%",
                statistics.totalBlocks(),
                String.format("%.2f", statistics.averageBlockSize()),
                statistics.maxBlockSize(),
                statistics.comparisonsPerformed(),
                statistics.exhaustiveCount(),
                String.format("%.2f", statistics.reductionPercentage()));

        return new BlockingResult(candidatePairs, statistics);
    }

    /**
     * Build an inverted index from blocking keys to sourceB records.
     */
    private Map<String, List<NormalizedRecord>> buildInvertedIndex(List<NormalizedRecord> sourceB) {
        Map<String, List<NormalizedRecord>> index = new HashMap<>();
        for (NormalizedRecord record : sourceB) {
            Set<String> keys = generateAllKeys(record);
            for (String key : keys) {
                index.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }
        }
        return index;
    }

    /**
     * Generate all blocking keys for a record using all registered key generators.
     */
    private Set<String> generateAllKeys(NormalizedRecord record) {
        Set<String> allKeys = new HashSet<>();
        for (BlockingKeyGenerator generator : keyGenerators) {
            Set<String> keys = generator.generateKeys(record);
            if (keys != null) {
                allKeys.addAll(keys);
            }
        }
        return allKeys;
    }

    /**
     * Helper class to build CandidatePair instances while accumulating shared blocking keys.
     */
    private static class CandidatePairBuilder {
        private final NormalizedRecord recordA;
        private final NormalizedRecord recordB;
        private final Set<String> sharedKeys = new LinkedHashSet<>();

        CandidatePairBuilder(NormalizedRecord recordA, NormalizedRecord recordB) {
            this.recordA = recordA;
            this.recordB = recordB;
        }

        void addSharedKey(String key) {
            sharedKeys.add(key);
        }

        CandidatePair build() {
            return new CandidatePair(recordA, recordB, Collections.unmodifiableSet(sharedKeys));
        }
    }
}
