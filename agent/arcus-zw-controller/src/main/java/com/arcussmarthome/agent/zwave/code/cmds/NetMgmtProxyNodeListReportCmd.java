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
package com.arcussmarthome.agent.zwave.code.cmds;

import com.arcussmarthome.agent.zwave.code.Decoder;
import com.arcussmarthome.agent.zwave.code.anno.Id;
import com.arcussmarthome.agent.zwave.code.anno.Name;
import com.arcussmarthome.agent.zwave.code.cmdclass.NetMgmtProxyCmdClass;
import com.arcussmarthome.agent.zwave.code.entity.CmdNetMgmtProxyNodeListReport;

@Id(NetMgmtProxyCmdClass.CMD_NODE_LIST_REPORT)
@Name("Node List Report")
public class NetMgmtProxyNodeListReportCmd extends AbstractCmd {

   @Override
   public Decoder getDecoder() {
      return CmdNetMgmtProxyNodeListReport.DECODER;
   }

}


