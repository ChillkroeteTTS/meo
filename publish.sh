#!/usr/bin/env bash

#npm version patch

cd bundle
webpack -p
cd ..

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3
DEBUG=electron-builder,electron-builder:* electron-builder --publish always -mwl

open dist
