package com.assignment1.contract;

import com.assignment1.state.TPMState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [TPMtate], which in turn encapsulates an [TPM].
 *
 * For a new [TPM] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [TPM].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
public class TPMContract implements Contract {
    public static final String ID = "com.assignment1.contract.TPMContract";

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {

        // Fetch the command and do common checks.
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);

        requireThat(require -> {
            // Generic constraints around the transaction.
            require.using("Only one output state should be created.",tx.getOutputs().size() == 1);
            final TPMState out = tx.outputsOfType(TPMState.class).get(0);
            require.using("The two players cannot be the same entity.", !out.getPlayer1().equals(out.getPlayer2()));
            require.using("All of the players must be signers.",
                    command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

            // Game specific invariants on output only. Will raise exception on failure.
            out.checkInvariants();

            return null;
        });

        // We have two commands, Create and Move, so check each separately.
        if (command.getValue() instanceof Commands.Create) {

            requireThat(require -> {
                // For creation there should be no input state.
                require.using("On creation there should be no input state.", tx.getInputs().size() == 0);
                // There should be a full set of available tokens as well.
                // There should also be zero moves.
                return null;
            });

        } else if (command.getValue() instanceof Commands.Move) {

            requireThat(require -> {
                // For move there should be one input, of the right type.
                require.using("Move should have one input state.", tx.getInputs().size() == 1);
                final TPMState in  = tx.inputsOfType(TPMState.class).get(0);
                final TPMState out = tx.outputsOfType(TPMState.class).get(0);

                // Make sure that the proposed move is a valid one.
                // This method will throw exceptions giving details of the move violation.
                in.checkMove(out);

                return null;
            });

        } else {
            // Thanks to https://training.corda.net/first-code/solution-contract !
            throw new IllegalArgumentException("Unknown Command : " + command.getValue());
        }
    }

    /**
     * We have two commands, Create and Move.
     */
    public interface Commands extends CommandData {
        class Create implements Commands {};
        class Move implements Commands {};
    };
}