package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DealerTest {

    Player player0;
    Player player1;
    private Player[] players;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;
    

    void assertInvariants() {
        assertTrue(player0.id >= 0);
        assertTrue(player1.id >= 0);
        assertTrue(player0.score() >= 0);
        assertTrue(player1.score() >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, ""), ui, util);
        table = new Table(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
        player0 = new Player(env, dealer, table, 0, true);
        player1 = new Player(env, dealer, table, 1, true);
        players = new Player[2];
        players[0] = player0;
        players[1] = player1;
        dealer = new Dealer(env, table, players);

        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    void placeCardsAndFewTokens(){
        dealer.placeCardsOnTable();
        table.tokensArray[0][0] = true;
        table.tokensArray[0][1] = true; 
    }

    // our tests begin here:
    @Test
    void placeTokens(){
        placeCardsAndFewTokens();
        assertEquals(table.tokensArray[0][0],true);
        assertEquals(table.tokensArray[0][1],true);
        dealer.removeTokens(0);
        assertEquals(table.tokensArray[0][0],false);
        assertEquals(table.tokensArray[0][1],false);
    }

    @Test
    void removeCards(){
        assertNull(table.slotToCard[0]);
        table.slotToCard[0] = 0;
        table.slotToCard[1] = 1;
        assertEquals(table.slotToCard[0],0);
        dealer.removeAllCardsFromTable();
        assertNull(table.slotToCard[0]);
        assertNull(table.slotToCard[1]);
    }
}
