package com.assignment1.test.state;

import com.assignment1.board.TPMBoard;
import com.assignment1.state.TPMState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TPMStateTests {

    static private final TestIdentity megaCorp = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    static private final TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "London", "GB"));

    @Test
    public void getters() {
        TPMState state = new TPMState(megaCorp.getParty(), miniCorp.getParty(), "berty", "game1");
        assertEquals(megaCorp.getParty(), state.getPlayer1());
        assertEquals(miniCorp.getParty(), state.getPlayer2());
        assertEquals("berty", state.getMoveHint());
        assertEquals("game1", state.getGameId());
        assertEquals(0, state.getMoves());
        assertEquals(TPMBoard.GameStatus.INITIAL,state.getGameStatus());
    }
}
