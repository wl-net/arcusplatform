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
package com.iris.agent.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class AsyncHttpService {
   private static final int DEFAULT_TIMEOUT_MS = 10_000;

   private AsyncHttpService() {
   }

   static void start() {
      // no-op — java.net.HttpURLConnection needs no initialization
   }

   static void shutdown() {
      // no-op
   }

   /////////////////////////////////////////////////////////////////////////////
   // HTTP request utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static CompletableFuture<SimpleHttpResponse> getAsync(String uri) {
      return CompletableFuture.supplyAsync(() -> {
         HttpURLConnection conn = null;
         try {
            conn = (HttpURLConnection) new URL(uri).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(DEFAULT_TIMEOUT_MS);
            conn.setReadTimeout(DEFAULT_TIMEOUT_MS);

            int statusCode = conn.getResponseCode();
            InputStream is = statusCode < 400 ? conn.getInputStream() : conn.getErrorStream();
            String body = "";
            if (is != null) {
               try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                  body = reader.lines().collect(Collectors.joining("\n"));
               }
            }
            return new SimpleHttpResponse(statusCode, body);
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         } finally {
            if (conn != null) {
               conn.disconnect();
            }
         }
      });
   }

   public static CompletableFuture<SimpleHttpResponse> getAsync(URI uri) {
      return getAsync(uri.toString());
   }

   public static final class SimpleHttpResponse {
      private final int statusCode;
      private final String body;

      SimpleHttpResponse(int statusCode, String body) {
         this.statusCode = statusCode;
         this.body = body;
      }

      public int statusCode() {
         return statusCode;
      }

      public String body() {
         return body;
      }
   }
}
