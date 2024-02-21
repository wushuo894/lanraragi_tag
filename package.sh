#!/bin/bash

mvn -B package --file pom.xml
cp target/lanraragi_tag-jar-with-dependencies.jar ./