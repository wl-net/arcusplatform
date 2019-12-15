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
package com.arcussmarthome.client.impl;

import java.util.Map;

import com.google.common.base.Function;
import com.arcussmarthome.capability.util.Addresses;
import com.arcussmarthome.client.ClientEvent;
import com.arcussmarthome.client.IrisClient;
import com.arcussmarthome.client.event.ClientFuture;
import com.arcussmarthome.client.event.Futures;
import com.arcussmarthome.client.service.InvitationService;

public class InvitationServiceImpl implements InvitationService {

   private static final String ADDRESS = Addresses.toServiceAddress(InvitationService.NAMESPACE);
   private IrisClient client;

   public InvitationServiceImpl(IrisClient client) {
      this.client = client;
   }

   @Override
   public String getName() {
      return InvitationService.NAME;
   }

   @Override
   public String getAddress() {
      return ADDRESS;
   }

   @Override
   public ClientFuture<GetInvitationResponse> getInvitation(
         String invitationCode, String inviteeEmail) {

      GetInvitationRequest request = new GetInvitationRequest();
      request.setCode(invitationCode);
      request.setInviteeEmail(inviteeEmail);
      request.setAddress(getAddress());
      request.setRestfulRequest(true);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, GetInvitationResponse>() {
         @Override
         public GetInvitationResponse apply(ClientEvent input) {
            GetInvitationResponse response = new GetInvitationResponse(input);
            return response;
         }
      });
   }



   @Override
   public ClientFuture<AcceptInvitationCreateLoginResponse> acceptInvitationCreateLogin(
         Map<String, Object> person, String password, String code,
         String inviteeEmail) {

      AcceptInvitationCreateLoginRequest request = new AcceptInvitationCreateLoginRequest();
      request.setCode(code);
      request.setInviteeEmail(inviteeEmail);
      request.setPassword(password);
      request.setPerson(person);
      request.setAddress(getAddress());
      request.setRestfulRequest(true);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, AcceptInvitationCreateLoginResponse>() {
         @Override
         public AcceptInvitationCreateLoginResponse apply(ClientEvent input) {
            AcceptInvitationCreateLoginResponse response = new AcceptInvitationCreateLoginResponse(input);
            return response;
         }
      });
   }
}

