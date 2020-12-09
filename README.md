# CMPT471 - Project

## Purpose

- To create a clustered WebSocket Server, that hold same data across multiple instances

## Code

[hahmjt/cmpt471_project](https://github.com/hahmjt/cmpt471_project)

- The project is made with Intelij, and Gradle multi module project
- JDK8 is required
- To execute the server please run this in root folder of the project

    ```bash
    ./gradlew :server:bootRun
    ```

- To launch the client please run this in root folder of the project

    ```bash
    ./gradlew :client:bootRun
    ```

- Transferred data should be printed in the terminal
- Spring Framework was used to ease development process
- Netty was used for TCP transport only, and it did not impact my implementation of the WebSocket Server and Client

## TL;DR

- In the root folder, there is docker-composer to launch three WebSocket servers and Nginx for load balancing to simulate clustered environment. With Docker installed, run

    ```bash
    docker-compose up
    ```

- Each server generates random data to simulate data generation. UDP broadcast was used to sync data across three servers.
- Regardless of server the clients connect to, it will share the same data.
    - I was hoping to create a procotol for the servers to share data quickly and reliably, but wasn't able to do so. I was able to achieve sync with UDP broadcasting.
- WebSocket Server Implementation supports masking, and multi frame transport
- Nginx configuration for load balancing three WebSocket server is in the nginx folder

## Test Cases

- Connecting to ws://[localhost:9999](http://localhost:9999)/ via [https://www.websocket.org/echo.html](https://www.websocket.org/echo.html) and sending any text will return the connected hostname
- Connecting to ws://[localhost:9999](http://localhost:9999)/data via [https://www.websocket.org/echo.html](https://www.websocket.org/echo.html) and sending any text will return the data the server holds, which should be same across three servers
    - Each data will be transferred as separate frame
- Using my Implementation of client will send "Hello Server" after initial handshake
    - WebSocket Client was tested with ws://echo.websocket.org

## Known Issues

- Functions that are implemented works fine, but there were some aspects i was not able to implement due to time constraints
- Such functions like permessage-deflate extension which is optional is not implemented.
- Sharing data across server with UDP without any confirmation of transfer could result in loss of data. I was hoping to create more sophisticated solution but was not able to do so.

## Videos

### Testing Client Implementation

[https://youtu.be/drqH0fe2oRo](https://youtu.be/drqH0fe2oRo)

### Showing Cluster of WebSocket Servers

[https://youtu.be/8amkockyols](https://youtu.be/8amkockyols)

### Connecting to Server Implementation with Chrome Browser

[https://youtu.be/5rDnZFW8YMw](https://youtu.be/5rDnZFW8YMw)

### Showing simulated data sharing between servers

[https://youtu.be/bO8K2z1VRm4](https://youtu.be/bO8K2z1VRm4)
