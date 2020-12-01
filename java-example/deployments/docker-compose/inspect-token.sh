#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo "usage: $0 access_token"
    exit 1
fi

cd $(dirname -- "$0")

echo $1
sudo docker-compose \
	-f hydra.yml \
	exec hydra \
	hydra token introspect \
	--endpoint http://127.0.0.1:4445/ \
	--client-id hydra-service-client \
	--client-secret hydra-secret \
	"$1"

