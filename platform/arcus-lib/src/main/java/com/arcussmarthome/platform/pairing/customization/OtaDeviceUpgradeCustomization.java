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
import java.util.Optional;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Predicate;
import com.arcussmarthome.bootstrap.ServiceLocator;
import com.arcussmarthome.core.dao.PopulationDAO;
import com.arcussmarthome.firmware.ota.DeviceOTAFirmwareResolver;
import com.arcussmarthome.firmware.ota.DeviceOTAFirmwareResponse;
import com.arcussmarthome.messages.capability.DeviceOtaCapability;
import com.arcussmarthome.messages.model.Model;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.model.dev.DeviceModel;
import com.arcussmarthome.messages.model.dev.DeviceOtaModel;
import com.arcussmarthome.messages.type.Population;

class OtaDeviceUpgradeCustomization extends PairingCustomization {
		
		private final Predicate<Model> matcher;

		OtaDeviceUpgradeCustomization(
				Predicate<Model> matcher, 
				String action,
				String id,
				@Nullable String header,
				@Nullable String title,
				@Nullable String note,
				@Nullable List<String> description,
				@Nullable String linkText,
				@Nullable String linkUrl
		) {
			super(
					action,
					id,
					header,
					title,
					note,
					description,
					linkText,
					linkUrl
			);
			this.matcher = matcher;
		}

		@Override
		protected boolean apply(Place place, Model device) {
			if(matcher != null && !matcher.apply(device)) {
				return false;
			}
			if(device.supports(DeviceOtaCapability.NAMESPACE)) {
				//TODO - Make it injectable
				DeviceOTAFirmwareResolver firmwareResolver = ServiceLocator.getInstance(DeviceOTAFirmwareResolver.class);
				PopulationDAO populationDao = ServiceLocator.getInstance(PopulationDAO.class);
				Population population = place.getPopulation()!=null?populationDao.findByName(place.getPopulation()):null;
				DeviceOTAFirmwareResponse fwResponse = firmwareResolver.resolve(Optional.ofNullable(population!=null?population.getName():null), DeviceModel.getProductId(device, ""), DeviceOtaModel.getCurrentVersion(device, ""));
				return fwResponse.isUpgrade();
			}
			return false;
		}
}

