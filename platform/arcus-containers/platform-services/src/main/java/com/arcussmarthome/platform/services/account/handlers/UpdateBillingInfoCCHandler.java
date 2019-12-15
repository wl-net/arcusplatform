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
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.billing.client.BillingClient;
import com.arcussmarthome.billing.client.model.BillingInfo;
import com.arcussmarthome.billing.client.model.RecurlyError;
import com.arcussmarthome.billing.client.model.request.AccountRequest;
import com.arcussmarthome.billing.exception.RecurlyAPIErrorException;
import com.arcussmarthome.billing.exception.TransactionErrorException;
import com.arcussmarthome.core.dao.AccountDAO;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.notification.Notifications;
import com.arcussmarthome.core.platform.ContextualRequestMessageHandler;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AccountCapability;
import com.arcussmarthome.messages.capability.NotificationCapability;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.model.Account;
import com.arcussmarthome.messages.model.Person;
import com.arcussmarthome.platform.services.account.billing.BillingErrorCodes;

@Singleton
public class UpdateBillingInfoCCHandler implements ContextualRequestMessageHandler<Account>, BillingErrorCodes {

   private final AccountDAO accountDao;
   private final PersonDAO personDao;
   private final BillingClient client;
   private final PlatformMessageBus platformBus;

   @Inject(optional = true)
   @Named(value = "billing.timeout")
   private int billingTimeout = 30;

   @Inject
   public UpdateBillingInfoCCHandler(
         AccountDAO accountDao,
         PersonDAO personDao,
         BillingClient client,
         PlatformMessageBus platformBus) {

      this.accountDao = accountDao;
      this.personDao = personDao;
      this.client = client;
      this.platformBus = platformBus;
   }

   @Override
   public String getMessageType() {
      return AccountCapability.UpdateBillingInfoCCRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(final Account context, PlatformMessage msg) {
   	if (context == null) {
      	return Errors.fromCode(MISSING_ACCOUNT_ERR, MISSING_ACCOUNT_MSG);
      }

   	MessageBody request = msg.getValue();

      String token = AccountCapability.UpdateBillingInfoCCRequest.getBillingToken(request);
      if (StringUtils.isBlank(token)) {
      	return Errors.fromCode(MISSING_BILLING_TOKEN_ERR, MISSING_BILLING_TOKEN_MSG);
      }

      try {
         if(!context.hasBillingAccount()) {
            createBilling(context, token);
         } else {
            updateBilling(context, token);
         }

         notification(context.getOwner());

         return AccountCapability.UpdateBillingInfoCCResponse.instance();

      } catch(Exception ex) {
         if (ex.getCause() instanceof RecurlyAPIErrorException) {

            RecurlyError e = ((RecurlyAPIErrorException) ex.getCause()).getErrors().get(0);
            return Errors.fromCode(e.getErrorSymbol(), e.getErrorText());
         } else if (ex.getCause() instanceof TransactionErrorException) {

            TransactionErrorException e = (TransactionErrorException) ex.getCause();
            return Errors.fromCode(e.getErrorCode(), e.getCustomerMessage());
         } else {
            return Errors.fromException(ex);
         }
      }
   }

   private void createBilling(Account context, String token) throws Exception {
      Person person = personDao.findById(context.getOwner());

      if(person == null) {
         throw new ErrorEventException(MISSING_ACCOUNT_OWNER_CODE, MISSING_ACCOUNT_OWNER_MSG);
      }

      AccountRequest accountRequest = new AccountRequest();
      accountRequest.setAccountID(context.getId().toString());
      accountRequest.setEmail(person.getEmail());
      accountRequest.setBillingTokenID(token);

      client.createAccount(accountRequest).get(billingTimeout, TimeUnit.SECONDS);

      BillingInfo billingInfo =
            client.getBillingInfoForAccount(context.getId().toString())
                     .get(billingTimeout, TimeUnit.SECONDS);

      updateLocal(context, billingInfo);
      accountDao.save(context);
   }

   private void updateBilling(Account context, String token) throws Exception {
      BillingInfo billingInfo =
            client.createOrUpdateBillingInfoForAccount(context.getId().toString(), token)
            .get(billingTimeout, TimeUnit.SECONDS);
      updateLocal(context, billingInfo);
      accountDao.save(context);
   }

   private void updateLocal(Account context, BillingInfo billingInfo) {
      context.setBillingCCLast4(billingInfo.getLastFour());
      context.setBillingCCType(billingInfo.getCardType());
      context.setBillingFirstName(billingInfo.getFirstName());
      context.setBillingLastName(billingInfo.getLastName());
      context.setBillingStreet1(billingInfo.getAddress1());
      context.setBillingStreet2(billingInfo.getAddress2());
      context.setBillingCity(billingInfo.getCity());
      context.setBillingState(billingInfo.getState());
      context.setBillingZip(billingInfo.getZip());

      accountDao.save(context);
   }

   private void notification(UUID personId) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(personId)
            .withSource(Address.platformService(AccountCapability.NAMESPACE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.CCUpdated.KEY)
            .create();
      platformBus.send(msg);
   }
}

