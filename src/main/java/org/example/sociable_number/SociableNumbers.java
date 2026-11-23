package org.example.sociable_number;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

final class SociableNumbers {
    private SociableNumbers() {
    }

    record Range(int first, int last) {
        int size() {
            return last - first + 1;
        }
    }

    static List<Range> ranges(int num, int cnt) {
        if (cnt < 1 || cnt > Math.min(num, 64)) {
            throw new IllegalArgumentException("Thread count is outside the allowed range.");
        }

        List<Range> ranges = new ArrayList<>(cnt);
        int previousEnd = 0;
        long totalWeight = (long) cnt * (cnt + 1) / 2;
        for (int worker = 1; worker <= cnt; worker++) {
            long cumulativeWeight = (long) worker * (worker + 1) / 2;
            long weightedEnd = num * cumulativeWeight / totalWeight;
            int latestEnd = num - (cnt - worker);
            int end = (int) Math.clamp(weightedEnd, previousEnd + 1L, latestEnd);
            ranges.add(new Range(previousEnd + 1, end));
            previousEnd = end;
        }
        return ranges;
    }

    static boolean isSociable(int start, int limit) {
        Set<Integer> visited = new HashSet<>();
        long value = start;
        while (value > 0 && value <= limit) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }
            if (!visited.add((int) value)) {
                return value == start && visited.size() >= 3;
            }
            value = properDivisorSum((int) value);
        }
        return false;
    }

    static long properDivisorSum(int number) {
        int remaining = number;
        long sum = 1;
        for (int factor = 2; (long) factor * factor <= remaining; factor++) {
            if (remaining % factor != 0) {
                continue;
            }
            long power = 1;
            long factorSum = 1;
            do {
                remaining /= factor;
                power *= factor;
                factorSum += power;
            } while (remaining % factor == 0);
            sum *= factorSum;
        }
        if (remaining > 1) {
            sum *= 1L + remaining;
        }
        return sum - number;
    }
}
