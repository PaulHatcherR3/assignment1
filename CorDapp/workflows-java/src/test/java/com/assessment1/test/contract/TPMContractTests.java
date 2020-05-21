package com.assessment1.test.contract;

import com.assessment1.contract.TPMContract;
import com.assessment1.state.TPMState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static java.util.Arrays.asList;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class TPMContractTests {
    static private final MockServices ledgerServices = new MockServices(asList("com.assessment1.contract", "com.assessment1.flow"));
    static private final TestIdentity megaCorp = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    static private final TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "London", "GB"));

    @Test
    public void transactionMustIncludeCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
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
                tx.input(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
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
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
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
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
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
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
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
                tx.output(TPMContract.ID, new TPMState(megaCorp.getParty(), megaCorpDupe.getParty()));
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
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Move());
                tx.failsWith("Move should have one input state.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionInvalidMove() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
                tx.output(TPMContract.ID, new TPMState(miniCorp.getParty(), megaCorp.getParty()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Move());
                tx.failsWith("Proposed move is invalid.");
                return null;
            });
            return null;
        }));
    }

/*
    @Test
    public void cannotCreateNegativeValueIOUs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(TPMContract.ID, new TPMState(-1, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new TPMContract.Commands.Create());
                tx.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        }));
    }
*/
}