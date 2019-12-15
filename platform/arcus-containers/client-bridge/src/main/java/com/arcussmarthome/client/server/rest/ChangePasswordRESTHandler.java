/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arcussmarthome.client.server.rest;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.client.Client;
import com.arcussmarthome.bridge.server.client.ClientFactory;
import com.arcussmarthome.bridge.server.http.HttpSender;
import com.arcussmarthome.bridge.server.http.annotation.HttpPost;
import com.arcussmarthome.bridge.server.http.impl.HttpResource;
import com.arcussmarthome.bridge.server.http.impl.auth.AlwaysAllow;
import com.arcussmarthome.bridge.server.netty.BridgeHeaders;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.notification.Notifications;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.io.json.JSON;
import com.arcussmarthome.messages.ClientMessage;
import com.arcussmarthome.messages.ErrorEvent;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.NotificationCapability;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.model.Person;
import com.arcussmarthome.messages.service.PersonService;
import com.arcussmarthome.messages.services.PlatformConstants;

@Singleton
@HttpPost("/person/ChangePassword")
public class ChangePasswordRESTHandler extends HttpResource {
   public static final String EMAIL_NOT_FOUND_ERROR_CODE = "error.changepassword.emailnotfound";   

   private final ClientFactory clientFactory;
   private final PersonDAO personDao;
   private final PlatformMessageBus platformBus;
   
   @Inject
   public ChangePasswordRESTHandler(ClientFactory clientFactory, PersonDAO personDao, BridgeMetrics metrics, AlwaysAllow alwaysAllow, PlatformMessageBus platformBus) {
      super(alwaysAllow, new HttpSender(ChangePasswordRESTHandler.class, metrics));
      this.personDao = personDao;
      this.platformBus  = platformBus;
      this.clientFactory = clientFactory;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest request, ChannelHandlerContext ctx) throws Exception {
      String json = request.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);

      ClientMessage.Builder responseBuilder = ClientMessage.builder().withCorrelationId(clientMessage.getCorrelationId()).withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE).getRepresentation());
      Pair<MessageBody, HttpResponseStatus> responseTuple;

      try {
         responseTuple = handleChangePassword(clientMessage, ctx);
      } catch (Exception e) {
         responseTuple = new ImmutablePair<MessageBody, HttpResponseStatus>(Errors.fromException(e), HttpResponseStatus.INTERNAL_SERVER_ERROR);
      }

      ClientMessage responseMessage = responseBuilder.withPayload(responseTuple.getLeft()).create();
      FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseTuple.getRight(), Unpooled.copiedBuffer(JSON.toJson(responseMessage), CharsetUtil.UTF_8));
      httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, BridgeHeaders.CONTENT_TYPE_JSON_UTF8);
      return httpResponse;
   }

   private Pair<MessageBody, HttpResponseStatus> handleChangePassword(ClientMessage clientMessage, ChannelHandlerContext ctx) throws Exception {

      String currentPassword = PersonService.ChangePasswordRequest.getCurrentPassword(clientMessage.getPayload());
      String newPassword = PersonService.ChangePasswordRequest.getNewPassword(clientMessage.getPayload());
      String emailAddress = PersonService.ChangePasswordRequest.getEmailAddress(clientMessage.getPayload());

      MessageBody responseBody;
      HttpResponseStatus status;

      Client client =  clientFactory.get(ctx.channel());

      if(StringUtils.isBlank(emailAddress) && client!=null && client.isAuthenticated()){
         emailAddress=client.getPrincipalName();
      }
      
      Person person = personDao.findByEmail(emailAddress);
      
      if (StringUtils.isBlank(emailAddress) || person==null) {
         responseBody = ErrorEvent.fromCode(EMAIL_NOT_FOUND_ERROR_CODE, "Email address is required for operation");
         status = HttpResponseStatus.BAD_REQUEST;
      } else {
         boolean success = personDao.updatePassword(emailAddress, currentPassword, newPassword);
         if(success){
            notify(person, client);
            if(client != null) { client.getAuthorizationContext(true); }
         }
         responseBody = PersonService.ChangePasswordResponse.builder().withSuccess(success).build();
         status = success?HttpResponseStatus.OK:HttpResponseStatus.BAD_REQUEST;
      }
      return new ImmutablePair<MessageBody, HttpResponseStatus>(responseBody, status);
   }
   
   private void notify(Person person, Client client) {

      // internal message to all client bridges to boot active sessions for this user now that there password
      // has changed
      platformBus.send(
         PlatformMessage.buildBroadcast(
            PersonCapability.PasswordChangedEvent.builder().withSession(client == null ? null : client.getSessionId()).build(),
            Address.fromString(person.getAddress())
         )
         .create()
      );

      PlatformMessage msg = Notifications.builder()
                              .withPersonId(person.getId())
                              .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE))
                              .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
                              .withMsgKey(Notifications.PasswordChanged.KEY)
                              .create();
      platformBus.send(msg);
   }
}

