# dotCMS Shopify Connector

## App Config





## dotCMS Shopify API

dotCMS provides a useful apis to proxy to Shopify's API which can help alleviate cross-site scripting issue when requesting data.

It can be found here:

`/v1/shopify/product`
and
`/v1/shopify/collection`

```

curl -H"Authorization: Bearer $TOK" "http://127.0.0.1:8082/api/v1/shopify/collection/?id=9257301049561"


curl -H"Authorization: Bearer $TOK" "http://127.0.0.1:8082/api/v1/shopify/collection/?id=gid://shopify/Collection/438449930457"


curl -H"Authorization: Bearer $TOK" "http://127.0.0.1:8082/api/v1/shopify/collection/_search?query=cheap&limit=3"

```



## Velocity Tool - $dotshopify
The plugin also provides a velocity viewtool  `$dotshopify` that allows you to pull product information from shopify as well. 

### Product by Id
```
$dotshopify.getProduct("9257301049561").data.product.title
```

### $dotshopify &  Products

```

#set($product = $dotshopify.getProduct("9257301049561").data.product)

$product.title
- $product.productType
- $product.id
- $$product.priceRange.minVariantPrice.amount $product.priceRange.minVariantPrice.currencyCode



#set($results = $dotshopify.searchProducts("boots", 3).data.products)

# pagination information
start: $results.pageInfo.startCursor
end: $results.pageInfo.endCursor
hasNext: $results.pageInfo.hasNextPage
hasPrev: $results.pageInfo.hasPreviousPage
---

# product results
#foreach($product in $results.edges)
$product.node.title
- $product.node.productType
- $product.node.id
- $$product.node.priceRange.minVariantPrice.amount $product.node.priceRange.minVariantPrice.currencyCode
- 
#end
---

# next page of product results
#set($results2 = $dotshopify.searchProducts("boots", 3, $results.pageInfo.endCursor, "AFTER").data.products)
#foreach($product in $results2.edges)
$product.node.title
- $product.node.productType
- $product.node.id
#end
---

# Pulling a collection by id
#set($collection = $dotshopify.getCollectionById("gid://shopify/Collection/438449930457"))
$collection.data.collection.title
--------
#foreach($product in $collection.data.collection.products.edges)
$product.node.title
#end
```


TODOS: 

- [ ] Respect Collection limits, Sort Order
- [ ] Add support for product by Handle
- [ ] Add support for collection by Handle
- [ ] Add support for collection product pagination
- [ ] Create a Product Detail .vtl example
- [ ] Use Shopify Interceptor to load product information into request attribute automatically
- [ ] Recommend

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
