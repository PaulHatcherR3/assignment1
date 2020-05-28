<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Assgnment1 CorDapp Three Persons Morris

For the Corda Engineering School assignment this is an implementation of Three Mens (Persons) Morris. Rules and history of the game can be found [here](https://en.wikipedia.org/wiki/Three_men%27s_morris).

The templates directory contains some configuration files for the Azure platform when I was looking at automated deployment. They are not part of the CorDapp project.

The CorDapp project was modified from the cordapp-example from the Corda samples repository. As such you can deploy a development Corda network and test the client and flows quite easily.

Following instructions assume you have JDK 1.8 installed. Also you've cloned this repository locally and have checked out master branch.

## Building Corda
cd to CorDapp then type `./gradlew build` will build the flows, client and run some tests.

## Running Corda
cd to CorDapp type `./gradlew deployNodes` to deploy nodes locally. This will generate node configs and files and copy them into workflows-java/build/nodes. cd into workflows-java/build/nodes. Install at least 8GB of RAM :) then type ./runnodes, this should fire up a notary, and three nodes PartyA, PartyB and PartyC. in the shell you can type `flow list` to see the installed flows. The game flows com.assignment1.TPMFlow should be there.

## Building the client

To make the client fat jar you need to `./gradlew clients:shadowJar`. This will build the client jar in clients/build/libs/clientRpc-0.1-all.jar.

## Running the client

From the CorDapp directory type `java -jar clients/build/libs/clientRpc-0.1-all.jar localhost:10005 gameBerty user1 test`. First argument is RPC connection, second is game id, can be any string, game ids are unique on the ledger. Third argument is the RPC username and password for the connection. These can be found in the node config you are connecting to. If the game is a new game (not on the ledger for the node) then the client will prompt who you want to play against. Then a TPMState is placed on ledger with the other party. Of course the other party always wants to play :)

The above will start one player. To start the other player to play a game jut run the client again connecting to the opponent node. Giving the same gameid (i.e. `gameBerty`) but unique node address, username and password. Then the to players can play against each other.

## TODO

* A game engine in the client for automated play.
* Restful interface for web clients.
* Better error messages.
* Testing, not enough tests on the state or flow. I can think of loads more to add :)

Paul
paul.hatcher@r3.com
