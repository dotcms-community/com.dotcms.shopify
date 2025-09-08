package com.dotcms.shopify.osgi;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.shopify.util.AppKey;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import common.Assert;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.io.IOUtils;

public class ActivatorUtil {

   private final File installedAppYaml = new File(
           ConfigUtils.getAssetPath() + File.separator + "server" + File.separator + "apps" + File.separator
                   + AppKey.DOT_SHOPIFY_APP_KEY.appValue + ".yml");

   String[] fieldUUIDs = new String[]{
           "87ae3b42-4822-4ba8-b87c-258155f7012a",
           "97a69459-e0aa-4278-bfd8-5797ab59e9db",
           "f92e8cb0-1f95-4935-8782-92b62fc84001",
           "db87ccb0-1f95-4935-8782-92b62fc84034"
   };

   public static List<JarEntry> listFilesInPackage(@Nonnull String packagePathStr) {

      String packagePath = stripPackagePath(packagePathStr);
      List<JarEntry> jarEntries = new ArrayList<>();

      try {
         String jarPath = ActivatorUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
         try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath)) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
               JarEntry entry = entries.nextElement();
               if (!entry.getName().startsWith(packagePath + "/") || entry.getName()
                       .equalsIgnoreCase(packagePath + "/")) {
                  continue;
               }
               System.out.println("jar path:" + entry.getName());
               jarEntries.add(entry);

            }
         }
      } catch (java.io.IOException e) {
         throw new RuntimeException("Error reading JAR file", e);
      }
      return jarEntries;
   }

   public static Map<String,String> loadGQLQueryMap(@Nonnull String packagePathStr) {



      return Map.of();
   }

   /**
    * Moves files from the plugin jar to the dotCMS virtual file system as fileAssets. If you do not specify a host,
    * they will be placed on the default host
    *
    * @param packagePathInJar - the directory path in the jar to copy
    */
   void copyFromJar(@Nonnull String packagePathInJar) {
      this.copyFromJar(packagePathInJar,
              Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                      .getOrElseThrow(DotRuntimeException::new));
   }


   static String stripPackagePath(String packagePathInJar) {
      return packagePathInJar.startsWith("/") && packagePathInJar.endsWith("/")
              ? packagePathInJar.substring(1, packagePathInJar.length() - 1)
              : packagePathInJar.startsWith("/")
                      ? packagePathInJar.substring(1)
                      : packagePathInJar.endsWith("/")
                              ? packagePathInJar.substring(0, packagePathInJar.length() - 1)
                              : packagePathInJar;
   }


   /**
    * Moves files from the plugin jar to the dotCMS virtual file system as fileAssets. If you do not specify a host,
    * they will be placed on the default host
    *
    * @param packagePathInJar - the directory path in the jar to copy
    * @param site             - the site to copy to
    */
   void copyFromJar(@Nonnull String packagePathInJar, @Nonnull Host site) {

      String strippedPackagePath = stripPackagePath(packagePathInJar);

      List<JarEntry> directoryList = listFilesInPackage(strippedPackagePath).stream().filter(e -> e.isDirectory())
              .collect(Collectors.toList());
      List<JarEntry> fileList = listFilesInPackage(strippedPackagePath).stream().filter(e -> !e.isDirectory())
              .collect(Collectors.toList());

      try {
         // create folders first
         for (JarEntry e : directoryList) {

            String folderPath = "/" + e.getName();
            Logger.info(this.getClass(), "Creating folder: " + folderPath);
            Folder subFolder = APILocator.getFolderAPI()
                    .findFolderByPath(folderPath, site, APILocator.systemUser(), false);
            if (!UtilMethods.isSet(() -> subFolder.getIdentifier())) {
               APILocator.getFolderAPI().createFolders(folderPath, site, APILocator.systemUser(), false);
            }
         }

         for (JarEntry e : fileList) {

            String fileName = e.getName().substring(e.getName().lastIndexOf("/") + 1);
            String folderPath = "/" + e.getName().substring(0, e.getName().lastIndexOf("/"));
            String fullPath = folderPath + "/" + fileName;
            Folder destFolder = APILocator.getFolderAPI()
                    .findFolderByPath(folderPath, site, APILocator.systemUser(), false);
            Identifier id = APILocator.getIdentifierAPI().find(site, fullPath);
            if (UtilMethods.isSet(() -> id.getId())) {
               Logger.warn(this.getClass(), "File already exists: " + folderPath + fileName);
               continue;
            }

            System.out.println("Writing File:" + fullPath);

            Logger.info(this.getClass(), "Creating file: " + fullPath);
            // Create tmp file
            File tmpDir = Files.createTempFile("shopify", "").toFile();
            tmpDir.delete();
            tmpDir.mkdirs();
            File tmpFile = new File(tmpDir, fileName);

            // write content to tmp file
            try (final InputStream in = this.getClass().getResourceAsStream("/" + e.getName())) {
               IOUtils.copy(in, Files.newOutputStream(tmpFile.toPath()));
            } catch (IOException ioe) {
               Logger.error(this.getClass(), "Error moving file: " + e.getName(), ioe);
               continue;
            }

            //

            Contentlet fileAsset = new Contentlet();
            fileAsset.setFolder(destFolder.getIdentifier());
            fileAsset.setHost(site.getIdentifier());
            fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, tmpFile);
            fileAsset.setStringProperty(FileAssetAPI.TITLE_FIELD, fileName);
            fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).id());

            fileAsset.setProperty(Contentlet.DISABLE_WORKFLOW, true);

            fileAsset.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());

            Contentlet dotfile = APILocator.getContentletAPI().checkin(fileAsset, APILocator.systemUser(), false);
            dotfile.setProperty(Contentlet.DISABLE_WORKFLOW, true);

            APILocator.getContentletAPI().publish(dotfile, APILocator.systemUser(), false);
            Assert.verify(dotfile != null && dotfile.getIdentifier() != null,
                    "Unable to create file asset: " + fileName);
            tmpFile.delete();
            tmpDir.delete();

         }
         ;
      } catch (Exception e) {
         throw new DotRuntimeException("Error moving files from jar to file assets:" + e.getMessage(), e);
      }

   }

   /**
    * copies the App yaml to the apps directory and refreshes the apps
    *
    * @throws IOException
    */
   public void copyAppYml() throws IOException {

      Logger.info(this.getClass().getName(), "copying YAML File:" + installedAppYaml);
      try (final InputStream in = this.getClass()
              .getResourceAsStream("/" + AppKey.DOT_SHOPIFY_APP_KEY.appValue + ".yml")) {
         IOUtils.copy(in, Files.newOutputStream(installedAppYaml.toPath()));
      }
      CacheLocator.getAppsCache().clearCache();

   }

   /**
    * Deletes the App yaml to the apps directory and refreshes the apps
    *
    * @throws IOException
    */
   public void deleteYml() throws IOException {

      Logger.info(this.getClass().getName(), "deleting the YAML File:" + installedAppYaml);

      installedAppYaml.delete();
      CacheLocator.getAppsCache().clearCache();

   }

   /**
    * Automatically creates a content type with all the custom fields for testing
    *
    * @throws Exception
    */
   public void createShopifyExampleContentType() throws Exception {
      ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
      ContentType type = Try.of(() -> typeAPI.find("ShopifyExample")).getOrNull();

      if (!UtilMethods.isSet(type)) {
         type = typeAPI.save(
                 ImmutableSimpleContentType.builder().name("Shopify Example").id(UUIDGenerator.generateUuid())
                         .host(Host.SYSTEM_HOST).folder(Folder.SYSTEM_FOLDER).variable("ShopifyExample")

                         .build());
      }

      List<Field> fields = new ArrayList<>();

      LinkedHashMap<String, Field> fieldMap = new LinkedHashMap<>(type.fieldMap());

      fieldMap.put("RowField00001",
              ImmutableRowField.builder().name("RowField00001").id(UUIDGenerator.generateUuid()).sortOrder(0)
                      .variable("RowField00001").build());
      fieldMap.put("ColumnField00001",
              ImmutableColumnField.builder().name("ColumnField00001").id(UUIDGenerator.generateUuid()).sortOrder(10)
                      .variable("ColumnField00001").build());

      fieldMap.put("title",
              ImmutableTextField.builder().name("Title").id(fieldUUIDs[0]).contentTypeId(type.id()).variable("title")
                      .indexed(true).sortOrder(20).searchable(true).listed(true).build());

      fieldMap.put("shopifyProduct",
              ImmutableCustomField.builder().name("Shopify Product").id(fieldUUIDs[1]).contentTypeId(type.id())
                      .variable("shopifyProduct").indexed(true).sortOrder(31).searchable(true).listed(false)
                      .values("#dotParse(\"/application/shopify/vtl/custom-fields/shopify-product-picker-custom-field.vtl\")\n")
                      .build());

      fieldMap.put("shopifyCollection",
              ImmutableCustomField.builder().name("Shopify Collection").id(fieldUUIDs[2]).contentTypeId(type.id())
                      .variable("shopifyCollection").indexed(true).sortOrder(42).searchable(true).listed(false)
                      .values("#dotParse(\"/application/shopify/vtl/custom-fields/shopify-collection-picker-custom-field.vtl\")\n")
                      .build());

      List<FieldVariable> fieldVars = new ArrayList<>();
      fieldVars.add(
              ImmutableFieldVariable.builder().name("customFieldOptions").fieldId(fieldMap.get("shopifyProduct").id())
                      .key("customFieldOptions")
                      .value("{\"showAsModal\": true,  \"width\": \"600px\",  \"height\": \"675px\"}").build());

      fieldVars.add(ImmutableFieldVariable.builder().name("customFieldOptions")
              .fieldId(fieldMap.get("shopifyCollection").id()).key("customFieldOptions")
              .value("{\"showAsModal\": true,  \"width\": \"600px\",  \"height\": \"675px\"}").build());

      typeAPI.save(type, new ArrayList(fieldMap.values()), fieldVars);

      String systemWorkflowScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme().getId();
      APILocator.getWorkflowAPI().saveSchemeIdsForContentType(type, Set.of(systemWorkflowScheme));

   }

}
