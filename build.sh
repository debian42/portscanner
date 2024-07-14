#!/bin/bash

if [ ! -d "bin" ]; then
  mkdir -p "bin"
fi

javac -cp de/codecoverage/tools/net/portscanner -d bin de/codecoverage/tools/net/portscanner/PortScanner.java
if ! [[ $? -eq 0 ]]; then    
    echo "Compiling failed"
    exit 1
fi
cd bin
jar cfm ../portscanner.jar ../mf.mf de/codecoverage/tools/net/portscanner/*.class
if ! [[ $? -eq 0 ]]; then    
    echo "Creating jar failed"
    exit 1
fi
cd ..

echo "portscanner.jar created"
