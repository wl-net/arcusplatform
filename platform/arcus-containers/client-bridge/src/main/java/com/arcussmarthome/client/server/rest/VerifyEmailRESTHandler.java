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

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.config.RESTHandlerConfig;
import com.arcussmarthome.bridge.server.http.HttpSender;
import com.arcussmarthome.bridge.server.http.annotation.HttpPost;
import com.arcussmarthome.bridge.server.http.handlers.RESTHandler;
import com.arcussmarthome.bridge.server.http.impl.auth.SessionAuth;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.dao.PersonPlaceAssocDAO;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.ClientMessage;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.model.Person;
import com.arcussmarthome.platform.person.PersonHandlerHelper;
import com.arcussmarthome.population.PlacePopulationCacheManager;

@Singleton
@HttpPost("/" + PersonCapability.NAMESPACE + "/VerifyEmail")
public class VerifyEmailRESTHandler extends RESTHandler {
	private static final Logger logger = LoggerFactory.getLogger(VerifyEmailRESTHandler.class);
	
   private final PersonDAO personDao;
   private final PlatformMessageBus platformBus;
   private final PersonPlaceAssocDAO personPlaceAssocDao;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public VerifyEmailRESTHandler(
         SessionAuth auth,
         BridgeMetrics metrics,
         PersonDAO personDao,
         PersonPlaceAssocDAO personPlaceAssocDao,
         PlatformMessageBus platformBus,
         RESTHandlerConfig restHandlerConfig,
         PlacePopulationCacheManager populationCacheMgr) {

      super(auth, new HttpSender(VerifyEmailRESTHandler.class, metrics),restHandlerConfig);
      this.personDao = personDao;
      this.platformBus = platformBus;
      this.personPlaceAssocDao = personPlaceAssocDao;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request) throws Exception {
   	//ensure person exists
   	String destinationStr = request.getDestination();
   	Person curPerson = loadPerson(destinationStr);
   	if(curPerson == null) {
   		throw new ErrorEventException(Errors.CODE_UNSUPPORTED_DESTINATION, "person [" + destinationStr + "] is not found. ");
   	}
   	
   	//verify token in the request matches what is in the Person entity
   	MessageBody msgBody = request.getPayload();
   	String curToken = PersonCapability.VerifyEmailRequest.getToken(msgBody);
   	if(StringUtils.isBlank(curToken) || !curToken.equals(curPerson.getEmailVerificationToken())) {
   		throw new ErrorEventException(PersonCapability.VerifyEmailResponse.CODE_TOKEN_INVALID, "The token does not match the current email address.");
   	}
   	
   	//update emailVerified field and send value change event for each place this person belongs to.
   	if(curPerson.getEmailVerified() == null) {
	   	curPerson.setEmailVerified(new Date());
	   	personDao.update(curPerson);
	   	if(PersonHandlerHelper.getInstance().sendPersonValueChangesToPlaces(personPlaceAssocDao, platformBus, populationCacheMgr, Address.fromString(request.getDestination()), curPerson.getId(), ImmutableMap.<String, Object>of(PersonCapability.ATTR_EMAILVERIFIED, Boolean.TRUE))){
   			//ok
   		}else{
	         logger.warn("This should not happen, but person [{}] has no places associated with it.  Possibly index out of sync", curPerson.getId());   			
   		}
   	}

      return PersonCapability.VerifyEmailResponse.instance();
   }

	private Person loadPerson(String destination) {
      Address addr = Address.fromString(destination);
      if(addr == null) {
         return null;
      }
      return personDao.findById((UUID) addr.getId());
   }

}

