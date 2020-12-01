#!/bin/bash

cd $(dirname -- "$0")

sudo docker-compose \
	-f hydra-ory-login.yml \
	up --build -d
