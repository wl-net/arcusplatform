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
package com.arcussmarthome.voice.alexa.handlers;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.alexa.AlexaInterfaces;
import com.arcussmarthome.alexa.error.AlexaErrors;
import com.arcussmarthome.alexa.error.AlexaException;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.dev.DevicePowerModel;
import com.arcussmarthome.messages.service.AlexaService;
import com.arcussmarthome.messages.type.AlexaPropertyReport;
import com.arcussmarthome.voice.VoiceConfig;
import com.arcussmarthome.voice.VoicePredicates;
import com.arcussmarthome.voice.alexa.AlexaConfig;
import com.arcussmarthome.voice.alexa.AlexaMetrics;
import com.arcussmarthome.voice.alexa.AlexaPredicates;
import com.arcussmarthome.voice.alexa.reporting.AlexaProactiveReportHandler;
import com.arcussmarthome.voice.alexa.reporting.DeferredCorrelator;
import com.arcussmarthome.voice.alexa.reporting.PropertyReporter;
import com.arcussmarthome.voice.context.VoiceContext;
import com.arcussmarthome.voice.context.VoiceContextExecutorRegistry;
import com.arcussmarthome.voice.context.VoiceDAO;
import com.arcussmarthome.voice.exec.CommandExecutor;

@Singleton
public class ExecuteHandler {

   private static final Logger logger = LoggerFactory.getLogger(ExecuteHandler.class);

   private final VoiceDAO voiceDao;
   private final PlatformMessageBus bus;
   private final CommandExecutor executor;
   private final ExecutorService executorService;
   private final AlexaConfig config;
   private final AlexaProactiveReportHandler proactiveReportHandler;
   private final Provider<VoiceContextExecutorRegistry> registry;

   @Inject
   public ExecuteHandler(
      VoiceDAO voiceDao,
      PlatformMessageBus bus,
      CommandExecutor executor,
      @Named(VoiceConfig.NAME_EXECUTOR) ExecutorService executorService,
      AlexaConfig config,
      AlexaProactiveReportHandler proactiveReportHandler,
      Provider<VoiceContextExecutorRegistry> registry
   ) {
      this.voiceDao = voiceDao;
      this.bus = bus;
      this.executor = executor;
      this.executorService = executorService;
      this.config = config;
      this.proactiveReportHandler = proactiveReportHandler;
      this.registry = registry;
   }

   @Request(value = AlexaService.ExecuteRequest.NAME, service = true, response = false)
   public void handleExecute(VoiceContext context, PlatformMessage msg) {
      HandlerUtil.markAssistantIfNecessary(context, voiceDao);
      MessageBody body = msg.getValue();
      AlexaMetrics.incCommand(AlexaService.ExecuteRequest.getDirective(body));

      Model m = context.getModelByAddress(Address.fromString(AlexaService.ExecuteRequest.getTarget(body)));
      if(m == null) {
         respondError(msg, AlexaErrors.NO_SUCH_ENDPOINT);
         return;
      }

      String directive = AlexaService.ExecuteRequest.getDirective(body);

      boolean hubOffline = context.isHubOffline();
      if(hubOffline && VoicePredicates.isHubRequired(m) && !reportOffline(directive)) {
         respondError(msg, AlexaErrors.BRIDGE_UNREACHABLE);
         return;
      }

      if(VoicePredicates.isDeviceOffline(m, hubOffline) && !reportOffline(directive)) {
         respondError(msg, AlexaErrors.ENDPOINT_UNREACHABLE);
         return;
      }

      if(AlexaPredicates.batteryPowered(m) && DevicePowerModel.getBattery(m, 100) < config.getBatteryThreshold()) {
         respondError(msg, AlexaErrors.ENDPOINT_LOW_POWER);
         return;
      }

      DirectiveTransformer.Txfm txfm = DirectiveTransformer.transformerFor(body);
      try {
         Optional<MessageBody> command = txfm.txfmRequest(body, m, config);
         if(command.isPresent()) {
            boolean cheat = config.isSuccessCheatEnabled() && txfm.allowCheat();
            boolean deferred = Boolean.TRUE.equals(AlexaService.ExecuteRequest.getAllowDeferred(body)) && txfm.deferred();
            if(deferred) {
               DeferredCorrelator correlator = new DeferredCorrelator(
                  msg.getCorrelationId(),
                  AlexaService.ExecuteRequest.getCorrelationToken(body),
                  new HashSet<>(command.get().getAttributes().keySet()),
                  txfm.completableDevAdvErrors(),
                  config.getCorrelationTimeoutSecs()
               );
               proactiveReportHandler.deferResponse(m.getAddress(), correlator);

               // always cheat when deferring
               cheat = true;
            }

            UUID placeId = context.getPlaceId();
            ListenableFuture<Pair<Model, Optional<PlatformMessage>>> future = executor.execute(m.getAddress(), command.get(), context, cheat, txfm.updateModelOnCheat());
            Futures.addCallback(future, new FutureCallback<Pair<Model, Optional<PlatformMessage>>>() {
               @Override
               public void onSuccess(Pair<Model, Optional<PlatformMessage>> result) {
                  // make sure we pull the context out of the registry rather than referencing the existing argument just in
                  // case the context's cache entry was flushed
                  respond(registry.get().get(placeId).context(), msg, result.getLeft(), result.getRight().orElse(null), txfm, deferred);
               }

               @Override
               public void onFailure(@NonNull Throwable t) {
                  logger.warn("unexpected error handling {}", msg, t);
                  respondError(msg, AlexaErrors.INTERNAL_ERROR);
               }
            }, executorService);
         } else {
            logger.debug("completed because the command body was empty, likely the model is already at the state resulting from {}", body);
            respond(context, msg, m, null, txfm, false);
         }
      } catch(Exception e) {
         logger.warn("failed to handle execute request", e);
         if(e instanceof AlexaException) {
            respondError(msg, ((AlexaException) e).getErrorMessage());
         } else {
            respondError(msg, AlexaErrors.INTERNAL_ERROR);
         }
      }
   }

   // allow report state even if the hub is offline because we cache state per https://developer.amazon.com/docs/device-apis/alexa-endpointhealth.html
   private boolean reportOffline(String directive) {
      if(AlexaInterfaces.REQUEST_REPORTSTATE.equals(directive)) {
         return config.isReportStateOffline();
      }
      return false;
   }

   private void respond(VoiceContext context, PlatformMessage request, Model m, @Nullable PlatformMessage msg, DirectiveTransformer.Txfm txfm, boolean deferred) {
      MessageBody r = AlexaService.ExecuteResponse.builder()
         .withProperties(PropertyReporter.report(context, m).stream().map(AlexaPropertyReport::toMap).collect(Collectors.toList()))
         .withPayload(txfm.txfmResponse(m, msg, config))
         .withDeferred(deferred)
         .build();
      bus.sendResponse(request, r);
   }

   private void respondError(PlatformMessage request, MessageBody body) {
      bus.sendResponse(request, body);
   }

}

