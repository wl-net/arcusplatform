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

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.config.RESTHandlerConfig;
import com.arcussmarthome.bridge.server.http.HttpSender;
import com.arcussmarthome.bridge.server.http.annotation.HttpPost;
import com.arcussmarthome.bridge.server.http.handlers.RESTHandler;
import com.arcussmarthome.bridge.server.http.impl.auth.AlwaysAllow;
import com.arcussmarthome.core.dao.InvitationDAO;
import com.arcussmarthome.messages.ClientMessage;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.service.InvitationService;
import com.arcussmarthome.messages.type.Invitation;

@Singleton
@HttpPost("/invite/GetInvitation")
public class GetInvitationRESTHandler extends RESTHandler {

   private final InvitationDAO invitationDao;

   @Inject
   public GetInvitationRESTHandler(
         AlwaysAllow alwaysAllow,
         BridgeMetrics metrics,
         InvitationDAO invitationDao,
         RESTHandlerConfig restHandlerConfig) {

      super(alwaysAllow, new HttpSender(GetInvitationRESTHandler.class, metrics),restHandlerConfig);
      this.invitationDao = invitationDao;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request) throws Exception {
      MessageBody payload = request.getPayload();
      validate(payload);

      String code = InvitationService.GetInvitationRequest.getCode(payload);
      String inviteeEmail = InvitationService.GetInvitationRequest.getInviteeEmail(payload);

      Invitation invite = invitationDao.find(code);

      if(invite == null ||
            !Objects.equal(invite.getInviteeEmail(), StringUtils.lowerCase(inviteeEmail)) ||
            invite.getAccepted() != null ||
            invite.getRejected() != null) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "invitation " + code + " for " + inviteeEmail + " not found");
      }

      MessageBody response = InvitationService.GetInvitationResponse.builder().withInvitation(invite.toMap()).build();
      return response;
   }

   private void validate(MessageBody body) {
      Errors.assertRequiredParam(InvitationService.GetInvitationRequest.getCode(body), "invitationCode");
      Errors.assertRequiredParam(InvitationService.GetInvitationRequest.getInviteeEmail(body), "inviteeEmail");
   }
}

