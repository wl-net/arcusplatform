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
package com.arcussmarthome.platform.rule.catalog.function;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AccountCapability;
import com.arcussmarthome.messages.capability.PersonCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.ModelStore;
import com.arcussmarthome.messages.model.serv.AccountModel;
import com.arcussmarthome.model.predicate.Predicates;

/**
 * 
 */
public class GetAccountOwnerQuery implements Function<ModelStore, Address> {
   private final Predicate<Model> isAccount =
         Predicates.isA(AccountCapability.NAMESPACE);
   
   @Override
   public Address apply(ModelStore store) {
      Model account = Iterables.tryFind(store.getModels(), isAccount).orNull();
      if(account == null) {
         return null;
      }
      String owner = AccountModel.getOwner(account);
      if(owner == null) {
         return null;
      }
      return Address.platformService(owner, PersonCapability.NAMESPACE);
   }

}

