# dotCMS Shopify Connector

### What's included:

1. dotCMS Shopify App to configure and test the connection between dotCMS and Shopify 
2. Two automatically created content types that can connect content with Shopify products and collections.
   - ShopifyProduct
   - ShopifyCollection
3. Code for 2 custom fields for the above content types for easy content/product stitching, including syncing shopify thumbnails.
4. REST endpoints for searching and retrieving information for Products and Product collections
5. Graphql Endpoint for proxying custom graphql requests to Shopify.
6. Velocity Viewtool `$dotshopify` for pulling data from Shopify Products and Product collections
7. Example .vtl components for rendering Shopify in dotCMS using `$dotshopify` tool 
   - Grid to display product collections
   - Product detail page


## Connector Installation

### Step 1: Install Shopify Headless sales channel
The dotCMS-Shopify Connector requires that you install the Shopify Headless Sales Channel in your Shopify store.  Open your Shopify store admin and click on the link to "Sales Channels >" in the left nav.  Type in "Headless" in the search box, click it and follow the prompts. Shopify will install the headless sales channel for you.  

Once you have installed the headless sales channel, you will need to generate a private access token.  Navigate to the "Headless Sales Channels" link in the left nav and in the Manage API Access screen, click on the "Manage" Button for the Storefront API.  Click on the "Generate Private Access Token" button.  Copy the token and save it somewhere safe.  You will need this token to configure the dotCMS Shopify Connector.

### Step 2: Install dotCMS/Shopify Connector Plugin
Upload the dotCMS Shopify plugin to your dotCMS instance. The latest plugin jar be found here: https://github.com/dotcms-community/com.dotcms.shopify/blob/main/target/shopify-25.05.20.jar.  Download this jar and then in your dotCMS, navigate to the plugins screen and upload the jar. After a few seconds, you should see the plugin load and become "active" - the plugin will automatically upload required assets into your `//{defaultHost}/application/shopify` directory.


### Step 3: Configure the dotCMS/Shopify Connector Plugin
Once your plugin is active, Navigate to the Admin > Apps screen and click on the "Shopify" icon for the dotCMS Shopify Connector.
To configure the dotCMS Connector, find the host you want to configure (or System Host for all sites) and then set 2 values in the app config. These are:
- shopify store key - e.g. `dot-demo-store`. This is the store name that you use to access the shopify storefront.
- shopify storefront private access token - this is the private key for the shopify headless storefront api that we installed in Step 1.

## What is in `/application/shopify` ?
The dotcms-shopify plugin installs a number of files on your default host under the path `/application/shopify`.  These include `.vtl` files for use as custom field and components to render on a dotCMS front end as well as files containing raw graphql queries that dotCMS uses to pull product and collection information.  Below is the directory structure and description of the files:

```

- /application/shopify/vtl/components (front end components for traditional implementations)
   - shopify-product-detail.vtl
      this vtl renders a product detail based upon a passed in product id or product handle
   - shopify-view-collection.vtl
      this vtl retrieves a passed in collection id and pulls the requested collection information for use by:
   - shopify-product-carousel.vtl
      not quite a carousel, more like a product grid that will display products from a collection.
      
- /application/shopify/vtl/custom-fields (reusable custom fields controls for content types)
   - shopify-collection-picker.vtl
      this is the custom field used to select a shopify collection and store 
      its reference id along side content for use when rendering. It also includes 
      a few custom properties that can be used to configure how the collection 
      behaves on when rendering. 
   - shopify-product-picker.vtl
      this is the custom field used to select a shopify product and store 
      its reference/id  along side content for use when rendering.  
         
- /application/shopify/gql (raw Graphql queries used to pull data from Shopify)
   - this directory includes the raw gql queries used by dotCMS to retrieve products 
      and collections.  Where possible, queries have been broken down into fragments 
      which can be reused across difference queries.  If you want to add a new field to
      retrieve when pulling product information, you can just edit the `product.fragment.gql`
      
```


## dotCMS Shopify REST APIs

dotCMS provides a useful apis to proxy to Shopify's API which can help alleviate 
cross-site scripting issue when requesting data.  To use these apis, you need to 
generate a dotCMS Access token for using the dotCMS API.  In the examples below the `$TOK` variable is 
a valid dotCMS access token.

There are two apis that are provided which can be used to access product and collection data respectively:

- `/v1/shopify/product`
and
- `/v1/shopify/collection`

#### Get Product By Id
```
curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/product/?id=9257301049561"
```

#### Get Product By handle
Each Shopify product has a handle.  The handle is a unique identifier for the product.  The handle is used to access the product in the Shopify Admin.  The following example shows how to get a product by handle.

```
curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/product/_search?handle=burton-freestyle-binding-2016"
```

#### Search Products
Searching products is a little more complex.  You can search for products by passing in a query string and optionally you can pass in a limit, sort key, cursor, first or last and reverse.  The default sort key is `RELEVANCE` and the default reverse is `false`.  The default limit is `20`.  The following examples show how to search for products.  To see all the which can be used see the ShopifySearchParams class.
```
curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/product/_search?query=boards"

curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/product/_search?query=boards&limit=1&sortKey=PRICE&reverse=true"
```

#### Get Collection By Id
```
curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/collection/?id=gid://shopify/Collection/438449930457"
```

#### Search Collections
```
curl -H"Authorization: Bearer $TOK" \
"http://127.0.0.1:8082/api/v1/shopify/collection/_search?query=cheap&limit=3"
```

## Shopify Graphql
The dotCMS / Shopify connector exposes an endpoint which can be used to use Shopify's raw Graphql endpoint which provides unlimited flexibility when requesting data from Shopify.  Here is a simple graphql example - it pulls back a product by handle and loops over the first 5 variants.  

```
curl -XPOST -H"Authorization: Bearer $TOK" \
-H"Content-Type:application/json" \
http://127.0.0.1:8082/api/v1/shopify/product/_gql -d '
{"query":"query product($handle: String!) {\n  product(handle: $handle) {\n    title\n    id\n    variants(first: 5) {\n      edges {\n        node {\n          id\n          title\n        }\n      }\n    }\n  }\n}","variables":{"handle":"burton-freestyle-binding-2016"},"operationName":"product"}
'
```

## `$dotshopify` - Server Side ViewTool 
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


### Examples with Searching Products with Pagination
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

## Developing
See [using the dotcli](https://github.com/dotcms-community/com.dotcms.shopify/tree/README-development.md) to learn how you can use the dotcli to live edit the .vtls and .gql queries that power much of this plugin.



## Building the plugin

Build using maven

```
./mvnw clean install
```

To skip tests, run

```
./mvnw clean install -DskipTests
```






### Important Disclaimer

This plugin is provided by dotCMS as an example only and is not warrentied or supported in any way. dotCMS is not responsible for any loss of data or for any damages, including general, special, incidental or consequential damages, arising out of its use.
