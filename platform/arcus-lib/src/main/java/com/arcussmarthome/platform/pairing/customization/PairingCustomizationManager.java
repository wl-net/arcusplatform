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
package com.arcussmarthome.platform.pairing.customization;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.core.dao.DeviceDAO;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.model.serv.PairingDeviceModel;
import com.arcussmarthome.messages.type.PairingCustomizationStep;
import com.arcussmarthome.platform.pairing.PairingDevice;
import com.arcussmarthome.prodcat.customization.PairingCatalog;
import com.arcussmarthome.prodcat.customization.PairingCatalogManager;
import com.arcussmarthome.util.LazyReference;
import com.arcussmarthome.validators.ValidationException;

@Singleton
public class PairingCustomizationManager {
	private static Logger logger = LoggerFactory.getLogger(PairingCustomizationManager.class);
	
	private final DeviceDAO deviceDao;
	private final PairingCatalogManager pairingCatalogManager;
	private final LazyReference<List<PairingCustomization>> customizations = new LazyReference<List<PairingCustomization>>() {
		private volatile PairingCatalog catalog;
		
		@Override
		public List<PairingCustomization> get() {
			// quick check
			if(catalog != pairingCatalogManager.getCatalog()) {
				// real check -- the synchronize & recheck prevents multiple reloads
				// the initial check prevents synchronizing most of the time
				synchronized (this) {
					if(catalog != pairingCatalogManager.getCatalog()) {
						reset();
						return super.get();
					}
				}
			}
			return super.get();
		}

		@Override
		protected List<PairingCustomization> load() {
			catalog = pairingCatalogManager.getCatalog();
			try {
				return PairingCustomization.toPairingCustomization(catalog.getCustomizations());
			}
			catch (ValidationException e) {
				logger.warn("Unable to load pairing customizations", e);
				throw new IllegalStateException(e);
			}
		}
	};
	
	@Inject
	public PairingCustomizationManager(
			DeviceDAO deviceDao,
			PairingCatalogManager pairingCatalogManager
	) {
		this.deviceDao = deviceDao;
		this.pairingCatalogManager = pairingCatalogManager;
	}
	
	@PostConstruct
	public void init() {
		// if this blows up, fail to start
		customizations.get();
	}
	
	public List<PairingCustomizationStep> apply(Place place, PairingDevice pairingDevice) throws ValidationException {
		Model device = deviceDao.modelById((UUID) Address.fromString(PairingDeviceModel.getDeviceAddress(pairingDevice)).getId());
		return 
			customizations
				.get()
				.stream()
				.map((c) -> c.toStepIf(place, device).orElse(null))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
	
}

