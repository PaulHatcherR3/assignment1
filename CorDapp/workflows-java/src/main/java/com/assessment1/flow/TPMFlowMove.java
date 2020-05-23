package com.assessment1.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.assessment1.contract.TPMContract;
import com.assessment1.state.TPMState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * Flow for TPM to perform a move on the TPMState object.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class TPMFlowMove {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String gameId;
        private final String moveHint;
        private final int src;
        private final int dst;

        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new IOU.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public Initiator(String gameId,String moveHint, int src, int dst) {
            this.gameId = gameId;
            this.moveHint = moveHint;
            this.src = src;
            this.dst = dst;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // This is for a move in the game. So we need as input the game UniqueId and a proposed move.

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            // Fetch the current state from the vault. We query using the foreign key for the LinearState on the ledger.
            Party me = getOurIdentity();
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, null, ImmutableList.of(gameId));
            List<StateAndRef<TPMState>> states = getServiceHub().getVaultService().queryBy(TPMState.class, queryCriteria).getStates();

            // Should be one, should be same parties (we queried for!). Not sure if this is overkill. Could just test and then throw exception?
            requireThat(require -> {
                require.using("Failed to find game on ledger", states.size() != 0);
                require.using("Should only be one game on ledger", states.size() == 1);
                return null;
            });

            // Rehydrate current state from Vault.
            TPMState state = states.get(0).getState().getData();

            // Now make the move. stateNext will be null if move is invalid. Bit lame since no hint why on failure.
            TPMState stateNew = state.move(me, moveHint, src, dst);

            // Sanity check game and players, but we queried on these, so should be correct.
            requireThat(require -> {
                require.using("GameId mismatch", state.getLinearId().getExternalId().equals(gameId));
                require.using("Node Party should be player1 or player2", state.getPlayer1().equals(me) || state.getPlayer2().equals(me));
                require.using("Move is invalid", null != stateNew);
                return null;
            });

            // We have correct game on ledger, we've made a move, all good.
            // This is interesting, what are we sending across here ?
            // New state makes sense, it's the proposed outcome, but what about the input?
            final Command<TPMContract.Commands.Move> txCommand = new Command<>(
                    new TPMContract.Commands.Move(),
                    ImmutableList.of(state.getPlayer1().getOwningKey(), state.getPlayer2().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(states.get(0))
                    .addOutputState(stateNew, TPMContract.ID)
                    .addCommand(txCommand);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(state.getPlayer1().equals(me) ? state.getPlayer2() : state.getPlayer1());
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(otherPartySession)));
        }
    }

    // This is the receiving party side for the above.
    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartySession;

        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {

                    // Query local vault for the input state.
                    requireThat(require -> {
                        // TODO : How can we validate the input here? Do we need to?
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be a board transaction.", output instanceof TPMState);
                        // Any other constraints that are not covered in the contract for this party ??
                        // TPMState iou = (TPMState) output;
                        // require.using("I won't accept IOUs with a value over 100.", iou.getValue() <= 100);
                        return null;
                    });
                }
            }

            final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
        }
    }
}
