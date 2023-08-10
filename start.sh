#!/bin/sh

git pull
mvn clean package
clear
mvn -q exec:java -Dexec.mainClass="MainKt" -Dexec.args="MTEwOTQ5NDQwOTIxNDU2NjUwMA.GUVQv1.GgvxRMUHVceyv7-xWdxpgi2F7Pv8LiHlUpMcFg"
