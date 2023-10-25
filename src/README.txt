Created by Nate Simmons on 10/10/2023 for CS 6378 Advanced Operating Systems Class with code from Jordan Frimpter
When run with a valid config.txt file, this program creates a number of nodes which
can communicate via sockets. The .java source files are included, as well as a
launcher and cleanup script.

build.sh	- builds the java program
launcher.sh	- launches the java program on dcXX machines. Requires my password unless you change the directories specified in the scripts to your own
cleanup.sh	- kills all processes
cleanFiles.sh	- removes .class and .out files

Make sure the script has execution privileges; running the following command will grant them:

  chmod +x build.sh launcher.sh cleanup.sh cleanFiles.sh

Then run the scripts as desired.
