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
package com.arcussmarthome.platform.services.place.handlers;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.capability.attribute.transform.BeanAttributesTransformer;
import com.arcussmarthome.core.dao.AuthorizationGrantDAO;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.notification.Notifications;
import com.arcussmarthome.core.platform.ContextualRequestMessageHandler;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.PlatformServiceAddress;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.model.Person;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.platform.person.InvitationHandlerHelper;
import com.arcussmarthome.security.authz.AuthorizationGrant;

@Singleton
public class AddPersonHandler implements ContextualRequestMessageHandler<Place> {

   private final PersonDAO personDao;
   private final AuthorizationGrantDAO grantDao;
   private final BeanAttributesTransformer<Person> personTransformer;
   private final PlatformMessageBus bus;

   @Inject
   public AddPersonHandler(
         PersonDAO personDao,
         AuthorizationGrantDAO grantDao,
         BeanAttributesTransformer<Person> personTranformer,
         PlatformMessageBus bus) {

      this.personDao = personDao;
      this.grantDao = grantDao;
      this.personTransformer = personTranformer;
      this.bus = bus;
   }

   @Override
   public String getMessageType() {
      return PlaceCapability.AddPersonRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      MessageBody body = msg.getValue();
      Map<String,Object> personAttrs = PlaceCapability.AddPersonRequest.getPerson(body);
      Person person = personTransformer.transform(personAttrs);      

      // we need some way to identify the person so make sure that at least one of email, first name or last name is specified
      if(StringUtils.isBlank(person.getFirstName()) &&
         StringUtils.isBlank(person.getLastName()) &&
         StringUtils.isBlank(person.getEmail())) {
         return Errors.missingParam("person:firstName, person:lastName or person:email");
      }
      Person actor = InvitationHandlerHelper.getActorFromMessage(msg, personDao);
      person.setOwner(false);
      person.setAccountId(context.getAccount());
      person.setHasLogin(false);
      person.setCurrPlace(context.getId());
      person = personDao.createPersonWithNoLogin(person);
      createGrants(person, context);
      emitAddedEvent(person, context, actor.getId());
      sendNotifications(person, context, actor);
      return PlaceCapability.AddPersonResponse.builder().withNewPerson(person.getAddress()).build();

   }

   private void sendNotifications(Person personAdded, Place context, Person actor) {
	   if(actor != null && StringUtils.isNoneBlank(personAdded.getEmail())) {
	      InvitationHandlerHelper.sendEmailNotification(bus, personAdded.getId().toString(), context.getId().toString(), context.getPopulation(), Notifications.HobbitAdded.KEY,
	            ImmutableMap.<String, String> of(Notifications.HobbitAdded.PARAM_INVITER_FIRSTNAME, actor.getFirstName()!=null?actor.getFirstName():"",
	                  Notifications.HobbitAdded.PARAM_INVITER_LASTNAME, actor.getLastName()!=null?actor.getLastName():""));
	   }
   }

private void emitAddedEvent(Person person, Place place, UUID inviterPersonId) {
      MessageBody addedBody = MessageBody.buildMessage(Capability.EVENT_ADDED, personTransformer.transform(person));
      PlatformMessage addedMsg = PlatformMessage.buildBroadcast(addedBody, Address.fromString(person.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .withActor(PlatformServiceAddress.platformService(inviterPersonId, PersonCapability.NAMESPACE))
            .create();
      bus.send(addedMsg);
   }

   private void createGrants(Person person, Place place) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setAccountId(place.getAccount());
      grant.setAccountOwner(false);
      grant.setEntityId(person.getId());
      grant.setPlaceId(place.getId());
      grant.setPlaceName(place.getName());
      grantDao.save(grant);
   }

}

