#!/bin/bash

# Code by Jordan Frimpter
# Command to grant permission to file to run [RUN THIS]: chmod +x build.sh

# code to remove carriage returns from files: sed -i -e 's/\r$//' <filename>

# compilation command [CHANGE THIS to match your project files]
javac Project2Node.java Neighbor.java Application.java MutualExclusion.java

echo done building
