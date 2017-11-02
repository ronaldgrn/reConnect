# reConnect
A simple Java based Emergency communication system using TCP Sockets

## How to execute
##### To compile
    mvn clean compile assembly:single


##### To run server
    java -cp reConnect-1.0-SNAPSHOT.jar Server
    

##### To run Client
    java -jar reConnect-1.0-SNAPSHOT.jar
    
or

    java -cp reConnect-1.0-SNAPSHOT.jar Client


## Server Commands
> **#create** roomName

Creates a new room

> **#banip** 127.0.0.1

Blacklists a particular ip. Closes all current connections and prevents new connections

> **#banuser** 589768

Kicks a particular user from all rooms.

> **#exit**

Ends server service



## Client Commands
> **#clear**

Clears screen


> **#rooms**

Displays a listing of rooms that the client can connect to


> **#join** roomName

When client is first started, lets us join an existing room

> **#setname** Ronald

When connected to a room, allows client to set a nickname
