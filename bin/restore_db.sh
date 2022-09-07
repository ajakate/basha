#! /bin/bash

pg_restore -O -c -d $1 $2
