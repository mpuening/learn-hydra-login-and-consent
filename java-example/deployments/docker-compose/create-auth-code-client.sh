#!/bin/bash

cd $(dirname -- "$0")

sudo docker-compose \
	-f hydra.yml \
	exec hydra \
	hydra clients create \
	--endpoint http://localhost:4445 \
	--id hydra-auth-code-client \
	--token-endpoint-auth-method none \
	--grant-types authorization_code,refresh_token \
	--response-types code,id_token \
	--scope openid,offline,profile.read \
	--callbacks http://localhost:8080/login/oauth2/code/hydra-openid,http://localhost:8080/login/oauth2/code/hydra-code,http://127.0.0.1:5555/callback \
	--post-logout-callbacks http://localhost:8080/bye

