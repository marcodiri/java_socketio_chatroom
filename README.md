[![Ubuntu Action Status](https://github.com/marcodiri/java_socketio_chatroom/actions/workflows/chatroom-sonarcloud.yml/badge.svg?branch=master)](https://github.com/marcodiri/java_socketio_chatroom/actions)
[![Windows Action Status](https://github.com/marcodiri/java_socketio_chatroom/actions/workflows/chatroom-windows.yml/badge.svg?branch=master)](https://github.com/marcodiri/java_socketio_chatroom/actions)
[![macOS Action Status](https://github.com/marcodiri/java_socketio_chatroom/actions/workflows/chatroom-macos.yml/badge.svg?branch=master)](https://github.com/marcodiri/java_socketio_chatroom/actions)

[![Coverage Status](https://coveralls.io/repos/github/marcodiri/java_socketio_chatroom/badge.svg?branch=master)](https://coveralls.io/github/marcodiri/java_socketio_chatroom?branch=master)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=security_rating)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=alert_status)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)  
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=bugs)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=code_smells)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=coverage)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=sqale_index)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=marcodiri_java_socketio_chatroom&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=marcodiri_java_socketio_chatroom)

---
# Chatroom App

## Advanced Programming Techniques exam project at UNIFI
Our project is a simple chatroom application developed in **Java 8**, with communications between clients and server handled by the [socket.io](https://socket.io/) library, natively in JavaScript.
We used [this](https://github.com/trinopoty/socket.io-server-java) Java port to develop the **Server** and [this](https://github.com/socketio/socket.io-client-java) to develop the **Client**.
Messages are stored in a **MongoDB** database and the GUI is implemented with **Java Swing**.

## Building
**N.B. The Server is bound to TCP port 3000, make sure that it is available before continuing.**

1. Clone the repo:
```console
git clone https://github.com/marcodiri/java_socketio_chatroom.git
```
2. Navigate inside the root project directory:
```console
cd java_socketio_chatroom
```
### Build with Maven
You can build the **Server** and **Client** jars with Maven using the command (we suggest using the provided wrapper *mvnw* on **Unix** or *mvnw.cmd* on **Windows**):

```console
./mvnw -f java_socketio_chatroom_aggregator/pom.xml clean package -Dmaven.test.skip
```
You'll find the jars in the *target* folder of the corresponding module.

## Testing
### Maven
You can run the tests with Maven using the command:

```console
./mvnw -f java_socketio_chatroom_aggregator/pom.xml clean verify
```

#### Unit tests and Integration tests
The following profiles can be enabled with the `-P` Maven switch:
* `jacoco` to check the code coverage
* `mutation-testing` to run mutation testing with **PIT**

#### End to End tests
To run the e2e tests enable the profile `e2e-test`.

### IDE
For IT and E2E tests you'll need to have a running instance of MongoDB, either on your PC on port **27017** or in a Docker container with the command:
```console
docker run --rm  -p 27017:27017 mongo:4.2.15
```

Then you can manually run the tests from your favourite IDE.

## Running
Build the modules following the steps in the [Building](#Building) section.
To launch the application run the generated *\*-jar-with-dependencies.jar* in the *target* folder of the corresponding module, with the command:
### Server
```console
cd java_socketio_chatroom_server
java -jar ./target/*-jar-with-dependencies.jar
```
### Client
```console
cd java_socketio_chatroom_client
java -jar ./target/*-jar-with-dependencies.jar
```

## Known issues
It seems that sometimes the socket.io library is not able to send or receive the events correctly, resulting in tests failure or undefined behaviours by the **Client** application.
We reported the [issue](https://github.com/trinopoty/socket.io-server-java/issues/17) to the library maintainers.
