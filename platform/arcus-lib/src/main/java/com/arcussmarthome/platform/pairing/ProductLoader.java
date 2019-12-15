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
package com.arcussmarthome.platform.pairing;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.prodcat.ProductCatalog;
import com.arcussmarthome.prodcat.ProductCatalogEntry;
import com.arcussmarthome.prodcat.ProductCatalogManager;

@Singleton
public class ProductLoader {
	private final ProductCatalogManager manager;
	
	@Inject
	public ProductLoader(ProductCatalogManager manager) {
		this.manager = manager;
	}
	
	public ProductCatalogEntry get(Place place, Address productAddress) {
		String population = place.getPopulation();
		return get(population, productAddress);
	}
	
	protected ProductCatalogEntry get(String population, Address productAddress) {
		if(StringUtils.isEmpty(population)) {
			population = Population.NAME_GENERAL;
		}
		ProductCatalog catalog = manager.getCatalog(population);
		Preconditions.checkState(catalog != null, "Unable to load product catalog");

		ProductCatalogEntry entry = catalog.getProductById(String.valueOf(productAddress.getId()));
		Preconditions.checkState(entry != null, "No product catalog entry addressed %s was found", productAddress);
		
		return entry;
	}
	
	protected ProductCatalogManager getProductCatalogManager() {
		return manager;
	}
}

