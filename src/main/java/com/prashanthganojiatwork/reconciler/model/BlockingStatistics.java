package com.prashanthganojiatwork.reconciler.model;

/**
 * Performance metrics for a blocking operation.
 *
 * @param totalBlocks           number of blocks formed
 * @param averageBlockSize      average records per block
 * @param maxBlockSize          largest block
 * @param comparisonsPerformed  actual comparisons made
 * @param exhaustiveCount       n × m (what exhaustive would have been)
 * @param reductionPercentage   1 - (performed / exhaustive), as percentage
 */
public record BlockingStatistics(
    int totalBlocks,
    double averageBlockSize,
    int maxBlockSize,
    long comparisonsPerformed,
    long exhaustiveCount,
    double reductionPercentage
) {}
