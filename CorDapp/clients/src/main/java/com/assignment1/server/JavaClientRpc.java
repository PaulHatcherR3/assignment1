package com.assignment1.server;

import com.assignment1.state.TPMState;
import com.google.common.collect.ImmutableList;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.contracts.StateAndRef;
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

        //proxy is used to convert the client high level calls to artemis specific low level messages
        CordaRPCOps proxy = rpcConnection.getProxy();

        //hit the node to retrieve network map
        List<NodeInfo> nodes = proxy.networkMapSnapshot();
        logger.info("All the nodes available in this network", nodes);

        //hit the node to get snapshot and observable for TPMState
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, null, ImmutableList.of(gameId));
        DataFeed<Vault.Page<TPMState>, Vault.Update<TPMState>> dataFeed = proxy.vaultTrackByCriteria(TPMState.class, queryCriteria);

        //this gives a snapshot of IOUState as of now. so if there are 11 IOUState as of now, this will return 11 IOUState objects
        Vault.Page<TPMState> snapshot = dataFeed.getSnapshot();

        // call a method for each IOUState
        snapshot.getStates().forEach(JavaClientRpc::actionToPerform);

        //this returns an observable on IOUState
        // Observable<Vault.Update<TPMState>> updates = dataFeed.getUpdates();

        //updates.subscribe(update -> update.getProduced().forEach(JavaClientRpc::actionToPerform));

        System.out.print("Chicken dinner? : ");

        try {
            int i = System.in.read();
            System.out.println("You typed:" + i);
        } catch (IOException e) {

        }

        //perform certain action for each update to IOUState
        // updates.toBlocking().subscribe(update -> update.getProduced().forEach(JavaClientRpc::actionToPerform));

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
                System.out.println("|\\  |  /|");
                System.out.println("| \\ | / |");
                System.out.println("|  \\|/  |");
            } else if (2 == i) {
                System.out.println("|  /|\\  |");
                System.out.println("| / | \\ |");
                System.out.println("|/  |  \\|");
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
