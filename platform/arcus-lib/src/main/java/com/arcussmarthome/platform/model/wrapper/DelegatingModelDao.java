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
/**
 *
 */
package com.arcussmarthome.platform.model.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.arcussmarthome.core.dao.AccountDAO;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.decorators.PersonSource;
import com.arcussmarthome.messages.capability.AccountCapability;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.HubCapability;
import com.arcussmarthome.messages.capability.PairingDeviceCapability;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.capability.SceneCapability;
import com.arcussmarthome.messages.capability.SchedulerCapability;
import com.arcussmarthome.messages.capability.SubsystemCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.SimpleModel;
import com.arcussmarthome.messages.model.serv.PlaceModel;
import com.arcussmarthome.platform.model.ModelDao;
import com.arcussmarthome.platform.model.ModelEntity;
import com.arcussmarthome.platform.model.ModelProvider;
import com.arcussmarthome.platform.pairing.PairingDeviceDao;
import com.arcussmarthome.platform.scene.SceneDao;
import com.arcussmarthome.platform.scheduler.SchedulerModelDao;
import com.arcussmarthome.platform.subsystem.SubsystemDao;

/**
 *
 */
public class DelegatingModelDao implements ModelDao {
   private static final Logger logger = LoggerFactory.getLogger(DelegatingModelDao.class);
   
   private final PlaceDAO placeDao;
   private final Map<String, ModelProvider> modelProviders;

   /**
    *
    */
   @Inject
   public DelegatingModelDao(
         AccountDAO accountDao,
         DeviceDAO deviceDao,
         HubDAO hubDao,
         PairingDeviceDao pairingDeviceDao,
         PersonSource personDao,
         PlaceDAO placeDao,
         SceneDao sceneDao,
         SchedulerModelDao schedulerModelDao,
         SubsystemDao subsystemDao
   ) {
      this(
            placeDao,
            ImmutableMap
               .<String, ModelProvider>builder()
               .put(PlaceCapability.NAMESPACE, (placeId, place) -> ImmutableList.of(place))
               .put(AccountCapability.NAMESPACE, wrap(accountDao))
               .put(DeviceCapability.NAMESPACE, wrap(deviceDao))
               .put(HubCapability.NAMESPACE, wrap(hubDao))
               .put(PairingDeviceCapability.NAMESPACE, wrap(pairingDeviceDao))
               .put(PersonCapability.NAMESPACE, wrap(personDao))
               .put(SceneCapability.NAMESPACE, wrap(sceneDao))
               .put(SchedulerCapability.NAMESPACE, wrap(schedulerModelDao))
               .put(SubsystemCapability.NAMESPACE, wrap(subsystemDao))
               .build()
      );
   }
   
   // FIXME have the other DAOs bind to map keys in order to support this
   public DelegatingModelDao(
         PlaceDAO placeDao,
         Map<String, ModelProvider> modelProviders
   ) {
      this.placeDao = placeDao;
      this.modelProviders = ImmutableMap.copyOf(modelProviders);
   }
   
   @Override
   public Collection<Model> loadModelsByPlace(UUID placeId, Set<String> namespaces) {
      Preconditions.checkArgument(modelProviders.keySet().containsAll(namespaces), "Unsupported namespaces");

      ModelEntity placeModel = placeDao.findPlaceModelById(placeId);
      if(placeModel == null) {
         throw new RuntimeException("Place " + placeId + " does not exist");
      }
      List<Model> entities = new ArrayList<>();
      if(namespaces.contains(PlaceCapability.NAMESPACE)) {
         entities.add(placeModel);
      }
      for(String namespace: namespaces) {
         entities.addAll(modelProviders.get(namespace).findForPlace(placeId, placeModel));
      }

      return entities;
   }
   
   private static ModelProvider wrap(AccountDAO accountDao) {
      return (placeId, place) -> {
         UUID accountId = UUID.fromString(PlaceModel.getAccount(place));
         Model account = accountDao.findAccountModelById(accountId);
         if(account != null) {
            return ImmutableList.of(account);
         }
         else {
            logger.warn("Unable to load account [{}] associated with place [{}]", accountId, placeId);
            return ImmutableList.of();
         }
      };
   }

   private static ModelProvider wrap(DeviceDAO deviceDao) {
      return (placeId, place) -> 
         deviceDao
            .streamDeviceModelByPlaceId(placeId)
            .collect(Collectors.toList());
   }

   private static ModelProvider wrap(HubDAO hubDao) {
      return (placeId, place) -> 
         Optional
            .ofNullable( hubDao.findHubModelForPlace(placeId) )
            .map(ImmutableList::of)
            .orElse(ImmutableList.of());
   }

   private static ModelProvider wrap(PairingDeviceDao pairingDeviceDao) {
      return (placeId, place) -> pairingDeviceDao.listByPlace(placeId);
   }

   private static ModelProvider wrap(PersonSource personDao) {
      return (placeId, place) -> 
         personDao
            .listAttributesByPlace(placeId, false)
            .stream()
            .map(SimpleModel::new)
            .collect(Collectors.toList());
   }

   private static ModelProvider wrap(SceneDao sceneDao) {
      return (placeId, place) -> sceneDao.listModelsByPlace(placeId);
   }

   private static ModelProvider wrap(SchedulerModelDao schedulerModelDao) {
      return (placeId, place) -> 
         Optional
            .ofNullable( schedulerModelDao.listByPlace(placeId, false) )
            .orElse(ImmutableList.of());
   }

   private static ModelProvider wrap(SubsystemDao subsystemDao) {
      return (placeId, place) -> subsystemDao.listByPlace(placeId);
   }

}

