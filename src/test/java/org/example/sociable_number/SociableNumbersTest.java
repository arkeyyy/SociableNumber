package org.example.sociable_number;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SociableNumbersTest {
    @Test
    void sumsProperDivisorsWithoutOverflow() {
        assertEquals(0, SociableNumbers.properDivisorSum(1));
        assertEquals(1, SociableNumbers.properDivisorSum(Integer.MAX_VALUE));
        assertEquals(15, SociableNumbers.properDivisorSum(16));
        assertEquals(55, SociableNumbers.properDivisorSum(36));
        assertEquals(2_997_558_082L, SociableNumbers.properDivisorSum(2_000_000_000));
    }

    @Test
    void classifiesEveryStartInTheSample() {
        var matches = IntStream.rangeClosed(1, 16000)
                .filter(start -> SociableNumbers.isSociable(start, 16000))
                .boxed()
                .toList();
        assertEquals(List.of(12496, 14264, 14288, 14536, 15472), matches);
        assertEquals(15995, 16000 - matches.size());
    }

    @Test
    void rejectsShortCyclesTailsAndCyclesOutsideTheLimit() {
        assertFalse(SociableNumbers.isSociable(6, 16000));
        assertFalse(SociableNumbers.isSociable(220, 16000));
        assertEquals(12496, SociableNumbers.properDivisorSum(9464));
        assertFalse(SociableNumbers.isSociable(9464, 16000));
        assertFalse(SociableNumbers.isSociable(12496, 15471));
        assertTrue(SociableNumbers.isSociable(12496, 15472));
    }

    @Test
    void usesTheWeightedSampleRanges() {
        assertEquals(List.of(
                new SociableNumbers.Range(1, 1600),
                new SociableNumbers.Range(1601, 4800),
                new SociableNumbers.Range(4801, 9600),
                new SociableNumbers.Range(9601, 16000)), SociableNumbers.ranges(16000, 4));
    }

    @Test
    void partitionsTheEntireIntervalIntoNonemptyRanges() {
        for (int num : List.of(1, 7, 64, 65, 16000, Integer.MAX_VALUE)) {
            for (int cnt = 1; cnt <= Math.min(num, 64); cnt++) {
                List<SociableNumbers.Range> ranges = SociableNumbers.ranges(num, cnt);
                assertEquals(cnt, ranges.size());
                long next = 1;
                for (var range : ranges) {
                    assertEquals(next, range.first());
                    assertTrue(range.size() > 0);
                    next += range.size();
                }
                assertEquals(num + 1L, next);
                if (cnt > 1 && num > cnt) {
                    assertTrue(ranges.stream().map(SociableNumbers.Range::size).distinct().count() > 1);
                }
            }
        }
    }

    @Test
    void interruptedWorkerCancelsClassification() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> SociableNumbers.isSociable(12496, 16000));
        } finally {
            Thread.interrupted();
        }
    }
}
