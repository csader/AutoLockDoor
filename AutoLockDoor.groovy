/**
 *  **************** Auto Lock Door ****************
 *
 *  Design Usage:
 *  Automatically locks a specific door after X minutes when closed and unlocks it when open after X seconds. Requires a contact sensor to prevent auto locking when the door is open.
 *
 *  Copyright 2019-2020 Chris Sader (@chris.sader)
 *
 *  This App is free.  If you like and use this app, please be sure to mention it on the Hubitat forums!  Thanks.
 *
 *  Donations to support development efforts are accepted via:
 *
 *  Paypal at: https://paypal.me/csader
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @chris.sader
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Changes:
 *
 *  1.0.1 Fixed null bug, removed "optional" from contact sensor (thanks @SoundersDude and @chipworkz)
 *  1.0.0 Initial Release
 *
 */

def setVersion(){
    state.name = "Auto Lock Door"
	state.version = "1.0.1"
}

definition(
    name: "Auto Lock Door",
    namespace: "chris.sader",
    author: "Chris Sader",
    description: "Automatically locks a specific door after X minutes when closed and unlocks it when open after X seconds.",
    tag: "Locks",
    iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
    iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg",
    iconX3Url: "http://www.gharexpert.com/mid/4142010105208.jpg",
    importUrl: "https://github.com/csader/AutoLockDoor/blob/master/AutoLockDoor.groovy"
)

preferences
{
    section("When a door unlocks...") {
        input "lock1", "capability.lock"
    }
    section("Lock it how many minutes later?") {
        input "minutesLater", "number", title: "Enter # Minutes"
    }
    section("Lock it only when this door is closed.") {
        input "openSensor", "capability.contactSensor", title: "Choose Door Contact Sensor"
    }
}

def installed()
{
    log.debug "Auto Lock Door installed."
    initialize()
}

def updated()
{
    unsubscribe()
    unschedule()
    log.debug "Auto Lock Door updated."
    initialize()
}

def initialize()
{
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", doorHandler)
    subscribe(openSensor, "contact.closed", doorClosed)
    subscribe(openSensor, "contact.open", doorOpen)
}

def lockDoor()
{
    log.debug "Locking Door if Closed"
    if((openSensor.latestValue("contact") == "closed")){
    	log.debug "Door Closed"
    	lock1.lock()
    } else {
    	if ((openSensor.latestValue("contact") == "open")) {
        def delay = minutesLater * 60
        log.debug "Door open will try again in $minutesLater minutes"
        runIn( delay, lockDoor )
        }
    }
}

def doorOpen(evt) {
    log.debug "Door open reset previous lock task..."
    unschedule( lockDoor )
    def delay = minutesLater * 60
    runIn( delay, lockDoor )
}

def doorClosed(evt) {
    log.debug "Door Closed"
}

def doorHandler(evt)
{
    log.debug "Door ${openSensor.latestValue("contact")}"
    log.debug "Lock ${evt.name} is ${evt.value}."

    if (evt.value == "locked") {                  // If the human locks the door then...
        log.debug "Cancelling previous lock task..."
        unschedule( lockDoor )                  // ...we don't need to lock it later.
    }
    else {                                      // If the door is unlocked then...
        def delay = minutesLater * 60          // runIn uses seconds
        log.debug "Re-arming lock in ${minutesLater} minutes (${delay}s)."
        runIn( delay, lockDoor )                // ...schedule to lock in x minutes.
    }
}
