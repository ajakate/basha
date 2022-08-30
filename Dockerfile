FROM clojure:openjdk-8-lein-slim-buster

RUN apt update

# install Python
RUN apt-get install -y --no-install-recommends make build-essential libssl-dev zlib1g-dev libbz2-dev libreadline-dev libsqlite3-dev wget ca-certificates curl llvm libncurses5-dev xz-utils tk-dev libxml2-dev libxmlsec1-dev libffi-dev liblzma-dev mecab-ipadic-utf8 git
ENV PYENV_ROOT /root/.pyenv
ENV PATH $PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH
RUN curl https://pyenv.run | bash
RUN pyenv update
RUN pyenv install 3.8.2
RUN pyenv global 3.8.2
RUN pyenv rehash

# sass and ffmpeg
RUN apt install -y nodejs npm ffmpeg
RUN npm install -g sass

# pg_config
RUN apt install -y libpq-dev

# dir setup
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

# clj deps
COPY project.clj /usr/src/app/
RUN lein deps

# py deps
COPY requirements.txt /usr/src/app/
RUN pip install -r requirements.txt

COPY . /usr/src/app

RUN lein uberjar

CMD ["java", "-Dclojure.main.report=stderr", "-cp", "target/uberjar/basha.jar", "clojure.main", "-m", "basha.core"]
