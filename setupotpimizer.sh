#!/bin/bash

git clone git://github.com/magarciaEPFL/scala.git GenRefactored99sZ
cd GenRefactored99sZ
git checkout -b GenRefactored99sZ origin/GenRefactored99sZ
ant all.clean && ant
