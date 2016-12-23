/**
 *  Copyright 2015 Peter Major
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    definition (name: "MCO Touch Panel S312", namespace: "VaticanUK", author: "Richard Pope") {
        capability "Switch"
        capability "Refresh"
        capability "Polling"
        capability "Configuration"
        capability "Zw Multichannel"

        attribute "switch1", "string"
        attribute "switch2", "string"

        command "report"
        command "on1"
        command "off1"
        command "on2"
        command "off2"
        

        fingerprint mfr: "015F", prod: "3102", model: "0202"
    }

    simulator {
        status "on":  "command: 2003, payload: FF"
        status "off": "command: 2003, payload: 00"
    }

    tiles(scale: 2){
    	multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            }
        }
        standardTile("switch1", "device.switch1",canChangeIcon: true, width: 2, height: 2) {
            state "on", label: "switch1", action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821"
            state "off", label: "switch1", action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        }
        standardTile("switch2", "device.switch2",canChangeIcon: true, width: 2, height: 2) {
            state "on", label: "switch2", action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821"
            state "off", label: "switch2", action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        standardTile("configure", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"configure", icon:"st.secondary.configure"
        }
        main(["switch","switch1", "switch2"])
        details(["switch","switch1","switch2","refresh","configure"])
    
        /*standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821"
            state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main "switch"
        details (["switch", "refresh"])*/
    }
}

def parse(String description) {
    def result = []
    if (description.startsWith("Err")) {
        result += createEvent(descriptionText:description, isStateChange:true)
    } else {
        def cmd = zwave.parse(description)
        if (cmd) {
            result += zwaveEvent(cmd)
            log.debug "Parsed ${cmd} to ${result.inspect()}"
        } else {
            log.debug "Non-parsed event: ${description}"
        }
        return result
    }
    
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    /*if (cmd.value == 0) {
        createEvent(name: "switch", value: "off")
    } else if (cmd.value == 255) {
        createEvent(name: "switch", value: "on")
    }*/
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	sendEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
	def result = []
	result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
	result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
	response(delayBetween(result, 1000)) // returns the result of reponse()
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
  sendEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
  def result = []
  result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
  result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
  response(delayBetween(result, 1000)) // returns the result of reponse()
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd) 
{
  if (cmd.endPoint == 2 ) {
    def currstate = device.currentState("switch2").getValue()
    if (currstate == "on")
      sendEvent(name: "switch2", value: "off", isStateChange: true, display: false)
    else if (currstate == "off")
      sendEvent(name: "switch2", value: "on", isStateChange: true, display: false)
  }
  else if (cmd.endPoint == 1 ) {
    def currstate = device.currentState("switch1").getValue()
      if (currstate == "on")
        sendEvent(name: "switch1", value: "off", isStateChange: true, display: false)
      else if (currstate == "off")
        sendEvent(name: "switch1", value: "on", isStateChange: true, display: false)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) 
{
  def map = [ name: "switch$cmd.sourceEndPoint" ]
    
  switch(cmd.commandClass) {
    case 32:
      if (cmd.parameter == [0]) {
				map.value = "off"
			}
			if (cmd.parameter == [255]) {
				map.value = "on"
			}
			createEvent(map)
			break
		case 37:
			if (cmd.parameter == [0]) {
				map.value = "off"
			}
			if (cmd.parameter == [255]) {
				map.value = "on"
			}
			break
	}
    
  def events = [createEvent(map)]
  if (map.value == "on") {
    events += [createEvent([name: "switch", value: "on"])]
  } else {
    def allOff = true
    (1..2).each { n ->
      if (n != cmd.sourceEndPoint) {
        if (device.currentState("switch${n}").value != "off") allOff = false
      }
    }
    if (allOff) {
      events += [createEvent([name: "switch", value: "off"])]
    }
  }
  
  events
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd) 
{
  log.debug("ManufacturerSpecificReport ${cmd.inspect()}")
  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  log.debug "msr: $msr"
  updateDataValue("MSR", msr)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) 
{
    // This will capture any commands not handled by other instances of zwaveEvent
    // and is recommended for development so you can see every command the device sends
    return createEvent(descriptionText: "${device.displayName}: ${cmd}")
}

def zwaveEvent(physicalgraph.zwave.commands.switchallv1.SwitchAllReport cmd) 
{
   log.debug "SwitchAllReport $cmd"
}

def report() 
{
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
}

def on() 
{
    zwave.switchAllV1.switchAllOn().format()
}

def on(endpoint) 
{
    delayBetween([
        zwave.switchAllV1.switchAllOn().format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
    ], 1000)
}

def off() 
{
       delayBetween([
        zwave.switchAllV1.switchAllOff().format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
    ], 1000)
}

def on1() 
{
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[255]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    ], 1000)
}

def off1() 
{
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[0]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    ], 1000)
}

def on2() 
{
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[255]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format()
    ], 1000)
}

def off2() 
{
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[0]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format()
    ], 1000)
}

def refresh() 
{
  def cmds = []
    
  cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
  cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
  cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
        
  delayBetween(cmds, 1000)
}

def poll(){
  refresh()
}

def configure() {
  delayBetween([
    zwave.associationV1.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId).format(),
    zwave.associationV1.associationSet(groupingIdentifier: 2, nodeId: zwaveHubNodeId).format(),
    zwave.associationV1.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
  ])
}
