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
package com.arcussmarthome.platform.subsystem.incident;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.common.alarm.AlertType;
import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.common.subsystem.alarm.incident.AlarmIncidentService;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.MessageConstants;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.errors.ErrorEventException;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.model.subs.AlarmSubsystemModel;
import com.arcussmarthome.messages.service.AlarmService;
import com.arcussmarthome.messages.type.IncidentTrigger;
import com.arcussmarthome.platform.alarm.incident.AlarmIncident;
import com.arcussmarthome.platform.alarm.incident.AlarmIncident.AlertState;
import com.arcussmarthome.platform.alarm.incident.AlarmIncidentDAO;
import com.arcussmarthome.platform.subsystem.SubsystemRegistry;
import com.arcussmarthome.population.PlacePopulationCacheManager;
import com.arcussmarthome.util.IrisCollections;
import com.arcussmarthome.util.IrisUUID;

@Singleton
public class PlatformAlarmIncidentService extends BaseAlarmIncidentService implements AlarmIncidentService {
	private static final Logger logger = LoggerFactory.getLogger(PlatformAlarmIncidentService.class);
	private final PlatformMessageBus platformBus;
	private final Cache<String, SettableFuture<Void>> cancelCache;
	private final ScheduledExecutorService cleanUpScheduler = Executors.newScheduledThreadPool(1);
	
	@Inject(optional = true)
	@Named("alarm.alert.timeout.secs")
	private int alertTimeoutSeconds = (int) TimeUnit.MINUTES.toSeconds(15);
	
	@Inject(optional = true)
	@Named("alarm.cancel.timeout.secs")
	private int cancelTimeoutSeconds = 30;

	@Inject(optional = true)
	@Named("alarm.cancel.timeout.concurrency")
	private int cancelTimeoutConcurrency = 4;

	@Inject(optional = true)
	@Named("alarm.cancel.cleanup.frequency.secs")
	private int cancelCleanupSecs = 5;
	
	@Inject
	public PlatformAlarmIncidentService(
			SubsystemRegistry registry,
			AlarmIncidentDAO incidentDao,
			AlarmIncidentHistoryListener historyListener,
			PlatformMessageBus platformBus,
			PlacePopulationCacheManager populationCacheMgr
	) {
		super(registry, incidentDao, historyListener, platformBus, populationCacheMgr);
		this.platformBus = platformBus;
		this.cancelCache = CacheBuilder.newBuilder()
			.recordStats()
			.concurrencyLevel(cancelTimeoutConcurrency)
			.expireAfterWrite(cancelTimeoutSeconds, TimeUnit.SECONDS)
			.removalListener((RemovalListener<String, SettableFuture<Void>>) notification -> {
				if(notification.wasEvicted() && notification.getValue() != null) {
					notification.getValue().setException(new TimeoutException());
				}
			})
			.build();
		cleanUpScheduler.scheduleAtFixedRate(cancelCache::cleanUp, 0, cancelCleanupSecs, TimeUnit.SECONDS);
	}
	
	@Override
	protected void onAlertAdded(SubsystemContext<?> context, AlarmIncident incident, String alarm, List<IncidentTrigger> events, boolean sendNotifications) {
		if(sendNotifications) {
			issueAlertAdded(incident, alarm, events);
		}
		super.onAlertAdded(context, incident, alarm, events, sendNotifications);
	}

	@Override
	protected void onAlertUpdated(SubsystemContext<?> context, AlarmIncident incident, String alarm, List<IncidentTrigger> events, boolean sendNotifications) {
		if(sendNotifications) {
			issueAlertAdded(incident, alarm, events);
		}
		super.onAlertUpdated(context, incident, alarm, events, sendNotifications);
	}

	@Override
	protected ListenableFuture<Void> doCancel(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, Address cancelledBy, String method) {
		String correlationId = UUID.randomUUID().toString();

		SettableFuture<Void> future = SettableFuture.create();
		cancelCache.put(correlationId, future);
		issueCancel(incident, cancelledBy, method, correlationId, cancelTimeoutSeconds);
		return future;
	}

	@Override
	protected Date onIncidentVerified(SubsystemContext<AlarmSubsystemModel> context, AlarmIncident incident, Address verifiedBy) {
		Date timeVerified = super.onIncidentVerified(context, incident, verifiedBy);
		if(incident.getAlertState() != AlertState.ALERT) {
			// if it's still in PREALERT, then VERIFY will trigger the ALERT and VERIFIED will come through that way
			return timeVerified;
		}
		if(timeVerified != null) {
			IncidentTrigger trigger = new IncidentTrigger();
			if(incident.getAlert() == AlertType.SMOKE || incident.getAdditionalAlerts().contains(AlertType.SMOKE)) {
				trigger.setAlarm(IncidentTrigger.ALARM_SMOKE);
			}
			else if(incident.getAlert() == AlertType.SECURITY || incident.getAdditionalAlerts().contains(AlertType.SECURITY)) {
				trigger.setAlarm(IncidentTrigger.ALARM_SECURITY);
			}
			else {
				trigger.setAlarm(IncidentTrigger.ALARM_PANIC);
			}
			trigger.setEvent(IncidentTrigger.EVENT_VERIFIED_ALARM);
			trigger.setSource(verifiedBy.getRepresentation());
			trigger.setTime(timeVerified);
			issueAlertAdded(incident, trigger.getAlarm(), ImmutableList.of(trigger));
			markTriggerSent(context, trigger);	
		}
		return timeVerified;
	}

	/**
	 * @see AlarmIncidentServiceDispatcher#setAttributes(com.arcussmarthome.common.subsystem.SubsystemExecutor, PlatformMessage)
	 */
	public void setAttributes(PlatformMessage msg) {
		Errors.assertValidRequest(
				MessageConstants.SERVICE.equals(msg.getSource().getNamespace()) &&
				!StringUtils.isEmpty(msg.getPlaceId()), 
				"Unsupported operation"
		);
		onIncidentUpdated(IrisUUID.fromString(msg.getPlaceId()), msg.getDestination(), msg.getValue());
	}

	/**
	 * @see AlarmIncidentServiceDispatcher#onEvent(com.arcussmarthome.common.subsystem.SubsystemExecutor, PlatformMessage)
	 * @param msg
	 */
	public void onEvent(PlatformMessage msg) {
		switch(msg.getMessageType()) {
		case AlarmService.CancelAlertResponse.NAME:
		case MessageConstants.MSG_EMPTY_MESSAGE:
		case MessageConstants.MSG_ERROR:
			onResponse(msg);
			break;
		}
	}
	
	private void onResponse(PlatformMessage msg) {
		SettableFuture<Void> future = cancelCache.getIfPresent(msg.getCorrelationId());
		if(future != null) {
			cancelCache.invalidate(msg.getCorrelationId());
			if(msg.isError()) {
				MessageBody body = msg.getValue();
				future.setException(new ErrorEventException((String) body.getAttributes().get("code"), (String) body.getAttributes().get("message")));
			} else {
				future.set(null);
			}
		}
	}

   protected void issueAlertAdded(AlarmIncident incident, String alarm, List<IncidentTrigger> triggers) {
		MessageBody body = AlarmService.AddAlarmRequest.builder()
				.withAlarm(alarm)
				.withTriggers(IrisCollections.transform(triggers, IncidentTrigger::toMap))
				.withAlarms(allAlarms(incident))
				.build();
		PlatformMessage msg = PlatformMessage.buildRequest(
				body,
				incident.getAddress(),
				Address.platformService(AlarmService.NAMESPACE)
			)
			.withCorrelationId(UUID.randomUUID().toString())
			.withPlaceId(incident.getPlaceId())
			.withPopulation(populationCacheMgr.getPopulationByPlaceId(incident.getPlaceId()))
			.withTimeToLive((int) TimeUnit.SECONDS.toMillis(alertTimeoutSeconds))
			.create();
		platformBus.send(msg);
	}

	private void issueCancel(AlarmIncident incident, Address cancelledBy, String method, String correlationId, int cancelTimeoutSeconds) {
		MessageBody body = AlarmService.CancelAlertRequest.builder()
				.withAlarms(allAlarms(incident))
				.withMethod(method)
				.build();

		PlatformMessage msg = PlatformMessage.buildRequest(
				body,
				incident.getAddress(),
				Address.platformService(AlarmService.NAMESPACE)
			)
			.withCorrelationId(correlationId)
			.withActor(cancelledBy)
			.withPlaceId(incident.getPlaceId())
			.withPopulation(populationCacheMgr.getPopulationByPlaceId(incident.getPlaceId()))
			.withTimeToLive((int) TimeUnit.SECONDS.toMillis(cancelTimeoutSeconds))
			.create();

		platformBus.send(msg);
	}	

}

