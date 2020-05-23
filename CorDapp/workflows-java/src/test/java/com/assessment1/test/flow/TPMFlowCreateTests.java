package com.assessment1.test.flow;

import com.assessment1.flow.TPMFlowCreate;
import com.assessment1.state.TPMState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.*;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TPMFlowCreateTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.assessment1.contract"),
                TestCordapp.findCordapp("com.assessment1.flow"))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.registerInitiatedFlow(TPMFlowCreate.Acceptor.class);
        }
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        TPMFlowCreate.Initiator flow = new TPMFlowCreate.Initiator(b.getInfo().getLegalIdentities().get(0), null,"123");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        TPMFlowCreate.Initiator flow = new TPMFlowCreate.Initiator(b.getInfo().getLegalIdentities().get(0),null,"123");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        TPMFlowCreate.Initiator flow = new TPMFlowCreate.Initiator(b.getInfo().getLegalIdentities().get(0), null,"123");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutput() throws Exception {
        Integer iouValue = 1;
        TPMFlowCreate.Initiator flow = new TPMFlowCreate.Initiator(b.getInfo().getLegalIdentities().get(0), null,"123");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);

            TPMState recordedState = (TPMState) txOutputs.get(0).getData();
            assertEquals(3, recordedState.getPlayer1Tokens());
            assertEquals(3, recordedState.getPlayer2Tokens());
            assertEquals(0, recordedState.getMoves());
            TPMState.Token board[] = recordedState.getBoard();
            for (TPMState.Token t : board) {
                assertNull(t);
            }
            assertEquals(recordedState.getPlayer1(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(recordedState.getPlayer2(), b.getInfo().getLegalIdentities().get(0));
        }
    }

    @Test
    public void flowRecordsTheCorrectStateInBothPartiesVaults() throws Exception {
        Integer iouValue = 1;
        TPMFlowCreate.Initiator flow = new TPMFlowCreate.Initiator(b.getInfo().getLegalIdentities().get(0), null,"123");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        future.get();

        // We check the recorded IOU in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.transaction(() -> {
                List<StateAndRef<TPMState>> states = node.getServices().getVaultService().queryBy(TPMState.class).getStates();
                assertEquals(1, states.size());
                TPMState recordedState = states.get(0).getState().getData();
                assertEquals(3, recordedState.getPlayer1Tokens());
                assertEquals(3, recordedState.getPlayer2Tokens());
                assertEquals(0, recordedState.getMoves());
                TPMState.Token board[] = recordedState.getBoard();
                for (TPMState.Token t : board) {
                    assertNull(t);
                }
                assertEquals(recordedState.getPlayer1(), a.getInfo().getLegalIdentities().get(0));
                assertEquals(recordedState.getPlayer2(), b.getInfo().getLegalIdentities().get(0));
                return null;
            });
        }
    }
}