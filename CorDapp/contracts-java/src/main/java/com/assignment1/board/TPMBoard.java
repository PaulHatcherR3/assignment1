package com.assignment1.board;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

@CordaSerializable
public class TPMBoard {

    @CordaSerializable
    public enum Token {PLAYER1, PLAYER2};

    public static final int BOARD_WIDTH = 3;
    private final int player1Tokens;
    private final int player2Tokens;
    private final Token[] board;

    @CordaSerializable
    public enum GameStatus {INITIAL, PLACEMENT, MOVING, FINISHED}
    private GameStatus gameStatus;

    @ConstructorForDeserialization
    public TPMBoard(int player1Tokens,
                    int player2Tokens,
                    Token[] board,
                    GameStatus gameStatus) {
        this.player1Tokens = Math.max(Math.min(player1Tokens, BOARD_WIDTH), 0);
        this.player2Tokens = Math.max(Math.min(player2Tokens, BOARD_WIDTH), 0);
        this.board = board;
        this.gameStatus = gameStatus;
    }

    private TPMBoard(int player1Tokens,
                     int player2Tokens,
                     Token[] board) {
        this(player1Tokens, player2Tokens, board,
            ((BOARD_WIDTH == player1Tokens) && (BOARD_WIDTH == player2Tokens)) ? GameStatus.INITIAL :
            (((0 == player1Tokens) && (0 == player2Tokens)) ? (TPMBoard.gameOver(board) ? GameStatus.FINISHED : GameStatus.MOVING) :
            GameStatus.PLACEMENT) );
    }

    public TPMBoard() {
        this.player1Tokens = BOARD_WIDTH;
        this.player2Tokens = BOARD_WIDTH;
        this.board = new Token[BOARD_WIDTH * BOARD_WIDTH];
        this.gameStatus = GameStatus.INITIAL;
    }

    public Token getToken(int x, int y) {
        return board[x*BOARD_WIDTH + y];
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

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    // Encode a board address into an integer, used to check for winning line.
    private static int encodeAddress(int c, int a, int p) {
        return c | (a << (8 * p));
    }

    // Winning line addresses encoded into a single integer, order is important, same as board scanning order.
    private static HashSet<Integer> winningAddresses = new HashSet<>(Arrays.asList(
            encodeAddress(encodeAddress(encodeAddress(0, 0, 0), 1, 1), 2, 2),
            encodeAddress(encodeAddress(encodeAddress(0, 0, 0), 4, 1), 8, 2),
            encodeAddress(encodeAddress(encodeAddress(0, 0, 0), 3, 1), 6, 2),
            encodeAddress(encodeAddress(encodeAddress(0, 1, 0), 4, 1), 7, 2),
            encodeAddress(encodeAddress(encodeAddress(0, 2, 0), 5, 1), 8, 2),
            encodeAddress(encodeAddress(encodeAddress(0, 2, 0), 4, 1), 6, 2),
            encodeAddress(encodeAddress(encodeAddress(0, 3, 0), 4, 1), 5, 2),
            encodeAddress(encodeAddress(encodeAddress(0, 4, 0), 5, 1), 6, 2)
    ));

    private static boolean gameOver(Token[] board, Token pt) {
        int ea = 0;
        int ii = 0;
        int a = 0;
        // Encode board state for token and lookup in winningAddresses.
        for (Token bt : board) {
            if (pt.equals(bt)) {
                ea = encodeAddress(ea, a, ii++);
            }
            a++;
        }
        return winningAddresses.contains(ea);
    }

    public static boolean gameOver(Token[] board) {
        return gameOver(board, Token.PLAYER1) || gameOver(board, Token.PLAYER2);
    }

    // Return true if the game is over, there is a mill for the given player.
    public boolean gameOver(Token pt) {
        return gameOver(board, pt);
    }

    // Return true if either player has a mill.
    public boolean gameOver() {
        return gameOver(board);
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

    public boolean legalMove(int src, int dst) {
        return legalMoves.contains(encodeAddress(encodeAddress(0, src, 0), dst, 1));
    }

    public boolean checkInvariants(Token tp) {
        // Sum of tokens in counters and on board should equal BOARD_WIDTH for each player.
        int pt = Token.PLAYER1 == tp ? getPlayer1Tokens() : (Token.PLAYER2 == tp ? getPlayer2Tokens() : BOARD_WIDTH);
        for (Token t : getBoard()) {
            if (null == t) {
                ;
            } else if (tp == t) {
                ++pt;
            }
        }
        return (BOARD_WIDTH == pt) && (board.length == BOARD_WIDTH*BOARD_WIDTH);
    }

    public static class TPMMove {
        private final int src;
        private final int dst;
        private final Token srcToken;
        private final Token dstToken;

        public TPMMove(int src, int dst, Token srcToken, Token dstToken) {
            this.src = src;
            this.dst = dst;
            this.srcToken = srcToken;
            this.dstToken = dstToken;
        }

        public int getSrc() {
            return src;
        }

        public int getDst() {
            return dst;
        }

        public Token getSrcToken() {
            return srcToken;
        }

        public Token getDstToken() {
            return dstToken;
        }
    }


    public TPMMove getMove(TPMBoard boardNext) throws IllegalArgumentException {

        // Work out given another board what has moved, only a single move allowed.
        Token srcToken = null;
        int src = -1;
        Token dstToken = null;
        int dst = -1;
        boolean error = false;
        String errorHint = null;
        for (int i = 0; i < getBoard().length; ++i) {
            Token srcTokenTmp = getBoard()[i];
            Token dstTokenTmp = boardNext.getBoard()[i];
            if (Objects.equals(srcTokenTmp, dstTokenTmp)) {
                // Same objects on board at same address, nothing has changed.
                ;
            } else if (null == srcTokenTmp) {
                // We've found a move to. Should always find one.
                if (null != dstToken) {
                    throw new IllegalArgumentException("Only one token move to allowed");
                }
                dstToken = dstTokenTmp;
                dst = i;
            } else if (null == dstTokenTmp) {
                // We've found a move from. Should find at most one.
                if (null != srcToken) {
                    throw new IllegalArgumentException("Only one token move from allowed");
                }
                srcToken = srcTokenTmp;
                src = i;
            } else {
                // Only end up here is token has been flipped.
                throw new IllegalArgumentException("Tokens have been swapped");
            }
        }
        return new TPMMove(src, dst, srcToken, dstToken);
    }

    // We also need to transition a state given a move. The move specifies, source, dest and player.
    public TPMBoard move(int src, int dst, Token token) {

        // We don't check much else here as any problems should be caught above when checking the contract.
        int player1TokensNew = getPlayer1Tokens();
        int player2TokensNew = getPlayer2Tokens();
        if (Token.PLAYER1 == token) {
            player1TokensNew = Math.max(0, player1TokensNew - 1);
        } else if (Token.PLAYER2 == token) {
            player2TokensNew = Math.max(0, player2TokensNew - 1);
        }

        // This is interesting, we're creating a new board and then copying elements.
        Token[] boardOld = getBoard();
        Token[] boardNew = new Token[boardOld.length];
        for (int i = 0; i < boardOld.length; ++i) {
            if (i == src) {
                boardNew[i] = null;
            } else if (i == dst) {
                boardNew[i] = token;
            } else {
                boardNew[i] = boardOld[i];
            }
        }

        return new TPMBoard(player1TokensNew, player2TokensNew, boardNew);
    }

    // Evaluate board score for player.

}
