package com.dotcms.shopify.osgi;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ContentTypeUtil {


    static final String[] PRODUCT_UUIDS = new String[]{
            "87ae3b42-4822-4ba8-b87c-258155f7012a",
            "97a69459-e0aa-4278-bfd8-5797ab59e9db",
            "f92e8cb0-1f95-4935-8782-92b62fc84001",
            "db87ccb0-1f95-4935-8782-92b62fc84034",
            "31224cd3-f1d6-4662-88bc-7e0a5b319f2f",
            "8fc44c5a-5aee-4408-bba5-994a936c02b2",
            "dc5011d9-43b9-4b86-939a-86fe2d86ec3c",
            "e4c1d700-8821-46f1-9f70-8bbb52efe02d",
            "fc05dd60-993b-411f-a790-e2ac0a848858",
            "1784837b-a3e6-4f6e-bd0b-b302beb1051c",
            "bf3760fa-c279-4af4-8209-97376d8d24d8",
            "33af1cb9-8f40-4fc3-ba63-1e0bfe0efbcb",
            "d48db025-52ee-45dd-8266-ca7c4657da08",
            "9f332c8a-528c-4d45-b73d-fc3e2cc5a6b0",
            "8b29a9f6-b048-4cd0-b5dd-62400b033c4b",
            "d28c7ff1-8220-4d08-bec2-cb25aad860e0",
            "83d873fe-36e0-419f-9a8f-a8580b2fda16",
            "f31df149-f1c4-49fd-9e08-212cb5bf9eb3",
            "7e00a64e-9d90-40eb-8ec2-4d51b675df54",
            "48cf1e2e-098a-49a8-85cf-fb34dd7b0b6c",
            "8fd1b279-060b-43b9-b351-c7b9e44fc7c8",
            "ba19fc37-6260-4fde-97b5-43ea3df4396a",
            "63edd7e7-1703-4589-aab0-3e4c7285138b"};


    static final String[] COLLECTION_UUIDS = new String[]{
            "4a93109f-91a0-4e8b-89bd-87eeafe75c33",
            "9f963d67-500b-41f6-8023-e0c71b2cab9a",
            "5c5ee7be-dee8-471e-87af-e09998dc66ae",
            "b639326b-59d1-4653-9f92-5cc3f6edc160",
            "2da492ea-d3b8-4cea-b644-c32c96da98f6",
            "3608007f-b9b1-4870-a7e9-3bae8702060f",
            "fbc03cd8-9eda-4b00-9a84-627dae70373d",
            "62485b56-2ad6-4255-a3f4-a26c369f3454",
            "5437e143-bcf2-4373-b762-67cd5b4c83a7",
            "840fbacf-3c4d-4b58-a677-8d3c1593c155",
            "a3798b13-705a-453f-839a-0f6011b15434",
            "7a5fed5f-ae54-486d-8311-3c6c05bc4661",
            "0f9cc2d4-10d4-4afb-8470-15350aa4deb4",
            "85f3a5f8-f3b6-41d5-859f-d977ec41df17",
            "ed6fc4bc-03b6-4ad5-a0a6-f8436c5e372a",
            "6916d819-18e4-4af8-9990-bffd214eeab7",
            "69c395a3-179d-4877-822e-e0a45f417b9c",
            "18d97432-506d-4ac6-8d3c-8669f1e38467",
            "924c4361-d1d0-422a-9edd-93fa9829fc5a",
            "a34bfef3-b3e1-4bba-a636-115917c8f59d",
            "ed269721-21f6-4d7d-9495-a38d8605425d",
            "c8887e95-d059-4b3f-b14b-f19f286b7ff8",
            "f32a31be-84fe-4a06-b777-94d474c6839b",
            "ec477ee0-85b4-4368-b60e-3d59b894de31",
            "644eabe2-a4db-4389-8b44-df9ba89a0407",
            "8eac6218-50cd-4fd3-9ccf-f9c1efe743fa",
            "9bda0e75-77db-4d89-b410-79f5c819b08e",
            "11e4e3ba-8055-4745-a5cb-9fee93d68be5",
            "38649833-c2b4-4620-99f2-b1a57ec7fb64",
            "97b945a0-5a99-4ecb-8db9-bc8884d2deb5",
            "ba3c3825-bbe8-4f5b-aaeb-1b7c8ef9e401"

    };

    /**
     * Automatically creates a content type with all the custom fields for testing
     *
     * @throws Exception
     */
    public static void createShopifyProductType() throws Exception {
        ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        ContentType type = Try.of(() -> typeAPI.find("dotShopifyProduct")).getOrNull();

        if (!UtilMethods.isSet(type)) {
            type = typeAPI.save(
                    ImmutableSimpleContentType.builder().name("Shopify Product")
                            .icon("shopping_cart")
                            .host(Host.SYSTEM_HOST).folder(Folder.SYSTEM_FOLDER).variable("dotShopifyProduct")
                            .build());
        }

        List<Field> fields = new ArrayList<>(type.fields());
        List<FieldVariable> fieldVars = new ArrayList<>();
        for (Field field : fields) {
            fieldVars.addAll(field.fieldVariables());
        }

        if (fields.stream().noneMatch(field -> field.variable().equalsIgnoreCase("RowField00001"))) {
            fields.add(
                    ImmutableRowField.builder().name("RowField00001").id(PRODUCT_UUIDS[1]).sortOrder(0)
                            .variable("RowField00001").build());
            fields.add(
                    ImmutableColumnField.builder().name("ColumnField00001").id(PRODUCT_UUIDS[2]).sortOrder(10)
                            .variable("ColumnField00001").build())
        }

        if (fields.stream().noneMatch(field -> field.variable().equalsIgnoreCase("title"))) {
            fields.add(
                    ImmutableTextField.builder().name("Title").id(PRODUCT_UUIDS[3]).contentTypeId(type.id())
                            .variable("title")
                            .indexed(true).sortOrder(20).searchable(true).listed(true).build());
        }

        if (fields.stream().noneMatch(field -> field.variable().equalsIgnoreCase("shopifyProduct"))) {

            fields.add(
                    ImmutableCustomField.builder().name("Shopify Product").id(PRODUCT_UUIDS[4]).contentTypeId(type.id())
                            .variable("shopifyProduct").indexed(true).sortOrder(31).searchable(true).listed(false)
                            .values("#dotParse(\"/application/shopify/vtl/custom-fields/shopify-product-picker-custom-field.vtl\")\n")
                            .build());
        }

        if (fields.stream().noneMatch(field -> field.variable().equalsIgnoreCase("shopifyProductImage"))) {

            fields.add(
                    ImmutableBinaryField.builder().name("Product Image")
                            .id(PRODUCT_UUIDS[5])
                            .contentTypeId(type.id())
                            .variable("shopifyProductImage")
                            .indexed(true)
                            .sortOrder(60)
                            .searchable(false)
                            .listed(false)
                            .build());
        }

        fieldVars.add(
                ImmutableFieldVariable.builder()
                        .name("customFieldOptions")
                        .fieldId(PRODUCT_UUIDS[4])
                        .id(PRODUCT_UUIDS[6])
                        .key("customFieldOptions")
                        .value("{\"showAsModal\": true,  \"width\": \"600px\",  \"height\": \"675px\"}").build());

        type =typeAPI.save(type, fields, fieldVars);


        if (APILocator.getWorkflowAPI().findSchemesForContentType(type).isEmpty()) {
            String systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme().getId();
            APILocator.getWorkflowAPI().saveSchemeIdsForContentType(type, Set.of(systemWorkflowScheme));
        }

    }


    /**
     * Automatically creates a content type with all the custom fields for testing
     *
     * @throws Exception
     */
    public static void createShopifyCollectionType() throws Exception {

        ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        ContentType type = Try.of(() -> typeAPI.find("dotShopifyCollection")).getOrNull();

        if (!UtilMethods.isSet(type)) {
            type = typeAPI.save(
                    ImmutableWidgetContentType.builder().name("Shopify Collection")
                            .icon("collections")
                            .host(Host.SYSTEM_HOST).folder(Folder.SYSTEM_FOLDER).variable("dotShopifyCollection")
                            .build());
        }

        List<Field> fields = new ArrayList<>(type.fields());
        List<FieldVariable> fieldVars = new ArrayList<>();

        fields.removeIf(field -> field.id().equals("widgetPreexecute") || field.id().equals("widgetUsage"));
        for (Field field : fields) {
            fieldVars.addAll(field.fieldVariables());
        }

        Optional<Field> collectionField = fields.stream().filter(field -> field.id().equals("shopifyCollection"))
                .findFirst();
        Optional<Field> shopifyCollectionImage = fields.stream()
                .filter(field -> field.id().equals("shopifyCollectionImage")).findFirst();

        if (collectionField.isEmpty()) {
            fields.add(ImmutableCustomField.builder().name("Shopify Collection")
                    .id(COLLECTION_UUIDS[4])
                    .contentTypeId(type.id())
                    .variable("shopifyCollection")
                    .indexed(true)
                    .sortOrder(40)
                    .searchable(true)
                    .listed(false)
                    .values("#dotParse(\"/application/shopify/vtl/custom-fields/shopify-collection-picker-custom-field.vtl\")\n")
                    .build());

            if (fieldVars.stream().noneMatch(fieldVar -> fieldVar.fieldId().equals(COLLECTION_UUIDS[4]))) {
                fieldVars.add(ImmutableFieldVariable.builder()
                        .name("customFieldOptions")
                        .fieldId(COLLECTION_UUIDS[4])
                        .key("customFieldOptions")
                        .value("{\"showAsModal\": true,  \"width\": \"600px\",  \"height\": \"675px\"}").build());
            }


        }

        if (shopifyCollectionImage.isEmpty()) {
            fields.add(
                    ImmutableBinaryField.builder().name("Collection Image")
                            .id(COLLECTION_UUIDS[5])
                            .contentTypeId(type.id())
                            .variable("shopifyCollectionImage")
                            .indexed(true)
                            .sortOrder(60)
                            .searchable(false)
                            .listed(false)
                            .build());
        }

        type = typeAPI.save(type, fields, fieldVars);

        if (APILocator.getWorkflowAPI().findSchemesForContentType(type).isEmpty()) {
            String systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme().getId();
            APILocator.getWorkflowAPI().saveSchemeIdsForContentType(type, Set.of(systemWorkflowScheme));
        }


    }


}
