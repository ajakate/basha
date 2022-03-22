psql -c "create extension if not exists uuid-ossp" $DATABASE_URL

# https://github.com/yogthos/migratus/issues/63
# https://github.com/technomancy/shouter/blob/master/project.clj <- migrations
# java -Dclojure.main.report=stderr -cp target/uberjar/basha.jar migrate
