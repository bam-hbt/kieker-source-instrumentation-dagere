# Kieker Source Instrumentation

*This is temporary fork, which is needed to deploy the artifacts to maven central. As soon as regular Kieker builds are possible, this repository will be discontinued.*

To measure the performance of method executions, measurement probes need to be inserted to the measured method. This projects provides instrumentation by changing the source code of the project. Alternatives, e.g. instrumentation via AspectJ, create more overhead and might therefore hinder identification of performance changes.

## Usage

TODO

## Versioning

The major version of kieker-source-instrumentation equals the major version of Kieker, i.e. versions built to instrument with Kieker 1.15 will be named 1.15.x (or 1.15.x-SNAPSHOT).
