if not exist "bin" mkdir "bin"

javac -cp de/codecoverage/tools/net/portscanner -d bin de/codecoverage/tools/net/portscanner/PortScanner.java
cd bin
jar cfm ../portscanner.jar ../mf.mf de/codecoverage/tools/net/portscanner/*.class
cd ..
