#! /bin/bash

pg_dump -O -Fc -T schema_migrations -d $1 > $2
