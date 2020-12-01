#!/bin/bash

cd $(dirname -- "$0")

sudo docker-compose \
	-f hydra.yml \
	exec hydra \
	hydra token client \
	--endpoint http://localhost:4444/ \
	--client-id hydra-service-client \
	--client-secret hydra-secret

