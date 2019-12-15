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
package com.arcussmarthome.platform.services.account;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.core.dao.AccountDAO;
import com.arcussmarthome.core.platform.ContextualEventMessageHandler;
import com.arcussmarthome.core.platform.ContextualPlatformMessageDispatcher;
import com.arcussmarthome.core.platform.ContextualRequestMessageHandler;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.core.platform.PlatformService;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.model.Account;
import com.arcussmarthome.messages.services.PlatformConstants;

@Singleton
public class AccountService extends ContextualPlatformMessageDispatcher<Account> implements PlatformService {
   public static final String PROP_THREADPOOL = "platform.service.account.threadpool";

   private static final Address address = Address.platformService(PlatformConstants.SERVICE_ACCOUNTS);

   private final AccountDAO accountDao;

   @Inject
   public AccountService(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor, 
         Set<ContextualRequestMessageHandler<Account>> handlers,
         Set<ContextualEventMessageHandler<Account>> eventHandlers, 
         AccountDAO accountDao
   ) {
      super(platformBus, executor, handlers, eventHandlers);
      this.accountDao = accountDao;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

  @Override
   protected Account loadContext(Object contextId, Integer qualifier) {
      if(!(contextId instanceof UUID)) {
         throw new IllegalArgumentException("The context ID must be a UUID");
      }
      return accountDao.findById((UUID) contextId);
   }
}

