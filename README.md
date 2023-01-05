# Vert.x Virtual Threads Incubator

[![Build Status](https://github.com/vert-x3/vertx-virtual-threads-incubator/workflows/CI/badge.svg?branch=main)](https://github.com/vert-x3/vertx-virtual-threads-incubator/actions?query=workflow%3ACI)

Incubator for virtual threads based prototypes.

## Prerequisites

- [Vert.x 4.3.7](https://vertx.io/docs/4.3.7)
- Java 19 using preview feature
  - [OpenJDK 19](https://jdk.java.net/19/)
  - [Maven](https://stackoverflow.com/questions/52232681/compile-and-execute-a-jdk-preview-feature-with-maven)
  - [Intellij](https://foojay.io/today/how-to-run-project-loom-from-intellij-idea/)

## Projects

- [Async/await incubator](vertx-async-await-incubator)
- [Execute blocking incubator](vertx-execute-blocking-incubator)
- Synchronous Vert.x incubator
  - [Core](vertx-core-sync-incubator)
  - [Web](vertx-web-sync-incubator)
- [Examples](examples)

## Usage

### enable preview flag must be enabled

```xml
<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <release>19</release>
          <compilerArgs>--enable-preview</compilerArgs>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <argLine>--enable-preview</argLine>
        </configuration>
      </plugin>
    </plugins>
  </pluginManagement>
</build>
```

### snapshots are available at s01.oss.sonatype.org

```xml
  <repositories>
  <repository>
    <id>vertx-snapshots-repository</id>
    <name>Vert.x Snapshots Repository</name>
    <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
    <releases>
      <enabled>false</enabled>
    </releases>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```
