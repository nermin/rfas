#!  /bin/bash
if [ $# -ne 2 ]
then
    echo "Usage `basename $0` <bind address> <number of worker threads>"
    exit
fi
export LD_LIBRARY_PATH=/usr/local/lib
java -jar -Djava.library.path=/usr/local/lib -Drequest.bind=5557 -Dresponse.bind=5558 -Dbind.address=$1 -Dworker.threads=$2 rfas-worker-assembly-1.0.jar
