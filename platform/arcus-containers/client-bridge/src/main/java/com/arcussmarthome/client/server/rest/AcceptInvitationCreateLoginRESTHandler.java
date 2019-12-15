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

import java.util.Map;
import java.util.UUID;

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
import com.arcussmarthome.capability.attribute.transform.BeanAttributesTransformer;
import com.arcussmarthome.core.dao.AuthorizationGrantDAO;
import com.arcussmarthome.core.dao.InvitationDAO;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.exception.EmailInUseException;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.ClientMessage;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.model.Person;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.service.InvitationService;
import com.arcussmarthome.messages.type.Invitation;
import com.arcussmarthome.platform.person.InvitationHandlerHelper;

import io.netty.handler.codec.http.HttpResponseStatus;

@Singleton
@HttpPost("/invite/AcceptInvitationCreateLogin")
public class AcceptInvitationCreateLoginRESTHandler extends RESTHandler {

   private final InvitationDAO invitationDao;
   private final PersonDAO personDao;
   private final PlaceDAO placeDao;
   private final AuthorizationGrantDAO authGrantDao;
   private final BeanAttributesTransformer<Person> personTransformer;
   private final PlatformMessageBus bus;

   @Inject
   public AcceptInvitationCreateLoginRESTHandler(
         AlwaysAllow alwaysAllow,
         BridgeMetrics metrics,
         InvitationDAO invitationDao,
         PersonDAO personDao,
         PlaceDAO placeDao,
         AuthorizationGrantDAO authGrantDao,
         BeanAttributesTransformer<Person> personTransformer,
         PlatformMessageBus bus,
         RESTHandlerConfig restHandlerConfig) {

      super(alwaysAllow, new HttpSender(AcceptInvitationCreateLoginRESTHandler.class, metrics),restHandlerConfig);
      this.invitationDao = invitationDao;
      this.personDao = personDao;
      this.placeDao = placeDao;
      this.authGrantDao = authGrantDao;
      this.personTransformer = personTransformer;
      this.bus = bus;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request) throws Exception {
      MessageBody payload = request.getPayload();
      validate(payload);

      String code = InvitationService.AcceptInvitationCreateLoginRequest.getCode(payload);
      String inviteeEmail = InvitationService.AcceptInvitationCreateLoginRequest.getInviteeEmail(payload);

      Invitation invite = invitationDao.find(code);

      if(invite == null || !Objects.equal(invite.getInviteeEmail(), StringUtils.lowerCase(inviteeEmail))) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "invitation " + code + " for " + inviteeEmail + " not found");
      }

      Place place = placeDao.findById(UUID.fromString(invite.getPlaceId()));

      if(place == null) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "place " + invite.getPlaceId() + " not found");
      }

      Person person = personTransformer.transform(InvitationService.AcceptInvitationCreateLoginRequest.getPerson(payload));
      person.setHasLogin(true);
      person.setOwner(false);
      person = personDao.create(person, InvitationService.AcceptInvitationCreateLoginRequest.getPassword(payload));

      InvitationHandlerHelper.createGrantsUponAcceptingInvitation(authGrantDao, person, place);
      invitationDao.accept(invite.getCode(), person.getId());

      InvitationHandlerHelper.emitPersonAddedEvent(bus, personTransformer, person, place);
      InvitationHandlerHelper.sendInvitationAcceptedNotifications(bus, invite, place.getPopulation());
      MessageBody response = InvitationService.AcceptInvitationCreateLoginResponse.builder().withPerson(personTransformer.transform(person)).build();
      return response;
   }
   

   @Override
	protected HttpResponseStatus overrideErrorResponseStatus(Throwable error) {
		if(error instanceof EmailInUseException) {
			return HttpResponseStatus.CONFLICT;
		}
		return super.overrideErrorResponseStatus(error);
	}

	private void validate(MessageBody body) {
      Map<String,Object> person = InvitationService.AcceptInvitationCreateLoginRequest.getPerson(body);
      Errors.assertRequiredParam(person, "person");
      Errors.assertRequiredParam(InvitationService.AcceptInvitationCreateLoginRequest.getCode(body), "invitationCode");
      Errors.assertRequiredParam(InvitationService.AcceptInvitationCreateLoginRequest.getInviteeEmail(body), "inviteeEmail");
      Errors.assertRequiredParam(InvitationService.AcceptInvitationCreateLoginRequest.getPassword(body), "password");
      Errors.assertRequiredParam(person.get(PersonCapability.ATTR_EMAIL), "pers:email");
   }
}

