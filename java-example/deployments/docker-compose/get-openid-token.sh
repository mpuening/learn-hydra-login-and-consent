#!/bin/bash

cd $(dirname -- "$0")

sudo docker-compose \
	-f hydra.yml \
	exec hydra \
	hydra token user \
	--endpoint http://127.0.0.1:4444/ \
	--client-id hydra-auth-code-client \
	--client-secret hydra-secret \
	--port 5555 \
	--scope openid,offline
