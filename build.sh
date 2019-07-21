#!/bin/bash

cd great-wall-util-frontend
yarn
npm run build
cd ..

rm -rf great-wall-util-backend/src/main/resources/public
mv great-wall-util-frontend/dist great-wall-util-backend/src/main/resources/public

cd great-wall-util-backend
mvn clean package
cd ..

mv great-wall-util-backend/target/great-wall-util-web.jar .