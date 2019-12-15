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
package com.arcussmarthome.client.impl;

import com.google.common.base.Function;
import com.arcussmarthome.capability.util.Addresses;
import com.arcussmarthome.client.ClientEvent;
import com.arcussmarthome.client.ClientRequest;
import com.arcussmarthome.client.IrisClient;
import com.arcussmarthome.client.event.ClientFuture;
import com.arcussmarthome.client.event.Futures;
import com.arcussmarthome.client.service.EasCodeService;

public class EasCodeServiceImpl implements EasCodeService {
	private static final String ADDRESS = Addresses.toServiceAddress(EasCodeService.NAMESPACE);
	private IrisClient client;

	public EasCodeServiceImpl(IrisClient client) {
		this.client = client;
	}

	@Override
	public String getName() {
		return EasCodeService.NAME;
	}

	@Override
	public String getAddress() {
		return ADDRESS;
	}

   @Override
   public ClientFuture<ListEasCodesResponse> listEasCodes() {
      ClientFuture<ClientEvent> result = initializeResultHolder(new ListEasCodesRequest());

      return Futures.transform(result, new Function<ClientEvent, ListEasCodesResponse>() {
         @Override
         public ListEasCodesResponse apply(ClientEvent input) {
            ListEasCodesResponse response = new ListEasCodesResponse(input);
            return response;
         }
      });
   }

   private <E extends ClientRequest> ClientFuture<ClientEvent> initializeResultHolder(E request){
      request.setAddress(getAddress());
      request.setRestfulRequest(true);
      return client.request(request);
   }
}

