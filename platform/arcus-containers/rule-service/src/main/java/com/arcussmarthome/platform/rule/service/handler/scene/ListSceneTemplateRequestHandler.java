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
/**
 * 
 */
package com.arcussmarthome.platform.rule.service.handler.scene;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Inject;
import com.arcussmarthome.capability.attribute.transform.BeanAttributesTransformer;
import com.arcussmarthome.capability.attribute.transform.BeanListTransformer;
import com.arcussmarthome.core.platform.PlatformRequestMessageHandler;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.errors.Errors;
import com.arcussmarthome.messages.service.SceneService;
import com.arcussmarthome.messages.service.SceneService.ListSceneTemplatesResponse;
import com.arcussmarthome.messages.service.SceneService.ListScenesRequest;
import com.arcussmarthome.platform.scene.SceneTemplateEntity;
import com.arcussmarthome.platform.scene.SceneTemplateManager;

/**
 * 
 */
public class ListSceneTemplateRequestHandler implements PlatformRequestMessageHandler {
   private final SceneTemplateManager sceneTemplateManager;
   private final BeanListTransformer<SceneTemplateEntity> transformer;

   /**
    * 
    */
   @Inject
   public ListSceneTemplateRequestHandler(
         SceneTemplateManager sceneTemplateDao,
         BeanAttributesTransformer<SceneTemplateEntity> transformer
   ) {
      this.sceneTemplateManager = sceneTemplateDao;
      this.transformer = new BeanListTransformer<>(transformer);
   }

   @Override
   public String getMessageType() {
      return SceneService.ListSceneTemplatesRequest.NAME;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      String placeId = ListScenesRequest.getPlaceId(message.getValue());
      Errors.assertRequiredParam(placeId, ListScenesRequest.ATTR_PLACEID);
      Errors.assertPlaceMatches(message, placeId);
      
      List<SceneTemplateEntity> templates = sceneTemplateManager.listByPlaceId(UUID.fromString(placeId));
      List<Map<String, Object>> models = transformer.convertListToAttributes(templates);
      return
            ListSceneTemplatesResponse
               .builder()
               .withSceneTemplates(models)
               .build();
   }

}

