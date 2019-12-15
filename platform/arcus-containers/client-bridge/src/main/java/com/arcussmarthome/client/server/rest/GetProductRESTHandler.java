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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.bridge.metrics.BridgeMetrics;
import com.arcussmarthome.bridge.server.config.RESTHandlerConfig;
import com.arcussmarthome.bridge.server.http.HttpSender;
import com.arcussmarthome.bridge.server.http.annotation.HttpPost;
import com.arcussmarthome.bridge.server.http.impl.auth.AlwaysAllow;
import com.arcussmarthome.capability.attribute.transform.BeanAttributesTransformer;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.PopulationDAO;
import com.arcussmarthome.messages.ClientMessage;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.service.ProductCatalogService;
import com.arcussmarthome.messages.service.ProductCatalogService.GetProductRequest;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.prodcat.ProductCatalog;
import com.arcussmarthome.prodcat.ProductCatalogEntry;
import com.arcussmarthome.prodcat.ProductCatalogManager;

@Singleton
@HttpPost("/" + ProductCatalogService.NAMESPACE + "/GetProduct")
public class GetProductRESTHandler extends ProductCatalogRESTHandler {

	private final BeanAttributesTransformer<ProductCatalogEntry> transformer;

	@Inject
	public GetProductRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, ProductCatalogManager manager,
			PopulationDAO populationDao, PlaceDAO placeDao, HubDAO hubDao, BeanAttributesTransformer<ProductCatalogEntry> transformer, @Named("UseChunkedSender") RESTHandlerConfig restHandlerConfig) {
		super(alwaysAllow, new HttpSender(GetProductRESTHandler.class, metrics), manager, populationDao, placeDao, hubDao, restHandlerConfig);
		this.transformer = transformer;
	}

	@Override
	protected MessageBody doHandle(ClientMessage request) throws Exception {

		MessageBody payload = request.getPayload();
		String placeAddressStr = ProductCatalogService.GetProductsRequest.getPlace(payload);
      Population population = determinePopulationFromRequest(placeAddressStr);
      ProductCatalog catalog = getCatalog(population);

		String id = ProductCatalogService.GetProductRequest.getId(payload);
		
		Errors.assertRequiredParam(id, GetProductRequest.ATTR_ID);

		ProductCatalogEntry product = catalog.getProductById(id);

		if (product == null) {
			return MessageBody.emptyMessage();
		}
		
		return ProductCatalogService.GetProductResponse.builder().withProduct(transformer.transform(product)).build();
	}
}

