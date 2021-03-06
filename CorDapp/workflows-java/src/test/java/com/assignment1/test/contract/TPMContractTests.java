package com.assignment1.test.contract;

import com.assignment1.contract.TPMContract;
import com.assignment1.state.TPMState;
import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static java.util.Arrays.asList;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class TPMContractTests {
    static private final MockServices ledgerServices = new MockServices(asList("com.assignment1.contract", "com.assignment1.flow"));
    static private final TestIdentity megaCorp = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    static private final TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "London", "GB"));

    @Test
    public void transactionMustIncludeCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(),null,""));
                tx.fails();
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Create());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(),null, "123"));
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,"123"));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Create());
                tx.failsWith("On creation there should be no input state.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,"123"));
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,"123"));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Create());
                tx.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustSignTransaction1() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,""));
                tx.command(miniCorp.getPublicKey(), new TPMContract.Commands.Create());
                tx.failsWith("All of the players must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustSignTransaction2() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,""));
                tx.command(megaCorp.getPublicKey(), new TPMContract.Commands.Create());
                tx.failsWith("All of the players must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionSameParties() {
        final TestIdentity megaCorpDupe = new TestIdentity(megaCorp.getName(), megaCorp.getKeyPair());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TPMContract.ID, new TPMState(megaCorp.getParty(), megaCorpDupe.getParty(), null,""));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Create());
                tx.failsWith("The two players cannot be the same entity.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMoveWithNoInput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,""));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Move());
                tx.failsWith("Move should have one input state.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionInvalidMoveState() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,"123"));
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,"123"));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Move());
                tx.failsWith("Next state is from a different game");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionInvalidMoveSrcInvariant() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                // Make a couple of moves, one of which will break the invariant properties of board.
                TPMState stateOld = new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,"123");
                TPMState stateNxt = stateOld.move(miniCorp.getParty(), null,1,2);
                stateOld = stateNxt;
                stateNxt = stateOld.move(megaCorp.getParty(), null,2,2);

                tx.input(TPMContract.ID, stateOld);
                tx.output(TPMContract.ID, stateNxt);
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Move());
                tx.failsWith("Expected 3 tokens in play for player1");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionInvalidMoveSrcGameOver() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                // Make a couple of moves, one of which will break the invariant properties of board.
                TPMState stateOld = new TPMState(miniCorp.getParty(), megaCorp.getParty(), null,"123");
                TPMState stateNxt = stateOld.move(miniCorp.getParty(),null,-1,1);
                stateOld = stateNxt;
                stateNxt = stateOld.move(megaCorp.getParty(), null,-1,2);
                stateOld = stateNxt;
                stateNxt = stateOld.move(miniCorp.getParty(),null,-1,4);
                stateOld = stateNxt;
                stateNxt = stateOld.move(megaCorp.getParty(),null,-1,5);
                stateOld = stateNxt;
                stateNxt = stateOld.move(miniCorp.getParty(),null,-1,7);
                stateOld = stateNxt;
                stateNxt = stateOld.move(megaCorp.getParty(),null,-1,8);

                tx.input(TPMContract.ID, stateOld);
                tx.output(TPMContract.ID, stateNxt);
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Move());
                tx.failsWith("Game is over");
                return null;
            });
            return null;
        }));
    }

}