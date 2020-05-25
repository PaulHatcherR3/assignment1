package com.assignment1.state;

import com.assignment1.contract.TPMContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;

import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.Arrays;
import java.util.List;

/**
 * The state object recording Three Person's Morris (TPM)
 *
 * A state must implement [ContractState] or one of its descendants.
 */
@BelongsToContract(TPMContract.class)
public class TPMState implements LinearState {

    private final TPMBoard board;
    private final Party player;
    private final Party player1;
    private final Party player2;
    private final int moves;
    private final String moveHint;
    private final String gameStatusHint;
    private final UniqueIdentifier linearId;

    /**
     * @param board The state of the board in play. Board is fixed size single dimension.
     * @param player The player who made the move to this state.
     * @param player1 The player in the game, they made the first move.
     * @param player2 The second player in the game.
     * @param moves The move counter, on issue this is set to 1.
     * @param gameStatusHint The game status hint.
     * @param gameStatusHint Human readable class set string as a helpful hint.
     * @param moveHint Free set string set by the move, maybe a popular local insult.
     */
    @ConstructorForDeserialization
    public TPMState(TPMBoard board,
                    Party player,
                    Party player1,
                    Party player2,
                    int moves,
                    String gameStatusHint,
                    String moveHint,
                    UniqueIdentifier linearId)
    {
        this.board = board;
        this.player = player;
        this.player1 = player1;
        this.player2 = player2;
        this.moves = moves;
        this.gameStatusHint = gameStatusHint;
        this.moveHint = moveHint;
        this.linearId = linearId;
    }

    /*
     * The constructor used when creating a new state for a move.
     */
    private TPMState(
                    TPMBoard board,
                    Party player,
                    Party player1,
                    Party player2,
                    int moves,
                    String moveHint,
                    UniqueIdentifier linearId,
                    int src,
                    int dst) {
        // Maybe call above constructor??
        this.board = board;
        this.player = player;
        this.player1 = player1;
        this.player2 = player2;
        this.moves = moves;
        this.moveHint = moveHint;
        this.linearId = linearId;

        // Easier to refer to player1 and player2 in hints at the moment.
        final String playerHint = player.equals(player1) ? "Player1" : (player.equals(player2) ? "Player2" : player.toString());

        final TPMBoard.GameStatus gameStatus = getBoard().getGameStatus();

        if ( TPMBoard.GameStatus.INITIAL == gameStatus) {
            this.gameStatusHint = "Initial";
        }
        else if ( TPMBoard.GameStatus.FINISHED == gameStatus) {
            if (board.gameOver(TPMBoard.Token.PLAYER1)) {
                this.gameStatusHint = String.format("Player1 won in %d moves", moves);
            } else if (board.gameOver(TPMBoard.Token.PLAYER2)) {
                this.gameStatusHint = String.format("Player2 won in %d moves", moves);
            } else {
                this.gameStatusHint = "Game has finished without a winner!";
            }
        } else if (TPMBoard.GameStatus.MOVING == gameStatus) {
            this.gameStatusHint = String.format("%s moved from %d to %d", playerHint, src, dst);
        } else if (TPMBoard.GameStatus.PLACEMENT == gameStatus) {
            this.gameStatusHint = String.format("%s placed at %d", playerHint, dst);
        } else {
            this.gameStatusHint = "Unknown board status";
        }
    }

    /* Initialize a new board.
     * @param player1 The player in the game, they made the first move.
     * @param player2 The second player in the game.
     */
    public TPMState(Party player1, Party player2, String createHint, String gameId) {
        this.board = new TPMBoard();
        this.player = null;
        this.player1 = player1;
        this.player2 = player2;
        this.gameStatusHint = "Initial";
        this.moveHint = createHint;
        this.moves = 0;
        this.linearId = new UniqueIdentifier(gameId);
    }

    public String getGameId() {
        return getLinearId().getExternalId();
    }

    public TPMBoard getBoard() {
        return board;
    }

    public Party getPlayer() { return player; }

    public Party getPlayer1() {
        return player1;
    }

    public Party getPlayer2() {
        return player2;
    }

    public TPMBoard.GameStatus getGameStatus() {
        return getBoard().getGameStatus();
    }

    public String getGameStatusHint() {
        return gameStatusHint;
    }

    public String getMoveHint() {
        return moveHint;
    }

    public int getMoves() {
        return moves;
    }

    // Make sure there are the required number of tokens in play and they are in valid positions.
    public void checkInvariants() {
        requireThat(require -> {

            // Make sure that player that set state was supposed to.
            // In checkMove we catch the case where player1 made the move but moved player2s token.
            if (null != getPlayer()) {
                // If move is odd then it was player1 that proposed the state, else player2.
                require.using("Expected Player2 to make move", (getPlayer().equals(getPlayer1())) || ((getPlayer().equals(getPlayer2())) && (0 == (getMoves() % 2))));
                require.using("Expected Player1 to make move", (getPlayer().equals(getPlayer2())) || ((getPlayer().equals(getPlayer1())) && (0 != (getMoves() % 2))));
            }

            require.using("Expected 3 tokens in play for player1", getBoard().checkInvariants(TPMBoard.Token.PLAYER1));
            require.using("Expected 3 tokens in play for player2", getBoard().checkInvariants(TPMBoard.Token.PLAYER2));
            return null;
        });
    }

    // Make sure that new state is valid from this current state.
    // Used in the contract to check move is valid. I think this is a better place to code this.
    // It is invariant to the contract, it's the rules of the game, which form a _part_ of the contract.
    public void checkMove(TPMState stateNew) {

        requireThat(require -> {
            // Make sure that the new state is the same game, must have same Id.
            require.using("Next state is from a different game", getLinearId().equals(stateNew.getLinearId()));
            require.using("Moves are not contiguous", ((getMoves() + 1) == stateNew.getMoves()));
            require.using("Game is over", !getBoard().gameOver());

            TPMBoard.TPMMove move = getBoard().getMove(stateNew.getBoard());

            // Check that the move was valid.
            if (move.getError()) {
                require.using(move.getErrorHint(), false);
            }
            
            // Must have moved something.
            require.using("No destination move found", null != move.getDstToken());

            // Make sure that the destination token is for the correct player.
            require.using("Expected Player2 to make move", (TPMBoard.Token.PLAYER2 == move.getDstToken()) || (stateNew.getPlayer().equals(getPlayer1())));
            require.using("Expected Player1 to make move", (TPMBoard.Token.PLAYER1 == move.getDstToken()) || (stateNew.getPlayer().equals(getPlayer2())));

            // If there are pieces to play then in initial placement phase.
            if ((0 != getBoard().getPlayer1Tokens()) || (0 != getBoard().getPlayer2Tokens())) {

                // Make sure boards are not identical.
                require.using("Token moved during initial phase", null == move.getSrcToken());

                // Make sure that the found token was taken from the correct pool.
                require.using("Player2 token mismatch",  (TPMBoard.Token.PLAYER2 == move.getDstToken()) || ((TPMBoard.Token.PLAYER1 == move.getDstToken()) && ((getBoard().getPlayer1Tokens()-1) == stateNew.getBoard().getPlayer1Tokens())));
                require.using("Player1 token mismatch",  (TPMBoard.Token.PLAYER1 == move.getDstToken()) || ((TPMBoard.Token.PLAYER2 == move.getDstToken()) && ((getBoard().getPlayer2Tokens()-1) == stateNew.getBoard().getPlayer2Tokens())));

            } else {

                // Something must have moved.
                require.using("No source token was found during play", null != move.getSrcToken());

                // Make sure that the tokens are the same.
                require.using("Tokens must be the same player", move.getSrcToken().equals(move.getDstToken()));

                // Check for a valid move on the board.
                require.using("Invalid move", getBoard().legalMove(move.getSrc(), move.getDst()));
            }
            return null;
        });
    }

    // We also need to transition a state given a move. The move specifies, source, dest and player.
    public TPMState move(Party player, String moveHint, int src, int dst) {

        TPMBoard boardNew;

        // We don't check much else here as any problems should be caught above when checking the contract.
        if (getPlayer1().equals(player)) {
            boardNew = getBoard().move(src,dst,TPMBoard.Token.PLAYER1);
        } else if (getPlayer2().equals(player)) {
            boardNew = getBoard().move(src,dst,TPMBoard.Token.PLAYER2);
        } else {
            boardNew = null;
        }

        return new TPMState(
            boardNew,
            player,
            getPlayer1(),
            getPlayer2(),
            getMoves()+1,
            moveHint,
            getLinearId(),
            src,dst
        );
    }

    @Override public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(player1, player2);
    }

    @Override
    public String toString() {
        return String.format("player=%s, player1=%s, player2=%s, gameStatus=%s, move=%s)", player, player1, player2, getBoard().getGameStatus(), moves);
    }
}