#!/usr/bin/env bash

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
    openssl aes-256-cbc -K $encrypted_4bc7aeb7f96e_key -iv $encrypted_4bc7aeb7f96e_iv -in secrets.tar.enc -out secrets.tar -d
    tar xvf secrets.tar
fi
