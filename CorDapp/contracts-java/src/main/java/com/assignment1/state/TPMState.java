package com.assignment1.state;

import com.assignment1.contract.TPMContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * The state object recording Three Person's Morris (TPM)
 *
 * A state must implement [ContractState] or one of its descendants.
 */
@BelongsToContract(TPMContract.class)
public class TPMState implements LinearState {

    @CordaSerializable
    public enum Token {PLAYER1, PLAYER2};
    private static final int BOARD_WIDTH=3;
    private final int player1Tokens;
    private final int player2Tokens;
    private final Token[] board;
    private final Party player;
    private final Party player1;
    private final Party player2;
    private final int moves;
    @CordaSerializable
    public enum GameStatus {INITIAL, PLACEMENT, MOVING, FINISHED};
    private GameStatus gameStatus;
    private final String gameStatusHint;
    private final String moveHint;
    private final UniqueIdentifier linearId;

    /**
     * @param player1Tokens Player1 Pieces off board.
     * @param player2Tokens Player2 Pieces off board.
     * @param board The state of the board in play. Board is fixed size single dimension.
     * @param player The player who made the move to this state.
     * @param player1 The player in the game, they made the first move.
     * @param player2 The second player in the game.
     * @param moves The move counter, on issue this is set to 1.
     * @param gameStatus The game status.
     * @param gameStatusHint Human readable class set string as a helpful hint.
     * @param moveHint Free set string set by the move, maybe a popular local insult.
     */
    @ConstructorForDeserialization
    public TPMState(int player1Tokens,
                    int player2Tokens,
                    Token[] board,
                    Party player,
                    Party player1,
                    Party player2,
                    int moves,
                    GameStatus gameStatus,
                    String gameStatusHint,
                    String moveHint,
                    UniqueIdentifier linearId)
    {
        this.player1Tokens = Math.max( Math.min( player1Tokens, BOARD_WIDTH), 0);
        this.player2Tokens = Math.max( Math.min( player2Tokens, BOARD_WIDTH), 0);
        this.board = board;
        this.player = player;
        this.player1 = player1;
        this.player2 = player2;
        this.moves = moves;
        this.gameStatus = gameStatus;
        this.gameStatusHint = gameStatusHint;
        this.moveHint = moveHint;
        this.linearId = linearId;
    }

    /*
     * The constructor used when creating a new state for a move.
     */
    private TPMState(int player1Tokens,
                    int player2Tokens,
                    Token[] board,
                    Party player,
                    Party player1,
                    Party player2,
                    int moves,
                    String moveHint,
                    UniqueIdentifier linearId,
                    int src,
                    int dst) {
        // Maybe call above constructor??
        this.player1Tokens = Math.max( Math.min( player1Tokens, BOARD_WIDTH), 0);
        this.player2Tokens = Math.max( Math.min( player2Tokens, BOARD_WIDTH), 0);
        this.board = board;
        this.player = player;
        this.player1 = player1;
        this.player2 = player2;
        this.moves = moves;
        this.moveHint = moveHint;
        this.linearId = linearId;

        // Easier to refer to player1 and player2 in hints at the moment.
        String playerHint = player.equals(player1) ? "Player1" : (player.equals(player2) ? "Player2" : player.toString());

        // We work out the state from the above fields.
        // The state field stops clients implementing this logic repeatedly.
        if ((BOARD_WIDTH == player1Tokens) && (BOARD_WIDTH == player2Tokens)) {
            this.gameStatus = GameStatus.INITIAL;
            this.gameStatusHint = "Initial";
        } else if ((0 == player1Tokens) && (0 == player2Tokens)) {
            if (gameOver(Token.PLAYER1)) {
                this.gameStatus = gameStatus.FINISHED;
                this.gameStatusHint = String.format("Player1 won in %d moves",moves);
            }
            else if (gameOver(Token.PLAYER2)) {
                this.gameStatus = gameStatus.FINISHED;
                this.gameStatusHint = String.format("Player2 won in %d moves",moves);
            } else {
                this.gameStatus = gameStatus.MOVING;
                this.gameStatusHint =String.format("%s moved from %d to %d", playerHint, src, dst);
            }
        } else {
            this.gameStatus = gameStatus.PLACEMENT;
            this.gameStatusHint = String.format("%s placed at %d", playerHint, dst);
        }
    }

    /* Initialize a new board.
     * @param player1 The player in the game, they made the first move.
     * @param player2 The second player in the game.
     */
    public TPMState(Party player1, Party player2, String createHint, String gameId) {
        this.player1Tokens = BOARD_WIDTH;
        this.player2Tokens = BOARD_WIDTH;
        this.board = new Token[BOARD_WIDTH*BOARD_WIDTH];
        this.player = null;
        this.player1 = player1;
        this.player2 = player2;
        this.gameStatus = GameStatus.INITIAL;
        this.gameStatusHint = "Initial";
        this.moveHint = createHint;
        this.moves = 0;
        this.linearId = new UniqueIdentifier(gameId);
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

    public Party getPlayer() { return player; }

    public Party getPlayer1() {
        return player1;
    }

    public Party getPlayer2() {
        return player2;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
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

    // Encode a board address into an integer, used to check for winning line.
    private static int encodeAddress(int c, int a, int p) {
        return c | (a << (8*p));
    }

    // Winning line addresses encoded into a single integer, order is important, same as board scanning order.
    private static HashSet<Integer> winningAddresses = new HashSet<>(Arrays.asList(
        encodeAddress(encodeAddress(encodeAddress(0, 0, 0), 1, 1), 2,2),
        encodeAddress(encodeAddress(encodeAddress(0, 0, 0), 4, 1), 8,2),
        encodeAddress(encodeAddress(encodeAddress(0, 0, 0), 3, 1), 6,2),
        encodeAddress(encodeAddress(encodeAddress(0, 1, 0), 4, 1), 7,2),
        encodeAddress(encodeAddress(encodeAddress(0, 2, 0), 5, 1), 8,2),
        encodeAddress(encodeAddress(encodeAddress(0, 2, 0), 4, 1), 6,2),
        encodeAddress(encodeAddress(encodeAddress(0, 3, 0), 4, 1), 5,2),
        encodeAddress(encodeAddress(encodeAddress(0, 4, 0), 5, 1), 6,2)
    ));

    // Return true if the game is over, there is a completed line.
    private boolean gameOver(Token pt) {
        int a = 0;
        int ii = 0;
        for (int i=0;i<board.length;++i) {
            if (pt.equals(board[i])) {
                a = encodeAddress(a, i, ii++);
            }
        }
        return winningAddresses.contains(a);
    }

    public boolean gameOver() {
        return gameOver(Token.PLAYER1) || gameOver(Token.PLAYER2);
    }

    // Legal moves encoded as src -> dst.
    private static HashSet<Integer> legalMoves = new HashSet<>(Arrays.asList(
            encodeAddress(encodeAddress(0, 0, 0), 1, 1),
            encodeAddress(encodeAddress(0, 1, 0), 0, 1),
            encodeAddress(encodeAddress(0, 0, 0), 4, 1),
            encodeAddress(encodeAddress(0, 4, 0), 0, 1),
            encodeAddress(encodeAddress(0, 0, 0), 3, 1),
            encodeAddress(encodeAddress(0, 3, 0), 0, 1),
            encodeAddress(encodeAddress(0, 1, 0), 2, 1),
            encodeAddress(encodeAddress(0, 2, 0), 1, 1),
            encodeAddress(encodeAddress(0, 1, 0), 4, 1),
            encodeAddress(encodeAddress(0, 4, 0), 1, 1),
            encodeAddress(encodeAddress(0, 2, 0), 4, 1),
            encodeAddress(encodeAddress(0, 4, 0), 2, 1),
            encodeAddress(encodeAddress(0, 2, 0), 5, 1),
            encodeAddress(encodeAddress(0, 5, 0), 2, 1),
            encodeAddress(encodeAddress(0, 3, 0), 4, 1),
            encodeAddress(encodeAddress(0, 4, 0), 3, 1),
            encodeAddress(encodeAddress(0, 3, 0), 6, 1),
            encodeAddress(encodeAddress(0, 6, 0), 3, 1),
            encodeAddress(encodeAddress(0, 4, 0), 5, 1),
            encodeAddress(encodeAddress(0, 5, 0), 4, 1),
            encodeAddress(encodeAddress(0, 4, 0), 6, 1),
            encodeAddress(encodeAddress(0, 6, 0), 4, 1),
            encodeAddress(encodeAddress(0, 4, 0), 7, 1),
            encodeAddress(encodeAddress(0, 7, 0), 4, 1),
            encodeAddress(encodeAddress(0, 4, 0), 8, 1),
            encodeAddress(encodeAddress(0, 8, 0), 4, 1),
            encodeAddress(encodeAddress(0, 5, 0), 8, 1),
            encodeAddress(encodeAddress(0, 8, 0), 5, 1),
            encodeAddress(encodeAddress(0, 6, 0), 7, 1),
            encodeAddress(encodeAddress(0, 7, 0), 6, 1),
            encodeAddress(encodeAddress(0, 7, 0), 8, 1),
            encodeAddress(encodeAddress(0, 8, 0), 7, 1)
    ));

    private boolean legalMove(int src, int dst) {
        return legalMoves.contains(encodeAddress(encodeAddress(0, src, 0), dst, 1));
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

            // Sum of tokens in counters and on board should equal BOARD_WIDTH for each player.
            int p1pt = getPlayer1Tokens();
            int p2pt = getPlayer2Tokens();
            for (Token t : getBoard()) {
                if (null == t) {
                    ;
                } else if (Token.PLAYER1 == t) {
                    ++p1pt;
                } else if (Token.PLAYER2 == t) {
                    ++p2pt;
                }
            }
            require.using("Expected 3 tokens in play for player1", BOARD_WIDTH == p1pt);
            require.using("Expected 3 tokens in play for player2", BOARD_WIDTH == p2pt);
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
            require.using("Game is over", !gameOver());

            // Work out what has changed on the board, should be one move onto or move on the board.
            Token srcToken = null;
            int srcAddress = -1;
            Token dstToken = null;
            int dstAddress = -1;

            for (int i=0; i<getBoard().length; ++i) {
                Token srcTokenTmp = getBoard()[i];
                Token dstTokenTmp = stateNew.getBoard()[i];
                if (Objects.equals(srcTokenTmp, dstTokenTmp)) {
                    // Same objects on board at same address, nothing has changed.
                    ;
                }
                else if (null == srcTokenTmp) {
                    // We've found a move to. Should always find one.
                    require.using("Only one token move to allowed", null == dstToken);
                    dstToken = dstTokenTmp;
                    dstAddress = i;
                }
                else if (null == dstTokenTmp) {
                    // We've found a move from. Should find at most one.
                    require.using("Only one token move from allowed", null == srcToken);
                    srcToken = srcTokenTmp;
                    srcAddress = i;
                } else {
                    // Only end up here is token has been flipped.
                    require.using("Tokens have been swapped", false);
                }
            }

            // Must have moved something.
            require.using("No destination move found", null != dstToken);

            // Make sure that the destination token is for the correct player.
            require.using("Expected Player2 to make move", (Token.PLAYER2 == dstToken) || (stateNew.getPlayer().equals(getPlayer1())));
            require.using("Expected Player1 to make move", (Token.PLAYER1 == dstToken) || (stateNew.getPlayer().equals(getPlayer2())));

            // If there are pieces to play then in initial placement phase.
            if ((0 != getPlayer1Tokens()) || (0 != getPlayer2Tokens())) {

                // Make sure boards are not identical.
                require.using("Token moved during initial phase", null == srcToken);

                // Make sure that the found token was taken from the correct pool.
                require.using("Player2 token mismatch",  (Token.PLAYER2 == dstToken) || ((Token.PLAYER1 == dstToken) && ((getPlayer1Tokens()-1) == stateNew.getPlayer1Tokens())));
                require.using("Player1 token mismatch",  (Token.PLAYER1 == dstToken) || ((Token.PLAYER2 == dstToken) && ((getPlayer2Tokens()-1) == stateNew.getPlayer2Tokens())));

            } else {

                // Something must have moved.
                require.using("No source token was found during play", null != srcToken);

                // Make sure that the tokens are the same.
                require.using("Tokens must be the same player", srcToken.equals(dstToken));

                // Check for a valid move on the board.
                require.using("Invalid move", legalMove(srcAddress, dstAddress));
            }
            return null;
        });
    }

    // We also need to transition a state given a move. The move specifies, source, dest and player.
    public TPMState move(Party player, String moveHint, int src, int dst) {

        // We don't check much else here as any problems should be caught above when checking the contract.
        Token t = null;
        int player1TokensNew = getPlayer1Tokens();
        int player2TokensNew = getPlayer2Tokens();
        if (getPlayer1().equals(player)) {
            t = Token.PLAYER1;
            if (player1TokensNew > 0) {
                player1TokensNew--;
            }
        } else if (getPlayer2().equals(player)) {
            t = Token.PLAYER2;
            if (player2TokensNew > 0) {
                player2TokensNew--;
            }
        }

        // This is interesting, we're creating a new board and then copying elements.
        Token[] boardOld = getBoard();
        Token[] boardNew = new Token[boardOld.length];
        for (int i=0; i<boardOld.length; ++i) {
            if (i == src) {
                boardNew[i] = null;
            } else if (i == dst) {
                boardNew[i] = t;
            } else {
                boardNew[i] = boardOld[i];
            }
        }

        return new TPMState(
            player1TokensNew,
            player2TokensNew,
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
        return String.format("TPMState(player1=%s, player2=%s, move=%s)", player1, player2, moves);
    }
}