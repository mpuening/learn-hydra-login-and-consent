#!/bin/bash

cd $(dirname -- "$0")

sudo docker-compose \
	-f hydra-spring-boot-login.yml \
	 down \
	--remove-orphans

# sudo docker-compose kill
# sudo docker-compose rm -rf
