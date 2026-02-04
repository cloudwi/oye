#!/bin/bash

# .env.local 파일이 있으면 로드
if [ -f .env.local ]; then
    export $(grep -v '^#' .env.local | xargs)
fi

./gradlew bootRun
