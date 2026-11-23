# Sociable Number

Sociable Number is a JavaFX desktop application that finds all sociable numbers between 1 and an inclusive upper limit. The default search covers 10,000 numbers with four worker threads.

## What counts as sociable?

Each step replaces a number with the sum of its positive proper divisors. For example, 6 has proper divisors 1, 2, and 3, so its next value is 6. The sum for 1 is zero.

A starting number qualifies when the sequence first returns to it after at least three steps and every value stays within the search interval. Perfect numbers, amicable pairs, and numbers that lead into another cycle all count toward **Other numbers**.

## Worker progress

Each worker checks a separate, contiguous range. Later workers receive larger ranges when possible. The progress pane shows each worker’s range, number count, progress, and state. A completed search displays its matches in ascending order. Stopping a search discards partial results.

The upper limit accepts positive 32-bit integers. The thread count ranges from 1 through the smaller of the upper limit and 64.

## Example

For an upper limit of 16,000 and four workers, the five matches belong to one cycle.

```text
12496 → 14288 → 15472 → 14536 → 14264 → 12496
```

The results area displays `12496, 14264, 14288, 14536, 15472`. The summary counts 5 sociable numbers and 15,995 other numbers, for a total of 16,000 checked.
