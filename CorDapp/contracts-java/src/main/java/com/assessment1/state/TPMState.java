package com.assessment1.state;

import com.assessment1.contract.TPMContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.util.Arrays;
import java.util.List;

/**
 * The state object recording Three Person's Morris (TPM)
 *
 * A state must implement [ContractState] or one of its descendants.
 */
@BelongsToContract(TPMContract.class)
public class TPMState implements ContractState {

    @CordaSerializable
    private enum Token {PLAYER1, PLAYER2};
    private final int player1Tokens;
    private final int player2Tokens;
    private final Token[] board;
    private final Party player1;
    private final Party player2;
    private final int moves;

    /**
     * @param player1Tokens Player1 Pieces off board.
     * @param player2Tokens Player2 Pieces off board.
     * @param board The state of the board in play. Board is fixed size single dimension.
     * @param player1 The player in the game, they made the first move.
     * @param player2 The second player in the game.
     * @param moves The move counter, on issue this is set to 1.
     */
    @ConstructorForDeserialization
    public TPMState(int player1Tokens, int player2Tokens,
                    Token[] board,
                    Party player1,
                    Party player2,
                    int moves)
    {
        this.player1Tokens = player1Tokens;
        this.player2Tokens = player2Tokens;
        this.board = board;
        this.player1 = player1;
        this.player2 = player2;
        this.moves = moves;
    }

    /* Initialize a new board.
     * @param player1 The player in the game, they made the first move.
     * @param player2 The second player in the game.
     */
    public TPMState(Party player1, Party player2) {
        this.player1Tokens = 3;
        this.player2Tokens = 3;
        this.board = new Token[3*3];
        this.player1 = player1;
        this.player2 = player2;
        this.moves = 0;
    }

    public int getPlayer1Tokens() {
        return player1Tokens;
    }

    public int getPlayer2Tokens() {
        return player2Tokens;
    }

    public Token[] getBoard() {
        return board;
    }

    public Party getPlayer1() {
        return player1;
    }

    public Party getPlayer2() {
        return player2;
    }

    public int getMoves() {
        return moves;
    }

    // Make sure there are the required number of tokens in play and they are in valid positions.
    public boolean checkInvariants() {
        // Sum of tokens in counters and on board should equal six.
        int p1pt = player1Tokens;
        int p2pt = player2Tokens;
        for (Token t : board) {
            if (null == t)               {;}
            else if (Token.PLAYER1 == t) {++p1pt;}
            else if (Token.PLAYER2 == t) {++p2pt;}
        }
        return (3 == p1pt) && (3 == p2pt);
    }

    // Make sure that new state is valid from this current state. Shouldn't this have been checked below?
    public boolean checkMove(TPMState newState) {
        // If there are pieces to play then in initial placement phase.
        if ((0 != player1Tokens) || (0 != player1Tokens)) {

        } else {
            // We're in play.
        }

        return true;
    }

    // We also need to transition a state given a move. The move specifies, source, dest and player.
    public boolean move(int src, int dst, Party player) {
        // player1 always goes first, so should be an even move.

        return true;
    }

    // @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(player1, player2);
    }

    @Override
    public String toString() {
        return String.format("TPMState(player1=%s, player2=%s, move=%s)", player1, player2, moves);
    }
}