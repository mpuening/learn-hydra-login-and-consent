#!/bin/bash

cd $(dirname -- "$0")

sudo docker-compose \
	-f hydra-ory-login.yml \
	down \
	--remove-orphans


# sudo docker-compose kill
# sudo docker-compose rm -rf
