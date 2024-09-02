#!/bash

./gradlew :modules:transport-netty4:build -x test -x check

cp ./modules/transport-netty4/build/distributions/transport-netty4-client-7.11.2.jar /System/Volumes/Data/data/program/es/elasticsearch-7.11.2/modules/x-pack-core/transport-netty4-client-7.11.2.jar



