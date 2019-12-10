# ChangeLog

## [1.0.0](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-importing-ckan/tags/1.0.0) (2019-11-08)

**Added:**
* buildInfo.json for build info via `/health` path
* config.schema.json
* `PIVEAU_LOG_LEVEL` for configuring general log level
* Change listener for configuration
* `sendHash` for configuring optional hash calculation in pipe
* `sedHash` to config schema

**Changed:**
* `PIVEAU_` prefix to logstash configuration environment variables
* Hash is optional and calculation is based on canonical algorithm
* Requires now latest LTS Java 11
* Docker base image to openjdk:11-jre

**Fixed:**
* Updated all dependencies

## [0.1.1](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-importing-ckan/tags/0.1.1) (2019-05-19)

**Fixed:**
* Don't use catalogue id as search filter

## [0.1.0](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-importing-ckan/tags/0.1.0) (2019-05-17)

**Added:**
* `catalogue` read from configuration and pass it to the info object
* Environment `PIVEAU_IMPORTING_SEND_LIST_DELAY` for a configurable delay
* `sendListDelay` pipe configuration option

**Changed:**
* Readme

**Removed:**
* `mode` configuration and fetchIdentifier

## [0.0.1](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-importing-ckan/tags/0.0.1) (2019-05-03)
Initial release
