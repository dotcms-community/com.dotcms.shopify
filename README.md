# dotCMS Shopify Connector

## App Config





## dotCMS Shopify API

dotCMS provides a useful api proxy to Shopify's API which can help alieviate cross site scripting issue when requesting data.

It can be found here:



## Velocity
The plugin also provides a velocity viewtool  `$dotshopify` that allows you to pull product information from shopify as well. 

### Product by Id
$dotshopify.getProduct("9257301049561").data.product.title
```

### Searching Products

```
#set($results = $dotshopify.searchProducts("boots", 3).data.products)

## pagination information
$results.pageInfo.startCursor
$results.pageInfo.endCursor
$results.pageInfo.hasNextPage
$results.pageInfo.hasPreviousPage

## product results
#foreach($product in $results.edges)
$product.node.title
$product.node.productType
$product.node.id
#end

## next page of product results
#set($results2 = $dotshopify.searchProducts("boots", 3, $results.pageInfo.endCursor, "AFTER").data.products)

#foreach($product in $results2.edges)
$product.node.title
$product.node.productType
$product.node.id
#end







## Building

Build using maven

```
./mvnw clean install
```

To skip tests, run

```
./mvnw clean install -DskipTests
```






### Important Disclaimer

This plugin is provided by dotCMS as an example only and is not warrentied or supported in any way. dotCMS is not responsible for any loss of data or for any damages, including
general, special, incidental or consequential damages, arising out of its use.
