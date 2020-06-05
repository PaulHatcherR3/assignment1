package com.assignment1.test.board;

import com.assignment1.board.TPMBoard;
import com.assignment1.state.TPMState;
import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.*;

public class TPMBoardTests {
    @Test
    public void initial() {
        final TPMBoard board = new TPMBoard();
        assertEquals(TPMBoard.GameStatus.INITIAL, board.getGameStatus());
        assertEquals(TPMBoard.BOARD_WIDTH*TPMBoard.BOARD_WIDTH, board.getBoard().length);
        for (int i=0;i<board.getBoard().length;++i){
            assertNull(board.getBoard()[i]);
        }
        assertTrue(board.checkInvariants(TPMBoard.Token.PLAYER1));
        assertTrue(board.checkInvariants(TPMBoard.Token.PLAYER2));
        assertFalse(board.gameOver());
        assertEquals(3, board.getPlayer1Tokens());
        assertEquals(3, board.getPlayer2Tokens());
    }

    @Test
    public void placement() {

        TPMBoard board = new TPMBoard();

        // Got through six placement moves.
        final int boardSize=TPMBoard.BOARD_WIDTH*TPMBoard.BOARD_WIDTH;
        TPMBoard.Token[] boardExpected = new TPMBoard.Token[boardSize];
        for (int i=0; i<(TPMBoard.BOARD_WIDTH*2); ++i) {
            final TPMBoard.Token token = (0==i%2) ? TPMBoard.Token.PLAYER1 : TPMBoard.Token.PLAYER2;

            boardExpected[i+3] = token;
            board = board.move(-1, i+3, token);

            assertEquals(i<5 ? TPMBoard.GameStatus.PLACEMENT : TPMBoard.GameStatus.MOVING, board.getGameStatus());
            assertEquals(TPMBoard.BOARD_WIDTH * TPMBoard.BOARD_WIDTH, board.getBoard().length);
            assertArrayEquals(boardExpected, board.getBoard());
            assertTrue(board.checkInvariants(TPMBoard.Token.PLAYER1));
            assertTrue(board.checkInvariants(TPMBoard.Token.PLAYER2));
            assertFalse(board.gameOver());
            assertEquals(((TPMBoard.BOARD_WIDTH*2)-1-i)/2, board.getPlayer1Tokens());
            assertEquals(((TPMBoard.BOARD_WIDTH*2)-0-i)/2, board.getPlayer2Tokens());
        }
    }

    @Test
    public void move() {

        TPMBoard board = new TPMBoard();

        // Got through six placement moves.
        // final int boardSize=TPMBoard.BOARD_WIDTH*TPMBoard.BOARD_WIDTH;
        for (int i=0; i<(TPMBoard.BOARD_WIDTH*2); ++i) {
            final TPMBoard.Token token = (0 == i % 2) ? TPMBoard.Token.PLAYER1 : TPMBoard.Token.PLAYER2;
            board = board.move(-1, i + 3, token);
        }

        final TPMBoard.Token boards[][] = {
                {null, null, null,
                 TPMBoard.Token.PLAYER1, TPMBoard.Token.PLAYER2, TPMBoard.Token.PLAYER1,
                 TPMBoard.Token.PLAYER2, TPMBoard.Token.PLAYER1, TPMBoard.Token.PLAYER2},
                {TPMBoard.Token.PLAYER1, null, null,
                 null, TPMBoard.Token.PLAYER2, TPMBoard.Token.PLAYER1,
                 TPMBoard.Token.PLAYER2, TPMBoard.Token.PLAYER1, TPMBoard.Token.PLAYER2},
                {TPMBoard.Token.PLAYER1, TPMBoard.Token.PLAYER2, null,
                 null, null, TPMBoard.Token.PLAYER1,
                 TPMBoard.Token.PLAYER2, TPMBoard.Token.PLAYER1, TPMBoard.Token.PLAYER2}
        };

        final Function<TPMBoard, Integer> assertions = (boardl) -> {
            assertEquals(TPMBoard.GameStatus.MOVING, boardl.getGameStatus());
            assertEquals(TPMBoard.BOARD_WIDTH * TPMBoard.BOARD_WIDTH, boardl.getBoard().length);
            assertTrue(boardl.checkInvariants(TPMBoard.Token.PLAYER1));
            assertTrue(boardl.checkInvariants(TPMBoard.Token.PLAYER2));
            assertFalse(boardl.gameOver());
            assertEquals(0, boardl.getPlayer1Tokens());
            assertEquals(0, boardl.getPlayer2Tokens());
            return 0;
        };

        assertArrayEquals(boards[0], board.getBoard());
        assertions.apply(board);

        // Make some moves.
        board = board.move(3, 0, TPMBoard.Token.PLAYER1);
        assertArrayEquals(boards[1], board.getBoard());
        assertions.apply(board);

        board = board.move(4, 1, TPMBoard.Token.PLAYER2);
        assertArrayEquals(boards[2], board.getBoard());
        assertions.apply(board);
    }
}
