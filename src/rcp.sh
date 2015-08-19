#!/bin/bash

if [ ! -d "$2" ]; then
    mkdir -p $2
fi

find $1/ -type f -print0 | while IFS= read -rd "" filename; do
    v=$((RANDOM % 4))
    if (( v == 0 )); then
        cp "$filename" $2/`uuidgen`.jpg
    fi
done
