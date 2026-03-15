@ZWave @Fibaro @button
Feature: ZWave Fibaro KeyFob FGKF-601 Driver Test

These scenarios test the functionality of the ZWave Fibaro KeyFob FGKF-601 driver

    Background:
    Given the ZW_Fibaro_KeyFob.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Keyfob
        And the message's devadv:drivername attribute should be ZWFibaroKeyFobDriver
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be false
        And the message's devpow:backupbatterycapable attribute should be false
    Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability but:statechanged.square should be recent
            And the capability but:statechanged.circle should be recent
            And the capability but:statechanged.cross should be recent
            And the capability but:statechanged.triangle should be recent
            And the capability but:statechanged.minus should be recent
            And the capability but:statechanged.plus should be recent

    @basic @connected @timeout
    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 2220 minutes

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should be <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Fob"              |
          | "Bob & Sue's Remote"     |


############################################################
# Generic ZWave Driver Tests
############################################################

    Scenario: Device reports battery level
        Given the capability devpow:battery is 50
        When the device response with battery report
            And with parameter level 75
            And send to driver
        Then the platform attribute devpow:battery should be 75
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

    Scenario: Make sure driver handles ZWave Plus Info Reports
        When the device response with zwaveplus_info report
            And with parameter zwaveversion 5
            And with parameter roletype 6
            And with parameter nodetype 2
            And send to driver
        Then protocol bus should be empty

    Scenario: Make sure driver handles Device Reset Locally Notification
        When the device response with device_reset_locally notification
            And send to driver
        Then protocol bus should be empty


############################################################
# Central Scene Tests - Square Button (Scene 1)
############################################################

    @button
    Scenario: Square button single press
        Given the capability but:state.square is RELEASED
        When the device response with central_scene notification
            And with parameter sequencenumber 1
            And with parameter properties1 0
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.square should be PRESSED
            And the capability but:statechanged.square should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Square button released
        Given the capability but:state.square is PRESSED
        When the device response with central_scene notification
            And with parameter sequencenumber 2
            And with parameter properties1 1
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.square should be RELEASED
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Circle Button (Scene 2)
############################################################

    @button
    Scenario: Circle button single press
        Given the capability but:state.circle is RELEASED
        When the device response with central_scene notification
            And with parameter sequencenumber 3
            And with parameter properties1 0
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.circle should be PRESSED
            And the capability but:statechanged.circle should be recent
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Cross Button (Scene 3)
############################################################

    @button
    Scenario: Cross button single press
        Given the capability but:state.cross is RELEASED
        When the device response with central_scene notification
            And with parameter sequencenumber 4
            And with parameter properties1 0
            And with parameter scenenumber 3
            And send to driver
        Then the capability but:state.cross should be PRESSED
            And the capability but:statechanged.cross should be recent
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Triangle Button (Scene 4)
############################################################

    @button
    Scenario: Triangle button single press
        Given the capability but:state.triangle is RELEASED
        When the device response with central_scene notification
            And with parameter sequencenumber 5
            And with parameter properties1 0
            And with parameter scenenumber 4
            And send to driver
        Then the capability but:state.triangle should be PRESSED
            And the capability but:statechanged.triangle should be recent
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Minus Button (Scene 5)
############################################################

    @button
    Scenario: Minus button single press
        Given the capability but:state.minus is RELEASED
        When the device response with central_scene notification
            And with parameter sequencenumber 6
            And with parameter properties1 0
            And with parameter scenenumber 5
            And send to driver
        Then the capability but:state.minus should be PRESSED
            And the capability but:statechanged.minus should be recent
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Plus Button (Scene 6)
############################################################

    @button
    Scenario: Plus button single press
        Given the capability but:state.plus is RELEASED
        When the device response with central_scene notification
            And with parameter sequencenumber 7
            And with parameter properties1 0
            And with parameter scenenumber 6
            And send to driver
        Then the capability but:state.plus should be PRESSED
            And the capability but:statechanged.plus should be recent
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Double/Triple Tap
############################################################

    @button
    Scenario: Square button double press
        Given the capability but:state.square is RELEASED
        When the device response with central_scene notification
            And with parameter sequencenumber 8
            And with parameter properties1 3
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.square should be PRESSED
            And the capability but:statechanged.square should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Circle button held down
        Given the capability but:state.circle is RELEASED
        When the device response with central_scene notification
            And with parameter sequencenumber 9
            And with parameter properties1 2
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.circle should be PRESSED
            And the capability but:statechanged.circle should be recent
            And the driver should place a base:ValueChange message on the platform bus
