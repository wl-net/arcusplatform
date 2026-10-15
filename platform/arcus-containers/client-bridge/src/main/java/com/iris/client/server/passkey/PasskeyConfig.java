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
package com.iris.client.server.passkey;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Passkeys are disabled unless {@code passkey.rp.id} is set.
 */
@Singleton
public class PasskeyConfig {

   /**
    * The relying party id: the domain passkeys are bound to, e.g. "example.com". The
    * web app and the client bridge must both be served from it or a subdomain of it.
    */
   @Inject(optional = true) @Named("passkey.rp.id")
   private String rpId = "";

   /** Shown by the browser and authenticator when creating a passkey. */
   @Inject(optional = true) @Named("passkey.rp.name")
   private String rpName = "Arcus";

   /**
    * Comma-separated origins allowed to use passkeys, e.g. "https://example.com".
    * Defaults to https://{rpId}.
    */
   @Inject(optional = true) @Named("passkey.origins")
   private String origins = "";

   /** How long a browser has to answer a registration or login request. */
   @Inject(optional = true) @Named("passkey.request.ttl.secs")
   private int requestTtlSecs = 300;

   public boolean isEnabled() {
      return StringUtils.isNotBlank(rpId);
   }

   public String getRpId() {
      return rpId;
   }

   public void setRpId(String rpId) {
      this.rpId = rpId;
   }

   public String getRpName() {
      return rpName;
   }

   public void setRpName(String rpName) {
      this.rpName = rpName;
   }

   public Set<String> getOrigins() {
      if (StringUtils.isBlank(origins)) {
         Set<String> defaults = new LinkedHashSet<>();
         defaults.add("https://" + rpId);
         return defaults;
      }
      return Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public void setOrigins(String origins) {
      this.origins = origins;
   }

   public int getRequestTtlSecs() {
      return requestTtlSecs;
   }

   public void setRequestTtlSecs(int requestTtlSecs) {
      this.requestTtlSecs = requestTtlSecs;
   }
}
