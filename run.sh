#!/bin/bash

echo "Building Simple HTTP Server..."

mkdir -p build

echo "Compiling Java sources..."
javac -d build src/shared/*.java src/server/*.java src/client/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo
    echo "Starting HTTP Server..."
    echo "Access the server at: http://localhost:8080"
    echo "Press Ctrl+C to stop the server"
    java -cp build server.HttpServer
else
    echo "Compilation failed!"
    exit 1
fi