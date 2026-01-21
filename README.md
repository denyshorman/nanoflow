# Nanoflow

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.denyshorman/nanoflow)

Java 21+ Flow library with concurrent emission support.

Nanoflow embraces simplicity through an **imperative model**:

```java
var flow = Flows.range(1, 4);

try (var items = flow.open()) {
    for (var item : items) {
        System.out.println(item);
    }
}
```

### Why Nanoflow?

*   **Imperative & Intuitive**: Consume flows using familiar Java constructs such as `try-with-resources` and `for (var item : items)`.
*   **Virtual Thread Native**: Built for Java 21+ virtual threads. Blocking operations are natural and efficient, with no complex non-blocking state machines required.
*   **Concurrent Emission**: Multiple threads can emit concurrently into the same flow; consumers see a single sequence in arrival order.
*   **Natural Backpressure**: With bounded buffers, producers block when consumers are slow, providing flow control without manual configuration.
*   **Exception Transparency**: Checked exceptions propagate naturally from producers to consumers, just like regular Java code.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [The Core Model](#the-core-model)
- [Creating Flows](#creating-flows)
- [Concurrent Emission](#concurrent-emission)
- [Error Handling](#error-handling)
- [Cancellation](#cancellation)
- [Operators](#operators)
  - [Intermediate Operators](#intermediate-operators)
  - [Terminal Operators](#terminal-operators)
- [Stream Interop](#stream-interop)
- [Backpressure & Implementation](#backpressure--implementation)
- [License](#license)

## Installation

Add Nanoflow as a dependency:

**Maven:**
```xml
<dependency>
    <groupId>io.github.denyshorman</groupId>
    <artifactId>nanoflow</artifactId>
    <version>VERSION</version>
</dependency>
```

**Gradle:**
```kotlin
implementation("io.github.denyshorman:nanoflow:VERSION")
```

## Quick Start

Most flows can be consumed with a simple terminal operator like `collect` or `toList`:

```java
var flow = Flows.of("Hello", "World");

flow.collect(System.out::println);
```

## The Core Model

Nanoflow's defining feature is its **open() + for-each** consumption model. Instead of callbacks or reactive chains, flows are consumed like regular Java collections:

```java
var flow = Flows.of(1, 2, 3, 4);

try (var items = flow.open()) {
    for (var item : items) {
        if (item > 3) break;
        System.out.println(item);
    }
}
```

### What Happens Here?

1. **`flow.open()`** returns a `Sequence` (which is both `Iterable` and `AutoCloseable`)
2. **`for (var item : items)`** consumes values from the sequence
3. **`break`** stops iteration and exits the `try-with-resources` block
4. **`try-with-resources`** closes the sequence and interrupts the producer; cleanup happens asynchronously in the producer

### Why This Model?

*   **Natural Backpressure**: With bounded buffers, the producer blocks when the consumer isn't ready. If the `for` loop is slow, the producer waits, with no configuration needed.
*   **Resource Safety**: `try-with-resources` signals cancellation by closing the sequence and interrupting the producer.
*   **Simplicity**: Asynchronous data streams look and feel like synchronous collections. No learning curve for basic iteration.
*   **Exception Handling**: Checked exceptions propagate naturally, just like regular Java code.

## Creating Flows

Nanoflow provides multiple ways to create flows, from simple static values to complex custom producers.

### From Collections and Streams

```java
// From existing collections
var fromList = Flows.from(List.of(1, 2, 3));
var fromSet = Flows.from(Set.of("a", "b", "c"));

// From streams (consumed once)
var fromStream = Flows.from(Stream.of(1, 2, 3));

// From varargs
var fromValues = Flows.of("Hello", "World");
```

### Ranges and Repetition

```java
var range = Flows.range(0, 5);           // 0, 1, 2, 3, 4 (exclusive end)
var rangeClosed = Flows.rangeClosed(1, 3); // 1, 2, 3 (inclusive end)
var repeated = Flows.repeat(3, "a");     // "a", "a", "a" (finite)
var infinite = Flows.repeat("b");        // "b", "b", "b", ... (infinite)
```

### Generators

```java
// Generate values from a supplier
var random = Flows.generate(() -> ThreadLocalRandom.current().nextInt(100));

// Generate a specific count
var tenRandom = Flows.generate(10, () -> Math.random());

// Iterate with a function
var powers = Flows.iterate(1, n -> n * 2); // 1, 2, 4, 8, 16, ...

// Iterate with a condition
var limited = Flows.iterate(1, n -> n < 100, n -> n * 2); // 1, 2, 4, 8, 16, 32, 64
```

### Timing

```java
var ticks = Flows.interval(Duration.ofSeconds(1)); // 0, 1, 2, ...
var single = Flows.timer(Duration.ofSeconds(1));   // 0 after delay
```

### Special Flows

```java
var empty = Flows.<String>empty();                    // Completes immediately
var never = Flows.<String>never();                    // Never completes
var error = Flows.<String>error(new IOException());   // Fails immediately
var deferred = Flows.defer(() -> Flows.of("lazy"));   // Created on each subscription
```

## Concurrent Emission

Nanoflow supports **thread-safe concurrent emission**. Multiple threads can call `emit()` on the same emitter, and the consumer sees a single sequence of values in arrival order (no cross-thread ordering guarantees).

This is useful for aggregating results from concurrent tasks:

```java
var flow = Flows.<Integer>flow(emitter -> {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        // Launch 1000 concurrent tasks
        for (var i = 0; i < 1000; i++) {
            final var value = i;
            executor.submit(() -> {
                try {
                    // Multiple threads calling emit() concurrently is safe.
                    emitter.emit(value);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    } // Executor waits for all tasks to complete
});

// Consumer sees all 1000 values in a single sequence
flow.collect(System.out::println);
```

### Use Cases

*   **Concurrent Processing**: Fan out work to multiple threads, collect results as they arrive
*   **Event Aggregation**: Multiple event sources emitting into a single flow
*   **Concurrent I/O**: Multiple network requests or file reads emitting results as they complete

## Error Handling

Nanoflow embraces Java's exception model; **checked exceptions propagate naturally** from producers to consumers, just like regular method calls.

### Producer Exceptions

Exceptions thrown in the producer action propagate to the consumer:

```java
var flow = Flows.<String>flow(emitter -> {
    // Checked exception - no need to catch it here
    var data = Files.readString(Path.of("data.txt"));
    emitter.emit(data);
});

try {
    // Exception propagates to the consumer
    flow.<IOException>collect(System.out::println);
} catch (IOException e) {
    System.err.println("Failed to read file: " + e.getMessage());
}
```

### Consumer Exceptions

Exceptions thrown in the consumer (e.g., in a `Collector`) propagate to the caller of the terminal operator. The producer thread is interrupted and should handle interruption normally:

```java
var flow = Flows.of(1, 2, 0, 4);

try {
    flow.collect(value -> {
        System.out.println(10 / value); // Throws on zero
    });
} catch (ArithmeticException e) {
    System.err.println("Division by zero!");
}
```

If the consumer throws, any blocked or active producer will observe interruption via `emit()`, `Thread.currentThread().isInterrupted()`, or any blocking call that reacts to interruption.

## Cancellation

Nanoflow provides cooperative cancellation through **thread interruption**. When a consumer closes the flow or is interrupted, the producer thread is automatically interrupted.

### How It Works

1. Consumer closes the flow (explicitly or via `try-with-resources`)
2. Producer thread is interrupted
3. Blocking operations that honor interruption may throw `InterruptedException` or return early
4. Producer should handle the exception and clean up

### Example

```java
var flow = Flows.<Integer>flow(emitter -> {
    try {
        var i = 0;
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(100); // Simulate work
            emitter.emit(i++);
        }
    } catch (InterruptedException e) {
        System.out.println("Producer canceled - cleaning up");
        // Perform cleanup here
    }
});

try (var items = flow.open()) {
    for (var item : items) {
        // Process a few items, then stop
        if (item > 5) break;
    }
} // Flow is closed here, producer is interrupted
```

### Best Practices

*   **Check interruption status** in CPU-bound loops: `Thread.currentThread().isInterrupted()`
*   **Handle `InterruptedException`** in blocking operations
*   **Clean up resources** before exiting the producer action
*   **Cross-thread cancellation:** interrupt the thread performing iteration

## Operators

Nanoflow provides a comprehensive set of operators for transforming, filtering, and aggregating flows.

### Intermediate Operators

Intermediate operators are **lazy**; they return a new `Flow` without executing anything. Execution happens only when a terminal operator is called.

#### Transforming

```java
// map: Transform each value
Flows.of(1, 2, 3)
    .map(n -> n * 2)
    .toList(); // [2, 4, 6]

// flatMap: Transform each value to a flow and flatten sequentially
Flows.of(1, 2, 3)
    .flatMap(n -> Flows.of(n, n * 10))
    .toList(); // [1, 10, 2, 20, 3, 30]

// scan: Emit running accumulations
Flows.of(1, 2, 3)
    .scan(0, Integer::sum)
    .toList(); // [0, 1, 3, 6]
```

#### Filtering

```java
// filter: Keep values matching predicate
Flows.range(1, 10)
    .filter(n -> n % 2 == 0)
    .toList(); // [2, 4, 6, 8]

// filterNot: Keep values not matching predicate
Flows.range(1, 6)
    .filterNot(n -> n % 2 == 0)
    .toList(); // [1, 3, 5]

// distinct: Remove duplicates
Flows.of(1, 2, 2, 3, 1)
    .distinct()
    .toList(); // [1, 2, 3]

// distinctUntilChanged: Remove consecutive duplicates
Flows.of(1, 1, 2, 2, 1)
    .distinctUntilChanged()
    .toList(); // [1, 2, 1]
```

#### Slicing

```java
// take: Take first N elements
Flows.range(1, 100).take(3).toList(); // [1, 2, 3]

// takeWhile: Take while predicate is true
Flows.range(1, 100).takeWhile(n -> n < 5).toList(); // [1, 2, 3, 4]

// drop: Skip first N elements
Flows.range(1, 6).drop(2).toList(); // [3, 4, 5]

// dropWhile: Skip while predicate is true
Flows.range(1, 6).dropWhile(n -> n < 3).toList(); // [3, 4, 5]
```

#### Combining

```java
// concat: Concatenate flows
Flows.of(1, 2).concat(Flows.of(3, 4)).toList(); // [1, 2, 3, 4]

// zip: Combine two flows element-wise
Flows.of("a", "b", "c")
    .zip(Flows.of(1, 2, 3), (s, n) -> s + n)
    .toList(); // ["a1", "b2", "c3"]
```

#### Buffering

```java
// buffer: Add a buffer between producer and consumer
Flows.range(1, 5).buffer(10).toList(); // [1, 2, 3, 4]

// chunked: Group elements into lists
Flows.range(1, 8).chunked(3).toList(); // [[1, 2, 3], [4, 5, 6], [7]]
```

#### Timing

```java
// delay: Delay the start of collection
Flows.of(1, 2, 3).delay(Duration.ofMillis(10)).toList();

// delayEach: Delay each element
Flows.of(1, 2, 3).delayEach(Duration.ofMillis(10)).toList();

// timeout: Fail if the next element does not arrive in time
Flows.<Integer>never().timeout(Duration.ofSeconds(1)).toList(); // throws TimeoutException
```

### Terminal Operators

Terminal operators **execute the flow** and return a result or perform side effects.

#### Collection

```java
var list = Flows.of(1, 2, 3).toList();           // [1, 2, 3]
var set = Flows.of(1, 2, 2, 3).toSet();          // [1, 2, 3]
var queue = Flows.of(1, 2).toCollection(LinkedList::new);

// collect: Custom consumer with exception handling
Flows.of("a", "b").collect(System.out::println);
```

#### Element Access

```java
var first = Flows.of(1, 2, 3).first();           // 1
var firstOrNull = Flows.<Integer>empty().firstOrNull(); // null
var firstOrDefault = Flows.<Integer>empty().firstOrDefault(99); // 99

var last = Flows.of(1, 2, 3).last();             // 3
var single = Flows.of(42).single();              // 42 (throws if not exactly one)
```

#### Aggregation

```java
var sum = Flows.of(1, 2, 3).reduce(Integer::sum);     // 6
var product = Flows.of(1, 2, 3).fold(1, (a, b) -> a * b); // 6
var count = Flows.of("a", "b", "c").count();           // 3
```

#### Predicates

```java
boolean hasEven = Flows.of(1, 2, 3).any(n -> n % 2 == 0);    // true
boolean allEven = Flows.of(2, 4, 6).all(n -> n % 2 == 0);    // true
boolean noneNegative = Flows.of(1, 2, 3).none(n -> n < 0);   // true
```

## Stream Interop

Stream interop is available for converting a flow to a standard `Stream`:

```java
try (var stream = Flows.range(1, 4).stream()) {
    var result = stream
        .map(n -> n * 2)
        .filter(n -> n > 2)
        .collect(Collectors.toList());
    // result: [4, 6]
}
```

**Note**: The returned stream must be closed (use `try-with-resources`) to ensure the underlying flow is properly cleaned up.

## Backpressure & Implementation

Nanoflow is designed for the **modern Java era** with virtual threads and simplicity in mind.

### Virtual Threads

The concurrency model embraces blocking operations:

*   **Producers can block** on `emit()` when the buffer is full; this is natural and efficient with virtual threads
*   **Consumers can block** in the `for` loop, with no need for complex reactive operators
*   **No callback hell**; write straightforward imperative code

### Backpressure

Backpressure is **automatic and built-in** when the buffer is bounded:

*   When the consumer is slow, the producer blocks on `emit()` when the buffer fills up
*   When the consumer is fast, the producer runs at full speed
*   No manual configuration; it just works

### Buffer Control

```java
// Synchronous handoff (buffer size 0) - tightest backpressure
var sync = Flows.flow(0, emitter -> { /* ... */ });

// Small buffer - balance between throughput and memory
var buffered = Flows.flow(100, emitter -> { /* ... */ });

// Unbounded buffer - maximum throughput, higher memory usage
var unbounded = Flows.flow(Integer.MAX_VALUE, emitter -> { /* ... */ });
```

### Lazy Operators

Intermediate operators are lazy and only do work when a terminal operator consumes the flow.

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE.md) for details.
