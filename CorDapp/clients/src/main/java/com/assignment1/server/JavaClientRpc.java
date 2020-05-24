package com.assignment1.server;

import com.assignment1.flow.TPMFlowCreate;
import com.assignment1.state.TPMState;
import com.google.common.collect.ImmutableList;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node.
 */
public class JavaClientRpc {

    private static final Logger logger = LoggerFactory.getLogger(JavaClientRpc.class);

    public static void main(String[] args) {
        //Get the node address to connect to, rpc username , rpc password via command line
        if (args.length != 4) throw new IllegalArgumentException("Usage: Client <node address> <gameId> <rpc username> <rpc password>");

        NetworkHostAndPort networkHostAndPort = NetworkHostAndPort.parse(args[0]);
        String rpcUsername = args[2];
        String rpcPassword = args[3];
        String gameId = args[1];

        /*get the client handle which has the start method
        Secure SSL connection can be established with the server if specified by the client.
        This can be configured by specifying the truststore path containing the RPC SSL certificate in the while creating CordaRPCClient instance.*/
        CordaRPCClient client = new CordaRPCClient(networkHostAndPort);

        //start method establishes conenction with the server, starts off a proxy handler and return a wrapper around proxy.
        CordaRPCConnection rpcConnection = client.start(rpcUsername, rpcPassword);

        try {
            //proxy is used to convert the client high level calls to artemis specific low level messages
            CordaRPCOps proxy = rpcConnection.getProxy();

            // Get a list of legal identities. Should have at least one.
            List<Party> parties = proxy.nodeInfo().getLegalIdentities();
            Party me = parties.get(0);
            System.out.println(String.format("Node legal identity %s", me));

            //hit the node to retrieve network map
            List<NodeInfo> nodes = proxy.networkMapSnapshot();
            logger.info("All the nodes available in this network", nodes);

            //hit the node to get snapshot and observable for TPMState
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(ImmutableList.of(me), null, ImmutableList.of(gameId));
            DataFeed<Vault.Page<TPMState>, Vault.Update<TPMState>> dataFeed = proxy.vaultTrackByCriteria(TPMState.class, queryCriteria);

             //this gives a snapshot of IOUState as of now. so if there are 11 IOUState as of now, this will return 11 IOUState objects
            Vault.Page<TPMState> snapshot = dataFeed.getSnapshot();

            // Subscribe to updates from query, after draining above ??
            dataFeed.getUpdates().subscribe(update -> update.getProduced().forEach(JavaClientRpc::actionToPerform));

            // If there are no states the game does not exist, ask to create.
            if (snapshot.getStates().isEmpty()) {
                System.out.print("No game found, create? [y/n] : ");
                if ('y' == getChar()) {

                    System.out.print("Enter opponent name : ");
                    final String opponent = getString();

                    // Create game
                    final Set<Party> opponents = proxy.partiesFromName(opponent, false);
                    if (opponents.isEmpty()) {
                        final String msg = String.format("Failed to find opponent '%s'", opponent);
                        logger.error(msg);
                        throw new IllegalArgumentException(msg);
                    }

                    System.out.println(String.format("Creating game '%s'", gameId));
                    Observable<String> ob = proxy.startTrackedFlowDynamic(TPMFlowCreate.Initiator.class, opponents.iterator().next(), "JavaClientRpc", gameId).getProgress();
                    ob.toBlocking().subscribe(hint -> {
                        logger.info(hint);
                    });
                }

                // Hopefully will have tried to create a game, query again.
                //snapshot = dataFeed.getSnapshot();
                //if (snapshot.getStates().isEmpty()) {
                //    final String msg = String.format("Failed to find any games");
                //    logger.error(msg);
                //    throw new IllegalArgumentException(msg);
                //}
            } else {

                // call a method for each IOUState
                snapshot.getStates().forEach(JavaClientRpc::actionToPerform);
            }

            //this returns an observable on IOUState
            // Observable<Vault.Update<TPMState>> updates = dataFeed.getUpdates();

            //updates.subscribe(update -> update.getProduced().forEach(JavaClientRpc::actionToPerform));

            //perform certain action for each update to IOUState
            // updates.toBlocking().subscribe(update -> update.getProduced().forEach(JavaClientRpc::actionToPerform));

            // proxy.shutdown();
        } finally {
            //rpcConnection.notifyServerAndClose();
        }
    }

    private static char getChar() {
        return getString().charAt(0);
    }

    private static String getString() {
        Scanner scanner = new Scanner(System.in);
        return scanner.next();
    }

    private static int getInt() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            if (scanner.hasNextInt()) {
                return scanner.nextInt();
            } else {
                scanner.next();
            }
        }
        return -1;
    }

    /**
     * @param stateRef
     */
    private static void actionToPerform(StateAndRef<TPMState> stateRef) {
        //logger.info("{}", state.getState().getData());
        TPMState state = stateRef.getState().getData();
        System.out.println(String.format("Game : %s, State: %s, Moves: %s, Hint: '%s'", state.getGameId(), state.getGameStatus(), state.getMoves(), state.getMoveHint()));
        for (int i=0; i<TPMState.BOARD_WIDTH; ++i) {
            if (1 == i) {
                System.out.println("| \\ | / |");
            } else if (2 == i) {
                System.out.println("| / | \\ |");
            }
            for (int j=0; j<TPMState.BOARD_WIDTH; ++j) {
                if (j > 0) {
                    System.out.print(" - ");
                }
                TPMState.Token t = state.getToken(j,i);
                System.out.print(null == t ? ' ' : (TPMState.Token.PLAYER1 == t ? 'O' : 'X'));
            }
            System.out.println();
        }
    }
}
