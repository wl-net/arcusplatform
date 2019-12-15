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
package com.arcussmarthome.client.server.rest;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.config.RESTHandlerConfig;
import com.arcussmarthome.bridge.server.http.HttpSender;
import com.arcussmarthome.bridge.server.http.annotation.HttpPost;
import com.arcussmarthome.bridge.server.http.impl.auth.AlwaysAllow;
import com.arcussmarthome.capability.attribute.transform.BeanAttributesTransformer;
import com.arcussmarthome.capability.attribute.transform.BeanListTransformer;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.PopulationDAO;
import com.arcussmarthome.messages.ClientMessage;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.service.ProductCatalogService;
import com.arcussmarthome.messages.service.ProductCatalogService.GetProductsByBrandRequest;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.model.Version;
import com.arcussmarthome.prodcat.ProductCatalog;
import com.arcussmarthome.prodcat.ProductCatalogEntry;
import com.arcussmarthome.prodcat.ProductCatalogManager;

@Singleton
@HttpPost("/" + ProductCatalogService.NAMESPACE + "/GetProductsByBrand")
public class GetProductsByBrandRESTHandler extends ProductCatalogRESTHandler {

	private final BeanListTransformer<ProductCatalogEntry> listTransformer;

	@Inject
	public GetProductsByBrandRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, ProductCatalogManager manager,
			PopulationDAO populationDao, PlaceDAO placeDao, HubDAO hubDao, BeanAttributesTransformer<ProductCatalogEntry> transformer, @Named("UseChunkedSender") RESTHandlerConfig restHandlerConfig) {
		super(alwaysAllow, new HttpSender(GetProductsByBrandRESTHandler.class, metrics), manager, populationDao, placeDao, hubDao, restHandlerConfig);

		this.listTransformer = new BeanListTransformer<ProductCatalogEntry>(transformer);
	}

	@Override
	protected MessageBody doHandle(ClientMessage request) throws Exception {
		
		MessageBody payload = request.getPayload();
		String placeAddressStr = ProductCatalogService.GetProductsRequest.getPlace(payload);
      Population population = determinePopulationFromRequest(placeAddressStr);
      Version hubFwVersion = determineHubFirmwareVersionFromRequest(placeAddressStr);
      ProductCatalog catalog = getCatalog(population);
      Boolean hubRequired = (Boolean) payload.getAttributes().get("hubrequired");

		String brandName = ProductCatalogService.GetProductsByBrandRequest.getBrand(payload);

		Errors.assertRequiredParam(brandName, GetProductsByBrandRequest.ATTR_BRAND);

		List<ProductCatalogEntry> entries;
		
		entries = catalog.getProductsByBrand(brandName, hubFwVersion, hubRequired);
		
		List<Map<String, Object>> products = listTransformer.convertListToAttributes(entries);
		return ProductCatalogService.GetProductsByBrandResponse.builder().withProducts(products).build();
	}

}

