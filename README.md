# dotCMS Shopify Connector

## Connector Installation

#### Install Shopify Headless sales channel
The dotCMS-dotCMS Connector requires that you install the shopify Headless Sales Channel in Shopify.  Open your shopify store and click on the "Sales Channels >" in the left nav.  Type in "Headless" in the search box, click it and follow the prompts and shopify will install the headless sales channel for you.  

#### dotCMS Connector Plugin
Upload the dotCMS Shopify plugin to your dotCMS instance. Navigate to the Apps tab and click on the "Shopify" icon for the dotCMS Shopify Connector.
To configure the dotCMS Connector, you need to set 2 values in the app config. These are:

- shopify store key - e.g. `dot-demo-store`. This is the store name that you use to access the shopify storefront.
- shopify storefront private access token - this is the private key for the shopify storefront api.  This is available to you once you have installed the headless sales channel in Shopify



## dotCMS Shopify API

dotCMS provides a useful apis to proxy to Shopify's API which can help alleviate 
cross-site scripting issue when requesting data.  To use these apis, you need to 
generate a dotCMS Access token for using the dotCMS API.  The `$TOK` variable is 
a valid dotCMS access token.`



`/v1/shopify/product`
and
`/v1/shopify/collection`

#### Get Product By Id
```
curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/product/?id=9257301049561"
```
**Get Collection By Id**
```
curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/collection/?id=gid://shopify/Collection/438449930457"
```

**Search Collections**
```
curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/collection/_search?query=cheap&limit=3"
```



## Velocity Tool - `$dotshopify`
The plugin also provides a velocity viewtool  `$dotshopify` that allows you to pull product information from shopify as well. 

#### Product by Id
```
$dotshopify.getProduct("9257301049561").data.product.title
```


#### Product by Handle
```
$dotshopify.getProductByHandle("burton-freestyle-binding-2016").data.productByHandle.title
```

#### RAW GQL

```

## build query
#set($query = "query product($handle: String!, $firstVariants: Int = 1) { product(handle: $handle) { title id variants(first: $firstVariants) { edges { node { id title } } } } } ")

## set args
#set($args = {})
$!args.put("handle", "burton-freestyle-binding-2016")
$!args.put("firstVariants",5)

$dotshopify.gql($query, $args).data.product.title
```



### $dotshopify & Product Data

```
#set($product = $dotshopify.getProduct("9257301049561").data.product)

$product.title
- $product.productType
- $product.id
- $$product.priceRange.minVariantPrice.amount $product.priceRange.minVariantPrice.currencyCode
```


### Searching Products with Pagination
```
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

- [x] Respect Collection limits, Sort Order
- [x] Add support for product by Handle
- [ ] Add support for collection by Handle
- [ ] Add support for collection product pagination
- [x] Create a Product Detail .vtl example
- [ ] Use Shopify Interceptor to load product information into request attribute automatically
- [ ] Recommendations

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
