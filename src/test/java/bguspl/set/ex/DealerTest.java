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

import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealerTest {


    Dealer dealer;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;

    private Table table;

    private Integer[] slotToCard;
    private Integer[] cardToSlot;
    private Player[] players;
    @Mock
    private Logger logger;


    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        dealer = new Dealer(env, table, players);
    }

    @Test
    void checkRemoveSet() {
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        Player player = new Player(env,dealer,table,0,true);
       dealer.removeSet(player);
        assertEquals(true,player.getPotentialSetSize()==0);
    }

    @Test
    //tries to add the same player to the queue twice
    void checkRemoveSet2() {
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        Player player = new Player(env,dealer,table,0,true);
        int card = player.getPotentialSet()[0];
        assertNull(table.cardToSlot[card]);

    }
}