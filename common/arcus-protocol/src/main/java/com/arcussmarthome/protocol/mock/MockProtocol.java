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
package com.arcussmarthome.protocol.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.arcussmarthome.capability.definition.ProtocolDefinition;
import com.arcussmarthome.io.Deserializer;
import com.arcussmarthome.io.Serializer;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.DeviceAdvancedCapability;
import com.arcussmarthome.messages.service.DeviceService;
import com.arcussmarthome.protocol.Protocol;
import com.arcussmarthome.protocol.Protocols;
import com.arcussmarthome.protocol.RemoveProtocolRequest;
import com.arcussmarthome.protocol.constants.MockConstants;

public enum MockProtocol implements Protocol<String>, MockConstants {
   INSTANCE;

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public String getNamespace() {
      return NAMESPACE;
   }

   @Override
   public ProtocolDefinition getDefinition() {
      return DEFINITION;
   }

   /* (non-Javadoc)
    * @see com.iris.protocol.Protocol#createSerializer()
    */
   @Override
   public Serializer<String> createSerializer() {
      return new Serializer<String>() {
         @Override
         public byte[] serialize(String value) throws IllegalArgumentException {
            return value.getBytes();
         }

         @Override
         public void serialize(String value, OutputStream out) throws IOException, IllegalArgumentException {
            out.write(this.serialize(value));
         }
      };
   }

   /* (non-Javadoc)
    * @see com.iris.protocol.Protocol#createDeserializer()
    */
   @Override
   public Deserializer<String> createDeserializer() {
      return new Deserializer<String>() {

         @Override
         public String deserialize(byte[] input) throws IllegalArgumentException {
            return new String(input);
         }

         @Override
         public String deserialize(InputStream input) throws IOException, IllegalArgumentException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
               IOUtils.copy(input, baos);
            }
            finally {
               baos.close();
            }
            return this.deserialize(baos.toByteArray());
         }
      };
   }

   @Override
   public boolean isTransientAddress() {
      return false;
   }

   @Override
   public PlatformMessage remove(RemoveProtocolRequest req) {
      // uses the same style as a bridge child, just emit an event that says it was deleted
      return Protocols.removeBridgeChild(req);
   }

}

