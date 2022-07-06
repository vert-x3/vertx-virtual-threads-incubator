# Vert.x Virtual Threads Incubator

[![Build Status](https://github.com/vert-x3/vertx-virtual-threads-incubator/workflows/CI/badge.svg?branch=main)](https://github.com/vert-x3/vertx-virtual-threads-incubator/actions?query=workflow%3ACI)

Incubator for virtual threads based prototypes.

## Prerequisites

- [Vert.x 4.3.2](https://vertx.io/docs/4.3.2)
- Java 19 using preview feature
  - [OpenJDK 19 EA](https://jdk.java.net/19/)
  - [Maven](https://stackoverflow.com/questions/52232681/compile-and-execute-a-jdk-preview-feature-with-maven)
  - [Intellij](https://foojay.io/today/how-to-run-project-loom-from-intellij-idea/)

## Projects

- [Async/await incubator](vertx-async-await-incubator/README.md)

## Usage

Snapshots are available at s01.oss.sonatype.org

```xml
<repository>
  <id>sonatype-nexus-snapshots</id>
  <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
  <snapshots>
    <enabled>true</enabled>
  </snapshots>
  <layout>default</layout>
  <releases>
    <enabled>false</enabled>
  </releases>
</repository>
```

