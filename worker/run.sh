#!/bin/bash
export LD_LIBRARY_PATH=/usr/local/lib
java -jar -Djava.library.path=/usr/local/lib -Drequest.bind=5557 -Dresponse.bind=5558 -Dworker.threads=2 rfas-worker-assembly-1.0.jar
