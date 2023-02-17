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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerTest {

    Player player;
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
        assertTrue(player.id >= 0);
        assertTrue(player.score() >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        player = new Player(env, dealer, table, 0, false);
        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void point() {

        // force table.countCards to return 3
        when(table.countCards()).thenReturn(3); // this part is just for demonstration

        // calculate the expected score for later
        int expectedScore = player.score() + 1;

        // call the method we are testing
        player.point();

        // check that the score was increased correctly
        assertEquals(expectedScore, player.score());

        // check that ui.setScore was called with the player's id and the correct score
        verify(ui).setScore(eq(player.id), eq(expectedScore));
    }

    //our tests begin here:
    @Test
    void numSetForCheck(){
        // calculates the expected number of sets the player claimed
        int expectedNumOfSets = player.numSetForCheck() + 1; 
        
        // calls the method we are testing
        player.setClaimed();
        
        //checks if the number of sets claimed indeed increases by one when player claims another set
        assertEquals(expectedNumOfSets, player.numSetForCheck());
    }

    @Test
    void keyPressed(){
        // empties the queue of player-presses 
        player.playerPressesQueue.clear();

        // checks if the presses queue indeed empty
        assertEquals(true, player.playerPressesQueue.isEmpty());
        
        // putting token - slot num 0
        player.keyPressed(0);
        
        //checks if the number of sets claimed indeed increases by one when player claims another set
        assertEquals(false, player.playerPressesQueue.isEmpty());

        // putting token - slot num 1,2
        player.keyPressed(1);
        player.keyPressed(2);
        
        //checks if after these 3 insertions the pressesQueue is indeed full
        assertEquals(0, player.playerPressesQueue.remainingCapacity());
    }


}