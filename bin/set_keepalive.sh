#! /bin/bash

(echo "*/2 * * * * curl $1") | crontab -
service cron start
