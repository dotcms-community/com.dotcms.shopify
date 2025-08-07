package com.dotcms.shopify.listener;

import com.dotcms.ai.validator.AIAppValidator;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotcms.shopify.util.ShopifyApp.AppKey;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public final class ShopifyAppListener implements EventSubscriber<AppSecretSavedEvent>,
    KeyFilterable {

  private final HostAPI hostAPI;

  public ShopifyAppListener(final HostAPI hostAPI) {
    this.hostAPI = hostAPI;
  }

  public ShopifyAppListener() {
    this(APILocator.getHostAPI());
  }

  /**
   * Notifies the listener of an {@link AppSecretSavedEvent}.
   *
   * <p>
   * This method is called when an {@link AppSecretSavedEvent} occurs. It performs the following
   * actions:
   * <ul>
   * <li>Logs a debug message if the event is null or the event's host identifier
   * is blank.</li>
   * <li>Finds the host associated with the event's host identifier.</li>
   * <li>Resets the AI models for the found host's hostname.</li>
   * <li>Validates the AI configuration using the {@link AIAppValidator}.</li>
   * </ul>
   * </p>
   *
   * @param event the {@link AppSecretSavedEvent} that triggered the notification
   */
  @Override
  public void notify(final AppSecretSavedEvent event) {
    if (Objects.isNull(event)) {
      Logger.info(this, "Missing event, aborting");
      return;
    }


    if (StringUtils.isBlank(event.getHostIdentifier())) {
      Logger.info(this, "Missing event's host id, aborting");
      return;
    }

    String userId = event.getUserId();
    List<User> adminUsers = Try.of(()->APILocator.getRoleAPI().findUsersForRole(APILocator.getRoleAPI().loadCMSAdminRole())).getOrElse(List.of());



    List<String> users =(userId != null)
        ? List.of(event.getUserId())
        : adminUsers.stream().map(u->u.getUserId()).collect(Collectors.toList());

    final String hostId = event.getHostIdentifier();
    final Host host = Try.of(() -> hostAPI.find(hostId, APILocator.systemUser(), false))
        .getOrNull();

    Logger.info(this, "Reloading Shopify API for host: " + host.getHostname());
    ShopifyAPI.api(host).reload();



    Map<String,Object> test= ShopifyAPI.api(host).testConnection();

    String status = (String) test.get("connection");
    boolean success = status.equalsIgnoreCase("Success");


    final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
    String velocityMessage = "Shopify API connection : <b>" + status + "</b><br>\n"
        + ((success) ? "" : test.get("response"));


    MessageSeverity severity =  (success)
        ? MessageSeverity.INFO
        : MessageSeverity.ERROR;



    systemMessageBuilder.setMessage(velocityMessage)
        .setLife(7*1000)
        .setSeverity(severity).create();



    SystemMessageEventUtil.getInstance().pushMessage(systemMessageBuilder.create(), users);




  }

  @Override
  public Comparable<String> getKey() {
    return AppKey.DOT_SHOPIFY_APP_KEY.appValue;
  }

}
