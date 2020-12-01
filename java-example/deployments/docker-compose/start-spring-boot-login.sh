#!/bin/bash

cd $(dirname -- "$0")

sudo docker-compose \
	-f hydra-spring-boot-login.yml \
	up --build -d
