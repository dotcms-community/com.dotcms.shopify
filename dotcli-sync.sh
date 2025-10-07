#!/bin/sh

if [ ! -d .git ]; then
    echo "this must be run from project root"
    exit 1
fi

# we want the workspace to be in tmp 
mkdir -p .tmp
cp .dot-workspace.yml .tmp/
cd .tmp



# set up file dir
rm -rf files/live/en-us/demo.dotcms.com/application/shopify
dotcli files pull  -nr //demo.dotcms.com/application
mkdir -p files/live/en-us/demo.dotcms.com/application/shopify/vtl/custom-fields
mkdir -p files/live/en-us/demo.dotcms.com/application/shopify/vtl/components
mkdir -p files/live/en-us/demo.dotcms.com/application/shopify/gql

# hardlinks so you can edit them
ln ../src/main/resources/application/shopify/vtl/components/*.vtl files/live/en-us/demo.dotcms.com/application/shopify/vtl/components
ln ../src/main/resources/application/shopify/vtl/custom-fields/*.vtl files/live/en-us/demo.dotcms.com/application/shopify/vtl/custom-fields
ln ../src/main/resources/application/shopify/gql/*.gql files/live/en-us/demo.dotcms.com/application/shopify/gql/

# watch us develop!
dotcli files push  -w 2 ./files/live/en-us/demo.dotcms.com/application/shopify
