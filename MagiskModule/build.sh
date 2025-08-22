#!/bin/bash

# Clean previous build
chmod +x ./cleanup.sh

../cleanup.sh

rm -f CheckMateModule.zip

# Package files
zip -9X CheckMateModule.zip \
  -r META-INF system common \
  customize.sh module.prop \
  post-fs-data.sh service.sh \
  README.md LICENSE \
  -x "*/.DS_Store" "*__MACOSX*"

# Set compression method
zip -Z store CheckMateModule.zip -r .
