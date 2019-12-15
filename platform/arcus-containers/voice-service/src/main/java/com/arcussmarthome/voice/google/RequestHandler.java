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
package com.arcussmarthome.voice.google;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.google.Constants.Error;
import com.arcussmarthome.google.Predicates;
import com.arcussmarthome.google.Transformers;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.service.GoogleService;
import com.arcussmarthome.messages.service.VoiceService;
import com.arcussmarthome.messages.type.GoogleCommand;
import com.arcussmarthome.messages.type.GoogleCommandResult;
import com.arcussmarthome.messages.type.GoogleDevice;
import com.arcussmarthome.prodcat.ProductCatalogManager;
import com.arcussmarthome.voice.VoiceConfig;
import com.arcussmarthome.voice.VoiceUtil;
import com.arcussmarthome.voice.context.VoiceContext;
import com.arcussmarthome.voice.google.homegraph.HomeGraphAPI;
import com.arcussmarthome.voice.proactive.ProactiveCreds;
import com.arcussmarthome.voice.proactive.ProactiveCredsDAO;

@Singleton
public class RequestHandler {

   private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

   private final PlatformMessageBus bus;
   private final ExecutorService executor;
   private final GoogleCommandExecutor commandExecutor;
   private final GoogleWhitelist whitelist;
   private final ProductCatalogManager prodCat;
   private final ProactiveCredsDAO proactiveCredsDAO;
   private final HomeGraphAPI homegraph;
   private final GoogleConfig config;

   @Inject
   public RequestHandler(
      PlatformMessageBus bus,
      @Named(VoiceConfig.NAME_EXECUTOR) ExecutorService executor,
      GoogleCommandExecutor commandExecutor,
      GoogleWhitelist whitelist,
      ProductCatalogManager prodCat,
      ProactiveCredsDAO proactiveCredsDAO,
      HomeGraphAPI homegraph,
      GoogleConfig config
   ) {
      this.bus = bus;
      this.executor = executor;
      this.commandExecutor = commandExecutor;
      this.whitelist = whitelist;
      this.prodCat = prodCat;
      this.proactiveCredsDAO = proactiveCredsDAO;
      this.homegraph = homegraph;
      this.config = config;
   }

   @Request(value = GoogleService.SyncRequest.NAME, service = true)
   public MessageBody handleSync(VoiceContext context) {
      long startTime = System.nanoTime();
      try {
         Optional<ProactiveCreds> optCreds = context.getProactiveCreds(VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE);
         if(!optCreds.isPresent()) {
            ProactiveCreds creds = new ProactiveCreds(context.getPlaceId().toString());
            proactiveCredsDAO.upsert(context.getPlaceId(), VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE, creds);
            context.updateProactiveCreds(VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE, creds);
            optCreds = Optional.of(creds);
         }
         boolean whitelisted = whitelist.isWhitelisted(context.getPlaceId());
         MessageBody body = GoogleService.SyncResponse.builder()
            .withDevices(
               context.streamSupported(
                  model -> Predicates.isSupportedModel(model, whitelisted, VoiceUtil.getProduct(prodCat, model)),
                  model -> Transformers.modelToDevice(model, whitelisted, VoiceUtil.getProduct(prodCat, model), this.config.isReportStateEnabled())
               )
               .map(GoogleDevice::toMap)
               .collect(Collectors.toList())
            )
            .withUserAgentId(optCreds.get().getAccess())
            .build();
         GoogleMetrics.timeHandlerSuccess(GoogleService.SyncRequest.NAME, startTime);
         this.homegraph.sendDelayedReportState(context); // We're sending a SYNC response, so we are required to send a Report State post.  It must be delayed to give the SYNC a chance to succeed.
         return body;
      } catch(RuntimeException e) {
         GoogleMetrics.timeHandlerFailure(GoogleService.SyncRequest.NAME, startTime);
         throw e;
      }
   }

   @Request(value = GoogleService.QueryRequest.NAME, service = true)
   public MessageBody handleQuery(
      VoiceContext context,
      @Named(GoogleService.QueryRequest.ATTR_ADDRESSES) Set<String> devIds
   ) {
      long startTime = System.nanoTime();
      try {
         MessageBody body = GoogleService.QueryResponse.builder()
            .withDevices(
               context.query(
                  devIds,
                  (model, hubOffline) -> Transformers.modelToStateMap(model, hubOffline, whitelist.isWhitelisted(context.getPlaceId()), VoiceUtil.getProduct(prodCat, model))
               )
            )
            .build();
         GoogleMetrics.timeHandlerSuccess(GoogleService.QueryRequest.NAME, startTime);
         return body;
      } catch(RuntimeException e) {
         GoogleMetrics.timeHandlerFailure(GoogleService.QueryRequest.NAME, startTime);
         throw e;
      }
   }

   @Request(value = GoogleService.ExecuteRequest.NAME, service = true, response = false)
   public void handleExecute(
      VoiceContext context,
      PlatformMessage msg,
      @Named(GoogleService.ExecuteRequest.ATTR_COMMANDS) List<Map<String,Object>> commands
   ) {
      long startTime = System.nanoTime();
      if(commands == null || commands.isEmpty()) {
         bus.send(response(ImmutableList.of(), msg));
         GoogleMetrics.timeHandlerSuccess(GoogleService.ExecuteRequest.NAME, startTime);
         return;
      }

      Futures.addCallback(
            commandExecutor.execute(context, commands.stream().map(GoogleCommand::new).collect(Collectors.toList())),
            new FutureCallback<List<GoogleCommandResult>>() {
               @Override
               public void onSuccess(List<GoogleCommandResult> result) {
                  GoogleMetrics.timeHandlerSuccess(GoogleService.ExecuteRequest.NAME, startTime);
                  bus.send(response(result, msg));
               }

               @Override
               public void onFailure(@NonNull Throwable t) {
                  logger.warn("execution request [{}] failed", msg, t);
                  Throwable c = t.getCause();
                  if(c == null) {
                     c = t;
                  }
                  if(c instanceof TimeoutException) {
                     bus.send(error(Error.TIMEOUT, "execution timed out", msg));
                  } else {
                     bus.send(error(Error.UNKNOWN_ERROR, c.getMessage(), msg));
                  }
                  GoogleMetrics.timeHandlerFailure(GoogleService.ExecuteRequest.NAME, startTime);
               }
            },
            executor
      );
   }

   private PlatformMessage response(List<GoogleCommandResult> results, PlatformMessage req) {
      MessageBody body = GoogleService.ExecuteResponse.builder().withCommands(results.stream().map(GoogleCommandResult::toMap).collect(Collectors.toList())).build();
      return PlatformMessage.createResponse(req, body);
   }

   private PlatformMessage error(String code, String msg, PlatformMessage req) {
      MessageBody body = Errors.fromCode(code, msg);
      return PlatformMessage.createResponse(req, body);
   }
}

