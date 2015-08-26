# Graylog Twilio SMS alarm callback
[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-twiliosms.svg)](https://travis-ci.org/Graylog2/graylog2-alarmcallback-twiliosms)

An alarm callback plugin for integrating the [Twilio SMS API](https://www.twilio.com/sms) into [Graylog](https://www.graylog.org/).


## Build

This project is using Maven and requires Java 7 or higher.

You can build the plugin (JAR) with `mvn package`. 

DEB and RPM packages can be built with `mvn jdeb:jdeb` and `mvn rpm:rpm` respectively.
