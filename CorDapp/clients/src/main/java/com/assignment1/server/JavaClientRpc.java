package com.assignment1.server;

import com.assignment1.flow.TPMFlowCreate;
import com.assignment1.flow.TPMFlowMove;
import com.assignment1.state.TPMBoard;
import com.assignment1.state.TPMState;
import com.google.common.collect.ImmutableList;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.contracts.StateAndRef;
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
import rx.Subscription;
import rx.observables.BlockingObservable;

import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node.
 */
public class JavaClientRpc {

    public static void main(String[] args) {
        //Get the node address to connect to, rpc username , rpc password via command line
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: Client <node address> <gameId> <rpc username> <rpc password>");
        }

        NetworkHostAndPort networkHostAndPort = NetworkHostAndPort.parse(args[0]);
        String rpcUsername = args[2];
        String rpcPassword = args[3];
        String gameId = args[1];

        new JavaClientRpcClass(networkHostAndPort, rpcUsername, rpcPassword, gameId).run();
    }

    private static class JavaClientRpcClass {
        private final Logger logger = LoggerFactory.getLogger(JavaClientRpc.class);

        private final NetworkHostAndPort networkHostAndPort;
        private final String rpcUsername;
        private final String rpcPassword;
        private final String gameId;
        private TPMState stateLast;
        private Semaphore semaphore;
        boolean error;
        Scanner scanner;

        JavaClientRpcClass(
                NetworkHostAndPort networkHostAndPort,
                String rpcUsername,
                String rpcPassword,
                String gameId) {
            this.networkHostAndPort = networkHostAndPort;
            this.rpcUsername = rpcUsername;
            this.rpcPassword = rpcPassword;
            this.gameId = gameId;
            this.semaphore = new Semaphore(0);
            this.error = false;
            this.scanner = new Scanner(System.in);
        }

        public void run() {
        /*get the client handle which has the start method
        Secure SSL connection can be established with the server if specified by the client.
        This can be configured by specifying the truststore path containing the RPC SSL certificate in the while creating CordaRPCClient instance.*/
            CordaRPCClient client = new CordaRPCClient(networkHostAndPort);

            //start method establishes conenction with the server, starts off a proxy handler and return a wrapper around proxy.
            CordaRPCConnection rpcConnection = client.start(rpcUsername, rpcPassword);

            //proxy is used to convert the client high level calls to artemis specific low level messages
            CordaRPCOps proxy = rpcConnection.getProxy();

            // Get a list of legal identities. Should have at least one.
            List<Party> parties = proxy.nodeInfo().getLegalIdentities();
            final Party me = parties.get(0);
            logger.info(String.format("Node legal identity %s", me));

            //hit the node to retrieve network map
            List<NodeInfo> nodes = proxy.networkMapSnapshot();
            logger.info("All the nodes available in this network", nodes);

            //hit the node to get snapshot and observable for TPMState
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(ImmutableList.of(me), null, ImmutableList.of(gameId));
            DataFeed<Vault.Page<TPMState>, Vault.Update<TPMState>> dataFeed = proxy.vaultTrackByCriteria(TPMState.class, queryCriteria);

            // This gives a snapshot of TPMState as of now. so if there are 11 TPMState as of now, this will return 11 TPMState objects
            Vault.Page<TPMState> snapshot = dataFeed.getSnapshot();

            // Subscribe to updates from query, after draining above ??
            Observable<Vault.Update<TPMState>> updates = dataFeed.getUpdates();
            Subscription subscription = updates.subscribe(update -> update.getProduced().forEach(update2 -> {actionToPerform(update2);}));

            try {
                // If there are no states the game does not exist, ask to create.
                if (!snapshot.getStates().isEmpty()) {
                    // Show the current state. State could be set here.
                    snapshot.getStates().forEach(update -> {
                        actionToPerform(update);
                    });
                } else {
                    final String msg = String.format("Game '%s' not found, create? [y/n] : ", gameId);
                    logger.info(msg);
                    System.out.print(msg);
                    if ('y' == getChar()) {

                        System.out.print("Enter opponent name : ");
                        final String opponent = getString();

                        // Get an opponent from the name.
                        final Set<Party> opponents = proxy.partiesFromName(opponent, false);
                        if (opponents.isEmpty()) {
                            final String msge = String.format("Failed to find opponent '%s'", opponent);
                            logger.error(msge);
                            return;
                        }

                        final Party party = opponents.iterator().next();

                        // Create game and wait for completion.
                        logger.info(String.format("Creating game '%s' with opponent '%s'", gameId, party));
                        Observable<String> ob = proxy.startTrackedFlowDynamic(TPMFlowCreate.Initiator.class, party, "JavaClientRpc", gameId).getProgress();

                        // TODO There must be a better way of detecting an error occurred inside blocking subscription.
                        error = false;
                        ob.toBlocking().subscribe(progress -> {
                            logger.info(progress);
                        }, err -> {
                            logger.error(err.getMessage());
                            error = true;
                        });

                        // Just get out the bus.
                        if (error) {return;}

                    } else {
                        // finally will shut everything down.
                        return;
                    }
                    // If we have a separate subscription for the state, in addition to the display, then could block here and wait.
                }

                // Wait for update.
                semaphore.tryAcquire(10, TimeUnit.SECONDS);

                // TODO I don't like this at all. Must be a more elegant way.
                TPMState state = null;
                synchronized (this) {
                    state = stateLast;
                }

                // Enter main game loop making moves. We need to work out if we're next or not.
                while (TPMBoard.GameStatus.FINISHED != state.getGameStatus()) {
                    // TPMState.player is the player who made the last move. However in initial state they created the board.
                    if ((state.getGameStatus() == TPMBoard.GameStatus.INITIAL) && me.equals(state.getPlayer1()) ||
                       ((state.getGameStatus() != TPMBoard.GameStatus.INITIAL) && !me.equals(state.getPlayer()))) {

                        // Our move.
                        logger.info(String.format("Next move '%s', you're token '%c'", me, state.getPlayer1().equals(me) ? 'O' : 'X'));

                        int src = -1;
                        int dst = -1;
                        if (state.getGameStatus() != TPMBoard.GameStatus.MOVING) {
                            System.out.print("Enter placement cell and optional comment : ");
                            dst = getInt();
                        } else {
                            System.out.print("Enter from dest cell and optional comment : ");
                            src = getInt();
                            dst = getInt();
                        }
                        final String hint = getString();

                        // Now try a move ! Block until move has completed.
                        Observable<String> ob = proxy.startTrackedFlowDynamic(TPMFlowMove.Initiator.class, gameId, hint, src, dst).getProgress();

                        error = false;
                        ob.toBlocking().subscribe(progress -> {
                            logger.info(progress);
                        }, err -> {
                            logger.error(err.getMessage());
                            error=true;
                        });

                        if (error) {continue;}

                    } else {
                        logger.info(String.format("Waiting for move ..."));
                    }

                    // Wait for state to change.
                    semaphore.tryAcquire(10, TimeUnit.SECONDS);

                    // TODO I don't like this at all.
                    synchronized (this) {
                        state = stateLast;
                    }
                }
            } catch (InterruptedException e) {
                logger.error(e.toString());
            } finally {
                // Not sure how to gracefully close. Seems to cause issues calling this.
                // It's not enough to simply fall off the end here as that blocks.
                subscription.unsubscribe();
                rpcConnection.notifyServerAndClose();
            }
        }

        private char getChar() {
            return getString().charAt(0);
        }

        private String getString() {
            return scanner.nextLine();
        }

        private int getInt() {
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
        private void actionToPerform(StateAndRef<TPMState> stateRef) {
            TPMState state = stateRef.getState().getData();

            // Yuck !
            synchronized (this) {
                stateLast = state;
            }

            logger.info(String.format("New state received : %s", state));
            System.out.println(String.format("Game : %s, State: %s, Moves: %s, Hint: '%s'", state.getGameId(), state.getGameStatus(), state.getMoves(), state.getMoveHint()));
            printBoard(state);

            // Release semaphore after we've output the board.
            semaphore.tryAcquire();
            semaphore.release();
        }

        private void printBoard(TPMState state) {
            System.out.println("   [0] [1] [2]");
            for (int i = 0; i < TPMBoard.BOARD_WIDTH; ++i) {
                if (1 == i) {
                    System.out.println("    | \\ | / |");
                    System.out.print("[3] ");
                } else if (2 == i) {
                    System.out.println("    | / | \\ |");
                    System.out.print("    ");
                } else {
                    System.out.print("    ");
                }
                for (int j = 0; j < TPMBoard.BOARD_WIDTH; ++j) {
                    if (j > 0) {
                        System.out.print(" - ");
                    }
                    TPMBoard.Token t = state.getBoard().getToken(j, i);
                    System.out.print(null == t ? ' ' : (TPMBoard.Token.PLAYER1 == t ? 'O' : 'X'));
                }
                if (1 == i) {
                    System.out.println(" [5]");
                } else {
                    System.out.println();
                }
            }
            System.out.println("   [6] [7] [8]");
        }
    }
}
