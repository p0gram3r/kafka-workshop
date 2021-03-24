#!/usr/bin/env bash

echo "################################## Run nginx"
export DOLLAR='$'
envsubst < ./src/nginx/nginx.conf.template > /etc/nginx/nginx.conf # /etc/nginx/conf.d/default.conf
envsubst < ./src/nginx/index.html > /usr/share/nginx/html/index.html # /etc/nginx/conf.d/default.conf
nginx -g "daemon off;"
