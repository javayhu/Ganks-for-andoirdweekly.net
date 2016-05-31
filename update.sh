#!/bin/bash

echo 'update starts'

mvn compile

mvn exec:java -Dexec.mainClass="data.AndroidWeeklyNetCrawler"

mvn exec:java -Dexec.mainClass="data.AndroidWeeklyNetParser"

echo 'update stops'




