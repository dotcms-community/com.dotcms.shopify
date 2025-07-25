package com.dotcms.shopify.osgi;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.shopify.util.ShopifyApp.AppKey;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
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
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.io.IOUtils;


public class ActivatorUtil {

  private final File installedAppYaml = new File(
      ConfigUtils.getAssetPath() + File.separator + "server" + File.separator + "apps" + File.separator
          + AppKey.DOT_SHOPIFY_APP_KEY.appValue + ".yml");

  public static List<String> listFilesInPackage(@Nonnull String packagePath) {
    List<String> fileList = new ArrayList<>();

    try {
      String jarPath = ActivatorUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();

      try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath)) {
        java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          java.util.jar.JarEntry entry = entries.nextElement();
          String entryName = entry.getName();

          if (entryName.startsWith(packagePath) && !entry.isDirectory()) {
            fileList.add(entryName);
          }
        }
      }
    } catch (java.io.IOException e) {
      throw new RuntimeException("Error reading JAR file", e);
    }
    return fileList;
  }

  /**
   * Moves files from the plugin jar to the dotCMS virtual file system as fileAssets. If you do not specify a host, they
   * will be placed on the default host
   *
   * @param packagePathInJar        - the directory path in the jar to copy
   * @param destinationFolderPathIn - the destination directory in dotCMS to copy to
   */
  void moveJarFilestoFileAssets(@Nonnull String packagePathInJar, @Nonnull String destinationFolderPathIn) {
    this.moveJarFilestoFileAssets(packagePathInJar, destinationFolderPathIn,
        Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)).getOrElseThrow(
            DotRuntimeException::new));
  }

  /**
   * Moves files from the plugin jar to the dotCMS virtual file system as fileAssets. If you do not specify a host, they
   * will be placed on the default host
   *
   * @param packagePathInJar        - the directory path in the jar to copy
   * @param destinationFolderPathIn - the destination directory in dotCMS to copy to
   * @param site                    - the site to copy to
   */
  void moveJarFilestoFileAssets(@Nonnull String packagePathInJar, @Nonnull String destinationFolderPathIn,
      @Nonnull Host site) {



    Folder folder = Try.of(()->APILocator.getFolderAPI().findFolderByPath(destinationFolderPathIn,site,APILocator.systemUser(),false)).getOrNull();
    if(UtilMethods.isSet(()->folder.getIdentifier())){
      Try.run(()->APILocator.getFolderAPI().delete(folder,APILocator.systemUser(),false));
    }

    try {
      Folder destFolder = Try.of(() -> APILocator.getFolderAPI()
          .createFolders(destinationFolderPathIn, site, APILocator.systemUser(), false)).getOrElseThrow(
          DotRuntimeException::new);

      final String destinationFolderPath =
          destinationFolderPathIn.endsWith("/") ? destinationFolderPathIn : destinationFolderPathIn + "/";

      List<String> fileList = listFilesInPackage(packagePathInJar);
      for (String pathName : fileList) {
        String fileName = pathName.substring(pathName.lastIndexOf("/") + 1);

        Identifier id = APILocator.getIdentifierAPI().find(site, destinationFolderPath + fileName);
        if (UtilMethods.isSet(() -> id.getId())) {
          Logger.warn(this.getClass(), "File already exists: " + destinationFolderPath + fileName);
          continue;
        }

        // Create tmp file
        File tmpDir = Files.createTempFile("shopify", "").toFile();
        tmpDir.delete();
        tmpDir.mkdirs();
        File tmpFile = new File(tmpDir, fileName);

        // write content to tmp file
        try (final InputStream in = this.getClass().getResourceAsStream("/" + pathName)) {
          IOUtils.copy(in, Files.newOutputStream(tmpFile.toPath()));
        } catch (IOException e) {
          Logger.error(this.getClass(), "Error moving file: " + pathName, e);
          continue;
        }

        //

        FileAsset fileAsset = new FileAsset();
        fileAsset.setFolder(destFolder.getIdentifier());
        fileAsset.setHost(site.getIdentifier());
        fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, tmpFile);
        fileAsset.setTitle(fileName);

        fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser())
            .find(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).id());
        fileAsset.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());

        Contentlet dotfile = APILocator.getContentletAPI().checkin(fileAsset, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(dotfile, APILocator.systemUser(), false);
        Assert.verify(dotfile != null && dotfile.getIdentifier() != null, "Unable to create file asset: " + fileName);
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
   * @throws Exception
   */
  public void createShopifyExampleContentType() throws Exception {
    ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
    ContentType type = Try.of(() -> typeAPI.find("ShopifyExample")).getOrNull();

    if (!UtilMethods.isSet(type)) {
      type = typeAPI.save(ImmutableSimpleContentType
          .builder()
          .name("Shopify Example")
          .id(UUIDGenerator.generateUuid())
          .host(Host.SYSTEM_HOST)
          .folder(Folder.SYSTEM_FOLDER)
          .variable("ShopifyExample")

          .build());
    }

    List<Field> fields = new ArrayList<>();


    LinkedHashMap<String, Field> fieldMap = new LinkedHashMap<>(type.fieldMap());
    fieldMap.put("title", ImmutableTextField.builder()
        .name("Title")
        .id(UUIDGenerator.generateUuid())
        .contentTypeId(type.id())
        .variable("title")
        .indexed(true)
        .searchable(true)
        .listed(true)
        .build());

    fieldMap.put("shopifyProduct", ImmutableCustomField.builder()
        .name("Shopify Product")
        .id(UUIDGenerator.generateUuid())
        .contentTypeId(type.id())
        .variable("shopifyProduct")
        .indexed(true)
        .searchable(true)
        .listed(false)
        .values("#dotParse(\"/application/shopify/vtl/shopify-product-picker-custom-field.vtl\")\n")
        .build());

    fieldMap.put("shopifyCollection", ImmutableCustomField.builder()
        .name("Shopify Collection")
        .id(UUIDGenerator.generateUuid())
        .contentTypeId(type.id())
        .variable("shopifyCollection")
        .indexed(true)
        .searchable(true)
        .listed(false)
        .values("#dotParse(\"/application/shopify/vtl/shopify-collection-picker-custom-field.vtl\")\n")
        .build());



    List<FieldVariable> fieldVars = new ArrayList<>();
    fieldVars.add(ImmutableFieldVariable.builder()
        .name("customFieldOptions")
        .fieldId(fieldMap.get("shopifyProduct").id())
        .key("customFieldOptions")
        .value("{\"showAsModal\": true,  \"width\": \"600px\",  \"height\": \"675px\"}")
        .build());

    fieldVars.add(ImmutableFieldVariable.builder()
        .name("customFieldOptions")
        .fieldId(fieldMap.get("shopifyCollection").id())
        .key("customFieldOptions")
        .value("{\"showAsModal\": true,  \"width\": \"600px\",  \"height\": \"675px\"}")
        .build());

    typeAPI.save(type, new ArrayList(fieldMap.values()), fieldVars);

    String systemWorkflowScheme =APILocator.getWorkflowAPI().findSystemWorkflowScheme().getId();
    APILocator.getWorkflowAPI().saveSchemeIdsForContentType(type, Set.of(systemWorkflowScheme));

  }


}
