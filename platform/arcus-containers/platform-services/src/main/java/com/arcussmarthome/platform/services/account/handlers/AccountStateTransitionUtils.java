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
package com.arcussmarthome.platform.services.account.handlers;

import java.util.UUID;

import com.arcussmarthome.core.notification.Notifications;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AccountCapability;
import com.arcussmarthome.messages.capability.NotificationCapability;

public class AccountStateTransitionUtils
{
   public static void sendAccountCreatedNotification(UUID personId, PlatformMessageBus platformBus) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(personId)
            .withSource(Address.platformService(AccountCapability.NAMESPACE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.AccountCreated.KEY)
            .create();
      platformBus.send(msg);
   }
}

