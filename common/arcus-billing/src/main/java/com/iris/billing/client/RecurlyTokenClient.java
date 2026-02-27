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
package com.iris.billing.client;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iris.billing.client.model.RecurlyJSONResponse;
import com.iris.billing.client.model.request.BillingInfoRequest;
import com.iris.billing.exception.RecurlyAPIErrorException;

public class RecurlyTokenClient {
    private static final String TOKEN_URL = "https://api.recurly.com/js/v1/token";
    private static final Gson GSON = new GsonBuilder().create();
    private final HttpClient client;

    public RecurlyTokenClient() {
        this.client = HttpClient.newHttpClient();
    }

    public ListenableFuture<String> getBillingToken(BillingInfoRequest request) {
        return doGetBillingToken(request);
    }

    private final ListenableFuture<String> doGetBillingToken(BillingInfoRequest billingInfoRequest) {
        final SettableFuture<String> future = SettableFuture.create();

        try {
            String formBody = billingInfoRequest.getMappings().entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                            + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header(HttpHeaders.ACCEPT, "application/xml")
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            client.sendAsync(request, BodyHandlers.ofString())
                    .whenComplete((response, ex) -> {
                        if (ex != null) {
                            future.setException(ex);
                        } else {
                            try {
                                RecurlyJSONResponse message = GSON.fromJson(
                                        response.body(),
                                        RecurlyJSONResponse.class
                                );
                                if (message.isError()) {
                                    future.setException(new RecurlyAPIErrorException(message.getCode(), message.getMessage()));
                                } else {
                                    future.set(message.getID());
                                }
                            } catch (Exception e) {
                                future.setException(e);
                            }
                        }
                    });
        } catch (Exception ex) {
            future.setException(ex);
        }

        return future;
    }
}
