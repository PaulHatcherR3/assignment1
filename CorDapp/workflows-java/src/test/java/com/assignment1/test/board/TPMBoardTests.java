package com.assignment1.test.board;

import com.assignment1.board.TPMBoard;
import org.junit.Test;

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
            for (int j = 0; j < board.getBoard().length; ++j) {
                assertEquals(boardExpected[j], board.getBoard()[j]);
            }
            assertTrue(board.checkInvariants(TPMBoard.Token.PLAYER1));
            assertTrue(board.checkInvariants(TPMBoard.Token.PLAYER2));
            assertFalse(board.gameOver());
            assertEquals(((TPMBoard.BOARD_WIDTH*2)-1-i)/2, board.getPlayer1Tokens());
            assertEquals(((TPMBoard.BOARD_WIDTH*2)-0-i)/2, board.getPlayer2Tokens());
        }
    }
}
