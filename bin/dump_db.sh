#! /bin/bash

pg_dump -O -Fc -a -T schema_migrations -d $1 > $2
