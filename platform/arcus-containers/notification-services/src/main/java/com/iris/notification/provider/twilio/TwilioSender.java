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
package com.iris.notification.provider.twilio;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.twilio.exception.TwilioException;
import com.twilio.http.HttpMethod;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.model.Person;
import com.iris.notification.dispatch.DispatchUnsupportedByUserException;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.provider.ivr.TwilioHelper;
import com.iris.util.Net;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.CallCreator;
import com.twilio.type.PhoneNumber;

@Singleton
public class TwilioSender {
   private static final Logger LOGGER = LoggerFactory.getLogger(TwilioSender.class);

   private final static String TWILIO_PARAM_KEY_RECORD= "Record";

   private final static String TWILIO_PARAM_KEY_IF_MACHINE= "IfMachine";
   private final static String TWILIO_PARAM_VALUE_TRUE= "True";
   
   private final static String TWILIO_PARAM_KEY_STATUSCALLBACK_EVENT_COMPLETED= "completed";

   private final static String IRIS_ACK_HANDLER_PATH="/ivr/script/ack";
   private final static String IRIS_ACK_EVENT_HANDLER_PATH="/ivr/event/ack";
   private final static String CUSTOM_MESSAGE_PARAM_NAME = "customMessage";
   private final static String SCRIPT_PARAM_NAME = "script";
   
   @Inject @Named("twilio.account.from") private String twilioAccountFrom;
   @Inject @Named("twilio.callback.serverurl") private String twilioCallbackServerUrl;
   @Inject(optional = true) @Named("twilio.applicationSid") private String twilioApplicationSid;
   @Inject(optional = true) @Named("twilio.fallbackUrl") private String twilioFallbackUrl;
   @Inject(optional = true) @Named("twilio.ifmachine") String ifMachine;
   @Inject(optional = true) @Named("twilio.recordCalls") boolean recordCalls=false;
   @Inject(optional = true) @Named("twilio.param.prefix") private String twilioNotificationParamPrefix="_";

   private Twilio twilio;
   
   @Inject
   public TwilioSender(@Named("twilio.account.sid") String twilioAccountSid, @Named("twilio.account.auth") String twilioAccountAuth) {
      Twilio.init(twilioAccountSid, twilioAccountAuth);
   }

   public String sendIVR(Notification notification, Person recipient) throws DispatchUnsupportedByUserException {
      
      Map<String,String>messageParameters=notification.getMessageParams() != null?notification.getMessageParams() : new HashMap<String,String>();
      
      if(notification.isCustomMessage()){
         messageParameters.put(CUSTOM_MESSAGE_PARAM_NAME, notification.getCustomMessage());
      }
      
      String parameters=Net.toQueryString(messageParameters, twilioNotificationParamPrefix);

      String linkback = String.join("&", new ImmutableMap.Builder<String,String>()
            .put(SCRIPT_PARAM_NAME, notification.getMessageKey() != null ? notification.getMessageKey() : "ivr.custom")
            .put(TwilioHelper.NOTIFICATION_ID_PARAM_NAME,notification.getEventIdentifier())
            .put(TwilioHelper.NOTIFICATION_EVENT_TIME_PARAM_NAME,Long.toString(notification.getRxTimestamp().toEpochMilli()))
            .put(TwilioHelper.PERSON_ID_PARAM_NAME,notification.getPersonId().toString())
            .put(TwilioHelper.PLACE_ID_PARAM_NAME, notification.getPlaceId().toString())
            .build().entrySet().stream()
            .map(e->String.format("%s=%s",e.getKey(),Net.urlEncode(e.getValue())))
            .collect(Collectors.toList()));
      
      List<NameValuePair> params = new ArrayList<NameValuePair>();

      String scriptHandlerPath = IRIS_ACK_HANDLER_PATH;

      String eventHandlerPath = IRIS_ACK_EVENT_HANDLER_PATH;

      CallCreator callCreator = Call.creator(new PhoneNumber(recipient.getMobileNumber()),
              new PhoneNumber(twilioAccountFrom),
              twilioApplicationSid
      );

      callCreator.setStatusCallback(format("%s%s?%s", twilioCallbackServerUrl, eventHandlerPath, linkback));
      callCreator.setStatusCallbackMethod(HttpMethod.GET);
      callCreator.setStatusCallbackEvent(TWILIO_PARAM_KEY_STATUSCALLBACK_EVENT_COMPLETED);

      if (recordCalls) {
         callCreator.setRecord(recordCalls);
      }
      
      if (twilioApplicationSid != null) {
         callCreator.setApplicationSid(twilioApplicationSid);
      }
      
      if (twilioFallbackUrl != null) {
         callCreator.setFallbackUrl(twilioFallbackUrl);
      }
      if (ifMachine != null) {
         callCreator.setMachineDetection(ifMachine);
      }

      try {
         Call call = callCreator.create();
         return call.getSid();
      } catch (TwilioException e) {
         LOGGER.error("Error Contacting Twilio",e);
         throw new RuntimeException("unknown twilio exception", e);
      }
   }
}

