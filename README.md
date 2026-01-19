# Nanoflow

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.denyshorman/nanoflow)


Nanoflow is a minimal library for Java 21+ providing cold streams with support for concurrent value emission. It focuses on a small, focused API for value production and collection.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
  - [Sequential Flow](#sequential-flow)
  - [Concurrent Flow](#concurrent-flow)
  - [Exception Handling](#exception-handling)
  - [Cancellation](#cancellation)
- [Technical Details](#technical-details)
  - [Thread Safety](#thread-safety)
  - [Exception Propagation](#exception-propagation)
  - [Performance](#performance)
- [License](#license)

## Overview

Nanoflow provides a lightweight mechanism for producing values asynchronously or sequentially. It is designed for scenarios where a full-featured reactive library is unnecessary.

The library is intentionally minimal, providing only the core primitives for stream production and collection: `flow`, `concurrentFlow`, and `collect`.

## Features

- **Concurrent Emission**: Multiple threads can emit values into a single flow simultaneously.
- **Serialized Collection**: Collectors receive values one at a time, ensuring thread-safe processing without external synchronization.
- **Virtual Thread Support**: Optimized for Java 21 virtual threads using pinning-safe synchronization.
- **Exception Transparency**: Checked exceptions propagate directly through the collection process without being wrapped.
- **Cold Streams**: Flows are lazy and only execute when `collect()` is called.
- **Minimal Footprint**: Zero external dependencies and a focused API.

## Requirements

- Java 21 or higher.

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.denyshorman</groupId>
    <artifactId>nanoflow</artifactId>
    <version>0.1.1</version>
</dependency>
```

### Gradle

```kotlin
implementation("io.github.denyshorman:nanoflow:0.1.1")
```

## Usage

### Sequential Flow

For single-threaded emission, use `Flows.flow()`:

```java
var flow = Flows.<String>flow(emitter -> {
    emitter.emit("Hello");
    emitter.emit("World");
});

var results = new ArrayList<String>();
flow.collect(results::add);
```

### Concurrent Flow

For multithreaded emission, use `Flows.concurrentFlow()`. The collector is guaranteed to be called sequentially even if multiple threads emit concurrently.

```java
var flow = Flows.<Integer>concurrentFlow(emitter -> {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (var i = 0; i < 1000; i++) {
            final var value = i;
            executor.submit(() -> emitter.emit(value));
        }
    }
});

var results = new ArrayList<Integer>();
flow.collect(results::add);
```

### Exception Handling

Checked exceptions from the emitter are propagated to the `collect()` caller:

```java
var flow = Flows.<String>flow(emitter -> {
    var data = Files.readString(Path.of("data.txt"));
    emitter.emit(data);
});

try {
    flow.collect(System.out::println);
} catch (IOException e) {
    // Handle exception
}
```

### Cancellation

`concurrentFlow` supports standard thread interruption. Emitting threads will receive an `InterruptedException` if the collecting thread is interrupted.

```java
var thread = Thread.ofVirtual().start(() -> {
    var flow = Flows.<Integer>concurrentFlow(emitter -> {
        while (true) {
            emitter.emit(1); // Throws InterruptedException when the collecting thread is interrupted
        }
    });
    
    try {
        flow.collect(v -> {});
    } catch (InterruptedException e) {
        // Flow cancelled
    }
});

thread.interrupt();
```

## Technical Details

### Thread Safety

- **Flows.flow()**: The emitter must be used from a single thread. The collector is invoked on the same thread as `collect()`.
- **Flows.concurrentFlow()**: The emitter is thread-safe. The collector is guaranteed to be invoked sequentially (serialized) by whichever thread is currently emitting.

### Exception Propagation

In a `concurrentFlow`, if the collector throws an exception, it propagates back to the specific emitter thread that triggered it. Other concurrent emitter threads continue their execution independently.

### Performance

- **Sequential Flow**: Virtually zero overhead, comparable to direct iteration.
- **Concurrent Flow**: Uses `ReentrantLock` for serialization, which is efficient for Virtual Threads and prevents pinning.

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE.md) for details.
