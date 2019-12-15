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
package com.arcussmarthome.platform.alarm.history;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.capability.attribute.transform.BeanAttributesTransformer;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.address.PlatformServiceAddress;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.capability.RuleCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.Person;
import com.arcussmarthome.messages.model.SimpleModel;
import com.arcussmarthome.platform.rule.RuleDao;
import com.arcussmarthome.platform.rule.RuleDefinition;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

// TODO:  back by objectcache?
@Singleton
public class ModelLoader {

   private final DeviceDAO deviceDao;
   private final RuleDao ruleDao;
   private final PersonDAO personDao;
   private final BeanAttributesTransformer<Person> personTxfm;

   @Inject
   public ModelLoader(DeviceDAO deviceDao, RuleDao ruleDao, PersonDAO personDao, BeanAttributesTransformer<Person> personTxfm) {
      this.deviceDao = deviceDao;
      this.ruleDao = ruleDao;
      this.personDao = personDao;
      this.personTxfm = personTxfm;
   }

   public Model findByAddress(String address) {
      if(StringUtils.isBlank(address)) {
         return null;
      }
      return findByAddress(Address.fromString(address));
   }

   public Model findByAddress(Address address) {
      String grp = (String) address.getGroup();
      switch(grp) {
         case DeviceCapability.NAMESPACE:
            return findDevice((UUID) address.getId());
         case RuleCapability.NAMESPACE:
            return findRule(address);
         case PersonCapability.NAMESPACE:
            return findPerson((UUID) address.getId());
         default:
            return null;
      }
   }

   private Model findDevice(UUID id) {
      return deviceDao.modelById(id);
   }

   private Model findPerson(UUID id) {
      Person p = personDao.findById(id);
      if(p == null) {
         return null;
      }
      return new SimpleModel(personTxfm.transform(p));
   }

   private Model findRule(Address address) {
      if(!(address instanceof PlatformServiceAddress)) {
         return null;
      }
      PlatformServiceAddress addr = (PlatformServiceAddress) address;
      RuleDefinition def = ruleDao.findById((UUID) addr.getContextId(), addr.getContextQualifier());
      if(def == null) {
         return null;
      }
      return new SimpleModel(ImmutableMap.of(
            Capability.ATTR_ADDRESS, def.getAddress(),
            Capability.ATTR_CAPS, def.getCaps(),
            Capability.ATTR_ID, def.getId().getRepresentation(),
            Capability.ATTR_TYPE, RuleCapability.NAMESPACE,
            RuleCapability.ATTR_NAME, def.getName()));
   }


}

