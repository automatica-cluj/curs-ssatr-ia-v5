# Description

This is a sample demonstrating writing and reading messages from an mqtt broker using the Eclipse Paho Java client library.

Pre-requisites:
- Java 17 or later
- Maven
- A local mqtt client for debugging (e.g. MQTTX - https://mqttx.app/)
- Access to a mqtt broker
  - The examples use the course broker `control.aut.utcluj.ro` (MQTT port `11188`, WebSocket port `11190`)
  - You can use the public broker at `broker.emqx.io` for testing purposes
  - You can also use the docker start a local mqtt broker using command `docker run -it -p 1883:1883 -p 9001:9001 eclipse-mosquitto`
  - You can also download and run the mosquitto broker locally from https://mosquitto.org/download/