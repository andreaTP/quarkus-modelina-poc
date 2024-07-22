# Quarkus - Modelina Extension

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->
[![Build](<https://img.shields.io/github/actions/workflow/status/quarkiverse/quarkus-modelina/build.yml?branch=main&logo=GitHub&style=flat-square>)](https://github.com/quarkiverse/quarkus-modelina/actions?query=workflow%3ABuild)
[![Maven Central](https://img.shields.io/maven-central/v/io.quarkiverse.modelina/quarkus-modelina.svg?label=Maven%20Central&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.modelina/quarkus-modelina)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)


Quarkus' extension for generation of client SDKs based on OpenAPI specification files.

This extension is based on the [Modelina](https://github.com/asyncapi/modelina).

**Want to contribute? Great!** We try to make it easy, and all contributions, even the smaller ones, are more than welcome. This includes bug reports, fixes, documentation, examples... But first, read [this page](CONTRIBUTING.md).

## Getting Started

If you have a supersonic, subatomic [Quarkus](https://quarkus.io/) project you can use this extension to generate code with Modelina:

```xml
<dependency>
  <groupId>io.quarkiverse.modelina</groupId>
  <artifactId>quarkus-modelina</artifactId>
  <version>VERSION</version>
</dependency>
```

remember to enable the code generation in the `quarkus-maven-plugin` configuration, if not already present, add `<goal>generate-code</goal>`:

```xml
<plugin>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-maven-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>build</goal>
        <goal>generate-code</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```
