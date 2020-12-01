#!/bin/bash

cd $(dirname -- "$0")

sudo docker-compose \
	-f hydra.yml \
	exec hydra \
	hydra clients create \
	--endpoint http://127.0.0.1:4445/ \
	--id hydra-service-client \
	--secret hydra-secret \
	--grant-types client_credentials,refresh_token \
	--response-types code,id_token \
	--scope offline,profile.read
