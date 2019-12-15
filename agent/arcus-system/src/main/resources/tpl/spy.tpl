#extends spy_tools content_spy

<div class="row">
  <div class="col-md-6">
      <div class="card">
     <div class="card-header">
      <span class="lead">Software Information</span>
     </div>
     <div class="card-body">
       <p>In Debug Mode: {%s com.arcussmarthome.hal.IrisHal#isInDebugMode %}</p>
       Hub ID: {%s com.arcussmarthome.agent.hal.IrisHal#getHubId %}<br>
       Hub OS: {%s com.arcussmarthome.agent.hal.IrisHal#getOperatingSystemVersion %}<br>
       Hub Agent: {%s com.arcussmarthome.agent.hal.IrisHal#getAgentVersion %}<br>
       Bootloader: {%s com.arcussmarthome.agent.hal.IrisHal#getBootloaderVersion %}<br>
       OS Type: {%s com.arcussmarthome.agent.hal.IrisHal#getOsType %}<br>
     </div>
   </div>
  </div>
   
  <div class="col-md-6">
   <div class="card">
     <div class="card-header">
      <span class="lead">Hardware Information</span>
     </div>
     <div class="card-body">
       Vendor: {%s com.arcussmarthome.agent.hal.IrisHal#getVendor %}<br>
       Model: {%s com.arcussmarthome.agent.hal.IrisHal#getModel %}<br>
       Serial Number: {%s com.arcussmarthome.agent.hal.IrisHal#getSerialNumber %}<br>
       Mac Address: {%s com.arcussmarthome.agent.hal.IrisHal#getMacAddress %}<br>
       Hardware Version: {%s com.arcussmarthome.agent.hal.IrisHal#getHardwareVersion %}<br>
       Manufacturing Info: {%s com.arcussmarthome.hal.IrisHal#getManufacturingInfo %}<br>
       Manufacturing Date: {%s com.arcussmarthome.hal.IrisHal#getManufacturingDate %}<br>
       Manufacturing Factory ID: {%s com.arcussmarthome.hal.IrisHal#getManufacturingFactoryID %}<br>
     </div>
   </div>
  </div>
   
</div>

