# basha

generated using Luminus version "4.19"

## How to use

1. Hit 'login' and create an account.
2. Logout and continue process for any users you want to share your sentence lists with.
3. Login to your original account.
4. Upload a sentence list for translating. Use your own or use [this sample](https://github.com/ajakate/basha/blob/master/resources/samples/sample.txt)
5. Share the list with the users you created.
6. Give your users the login info (offline)
7. Wait for them to finish tranlsating your list
8. Download an anki deck of translations with audio
9. Study and learn!

## Quick Deploy

For the fastest way to get a free instance to try out:
[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/ajakate/basha)


## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

You will also need:
- [docker](https://docs.docker.com/get-docker/) and [docker-compose](https://docs.docker.com/compose/install/)
- python3, you'll need this runnable as `python` so I recommend using [conda](https://docs.conda.io/en/latest/miniconda.html)
- npm
- sass

## Running

You'll need docker running for the database.
Once docker is running, run: `docker-compose up -d` in this directory to start postgres.

Then run `./bin/recreate_db` to provision the database correctly.

Create a file `dev-config.edn` in the root if it does not exist. Put this content in it:
```
{:dev true
 :port 3000
 :nrepl-port 7000
 :database-url "postgres://basha_user:basha_password@localhost:5432/dev"
 :token-secret "0f6cf8sdf4f58cfd" ;; <- this token secret can be any random string
}
```

First, run `lein install` and `npm install` in the main directory, then you'll need three tabs open...

### Tab 1:
`lein scss :development watch`

### Tab 2:
`npm run watch`

### Tab 3:

In this tab, you'll need the python env setup first. If you're using conda:

First time only:
```
conda create -n basha python=3.8.2
pip install -r requirements.txt
```

Every time (including first time):
`conda activate basha`

Then run:

```
lein repl
> (migrate)
> (start)
```

**NOTE:**

There is a small chance the migrate command will fail.
If this happens, go to the file `./resources/migrations/20211012223102-add-users-table.up.sql` and comment out line 1.

## License

Copyright © 2021 FIXME
