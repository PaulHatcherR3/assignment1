<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Three Persons Morris CorDapp

For the Corda Engineering School assignment this is an implementation of Three Mens (Persons) Morris. Rules and history of the game can be found [here](https://en.wikipedia.org/wiki/Three_men%27s_morris).

The templates directory contains some configuration files for the Azure platform when I was looking at automated deployment. They are not part of the CorDapp project.

The CorDapp project was modified from the [cordapp-example](https://github.com/corda/samples-java/tree/master/Basic/cordapp-example) from the Corda samples repository. With the samples in that repository you can create and deploy nodes for testing and development very easily.

The following instructions assume you have JDK 1.8 installed. Also you've cloned this repository locally and have checked out master branch.

## Building Corda
cd to CorDapp then type `./gradlew build` will build the flows, client and run some tests.

## Running Corda
cd to CorDapp type `./gradlew deployNodes` to deploy nodes locally. This will generate node configs and files and copy them into workflows-java/build/nodes. cd into workflows-java/build/nodes. Install at least 16GB of RAM :) then type `./runnodes`, this should start a notary, and three nodes PartyA, PartyB and PartyC. in the shell you can type `flow list` to see the installed flows. The game flows com.assignment1.TPMFlow should be there.

## Building the client

To make the client fat jar you need to `./gradlew clients:shadowJar`. This will build the client jar in clients/build/libs/clientRpc-0.1-all.jar.

## Running the client

From the CorDapp directory type `java -jar clients/build/libs/clientRpc-0.1-all.jar localhost:10005 gameBerty user1 test`. First argument is RPC connection, second is the gameId, can be any string, gameIds are unique on the ledger. Third argument is the RPC username and password for the connection. These can be found in the node config you are connecting to. If the game is a new game (not on the ledger for the node) then the client will prompt who you want to play against. Then a TPMState is placed on ledger with the other party. Of course the other party always wants to play :)

The above will start one player. To start the other player to play a game run the client again connecting to the opponent node. Giving the same gameid (i.e. `gameBerty`) but unique node address, username and password. Then the to players can play against each other.

## Playing the game

If the gameId is not on ledger then you'll be prompted to create it, and type the name of the opposing player (Party).
```
paulhatcher@20LDN-MAC280 CorDapp % java -jar clients/build/libs/clientRpc-0.1-all.jar localhost:10009 gameBerty user1 test
[INFO ] 10:11:16.472 [main] RPCClient.logElapsedTime - Startup took 675 msec
[INFO ] 10:11:16.638 [main] JavaClientRpc.run - Node legal identity O=PartyB, L=New York, C=US
[INFO ] 10:11:16.660 [main] JavaClientRpc.run - All the nodes available in this network
[INFO ] 10:11:16.878 [main] JavaClientRpc.run - Game 'gameBerty' not found, create? [y/n] :
Game 'gameBerty' not found, create? [y/n] : y
Enter opponent name : PartyA
[INFO ] 10:11:23.347 [main] JavaClientRpc.run - Creating game 'gameBerty' with opponent 'O=PartyA, L=London, C=GB'
[INFO ] 10:11:23.437 [main] JavaClientRpc.lambda$run$3 - Starting
[INFO ] 10:11:23.437 [main] JavaClientRpc.lambda$run$3 - Generating transaction based on new IOU.
[INFO ] 10:11:23.442 [main] JavaClientRpc.lambda$run$3 - Verifying contract constraints.
[INFO ] 10:11:23.443 [main] JavaClientRpc.lambda$run$3 - Signing transaction with our private key.
[INFO ] 10:11:23.444 [main] JavaClientRpc.lambda$run$3 - Gathering the counterparty's signature.
[INFO ] 10:11:23.460 [main] JavaClientRpc.lambda$run$3 - Collecting signatures from counterparties.
[INFO ] 10:11:23.468 [main] JavaClientRpc.lambda$run$3 - Starting
[INFO ] 10:11:25.068 [main] JavaClientRpc.lambda$run$3 - Verifying collected signatures.
[INFO ] 10:11:25.073 [main] JavaClientRpc.lambda$run$3 - Obtaining notary signature and recording transaction.
[INFO ] 10:11:25.220 [main] JavaClientRpc.lambda$run$3 - Broadcasting transaction to participants
[INFO ] 10:11:25.438 [rpc-client-observation-pool-1] JavaClientRpc.actionToPerform - New state received : player=null, player1=O=PartyB, L=New York, C=US, player2=O=PartyA, L=London, C=GB, gameStatus=I
```

The board is represented with some ASCII art. Position addresses on the board are shown in square brackets, `[0]` is upper left, `[2]` upper right, `[6]` bottom left etc.

```
Game : gameBerty, State: INITIAL, Moves: 0, Hint: 'JavaClientRpc'
   [0] [1] [2]
      -   -
    | \ | / |
[3]   -   -   [5]
    | / | \ |
      -   -
   [6] [7] [8]
```

Players take it in turns to move and are prompted on their turn. During the placement phase of the game `State: PLACEMENT` you only need to specify a destination board address at the prompt, e.g. `5`.

Once the game enters the `MOVING` phase of the game you specify source and destination addresses separated by spaces e.g. `4 5`. The prompt above the board tells you if you are player `'O'` or player `'X'` on the board.

Finally you can insult or praise the other player by adding some optional free text to your move e.g. `4 5 I think I'm going to win!`.

This text appears in the hint above to the board. Keep it clean.

```
Game : gameBerty, State: MOVING, Moves: 12, Hint: ' Where's your cheese?'
   [0] [1] [2]
      - O - O
    | \ | / |
[3] O - X -   [5]
    | / | \ |
    X - X -
   [6] [7] [8]
[INFO ] 10:28:27.237 [main] JavaClientRpc.run - Next move 'O=PartyB, L=New York, C=US', you're token 'O'
Enter from dest cell and optional comment : 3 0 Never mind my cheese, I win!
[INFO ] 10:28:46.049 [main] JavaClientRpc.lambda$run$5 - Starting
[INFO ] 10:28:46.050 [main] JavaClientRpc.lambda$run$5 - Generating transaction based on new IOU.
[INFO ] 10:28:46.051 [main] JavaClientRpc.lambda$run$5 - Verifying contract constraints.
[INFO ] 10:28:46.058 [main] JavaClientRpc.lambda$run$5 - Signing transaction with our private key.
[INFO ] 10:28:46.063 [main] JavaClientRpc.lambda$run$5 - Gathering the counterparty's signature.
[INFO ] 10:28:46.066 [main] JavaClientRpc.lambda$run$5 - Collecting signatures from counterparties.
[INFO ] 10:28:46.066 [main] JavaClientRpc.lambda$run$5 - Starting
[INFO ] 10:28:46.149 [main] JavaClientRpc.lambda$run$5 - Verifying collected signatures.
[INFO ] 10:28:46.150 [main] JavaClientRpc.lambda$run$5 - Obtaining notary signature and recording transaction.
[INFO ] 10:28:46.156 [main] JavaClientRpc.lambda$run$5 - Requesting signature by notary service
[INFO ] 10:28:46.163 [main] JavaClientRpc.lambda$run$5 - Requesting signature by Notary service
[INFO ] 10:28:46.216 [main] JavaClientRpc.lambda$run$5 - Validating response from Notary service
[INFO ] 10:28:46.222 [main] JavaClientRpc.lambda$run$5 - Broadcasting transaction to participants
[INFO ] 10:28:46.236 [rpc-client-observation-pool-1] JavaClientRpc.actionToPerform - New state received : player=O=PartyB, L=New York, C=US, player1=O=PartyB, L=New York, C=US, player2=O=PartyA, L=London, C=GB, gameStatus=FINISHED, move=13)
Game : gameBerty, State: FINISHED, Moves: 13, Hint: ' Never mind my cheese, I win!'
   [0] [1] [2]
    O - O - O
    | \ | / |
[3]   - X -   [5]
    | / | \ |
    X - X -
   [6] [7] [8]
[INFO ] 10:28:46.273 [main] JavaClientRpc.lambda$run$5 - Done
```

Player `'O'` wins the game moving `3 0` having held their nerve after a provocative inquiry about the location of their cheese.

## BUGS

Plenty ...
* I've seen bottom row mills (rows of three) not win the game. Better test coverage of winning state required.
* Sometimes I think the Java input scanner is not being used correctly. So moves seem sometimes not to work. If this happens just kill and restart the client.

## TODO

* Better understand how to use Observables properly. The client could be improved upon somewhat.
* A game engine in the client for automated play.
* Restful interface for web clients.
* Better error messages.
* Testing, not enough tests on the state or flow. Coverage is not great, I can think of loads more to add :)

Paul
paul.hatcher@r3.com
