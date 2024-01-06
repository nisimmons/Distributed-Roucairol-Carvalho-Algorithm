Created by Nate Simmons for CS 6378 Advanced Operating Systems Class with bash scripts from Jordan Frimpter
When run with a valid config.txt file, this program creates a number of nodes which 
can communicate via sockets and access a resource mutually exclusively. They use vector clocks to ensure 
exclusion, and there is log output and an evaluator to check correctness post-op.
