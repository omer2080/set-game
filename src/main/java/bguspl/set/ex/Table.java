package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * Mapping between tokens to slots & players.
     * tokensArray[slot][player] returns true iff the matching player has a token in that slot)
     */
    protected final boolean[][] tokensArray; 

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        tokensArray = new boolean[slotToCard.length][env.config.players];
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    synchronized public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * Not responsible for removing tokens.
     * @param slot - the slot from which to remove the card.
     */
    synchronized public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        Integer cardToRemove = slotToCard[slot];
        slotToCard[slot] = null; 
        cardToSlot[cardToRemove] = null;
        env.ui.removeCard(slot);
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     * @return       - true iff a token was successfully placed.
     */
    synchronized public boolean placeToken(int player, int slot) {
        boolean isPlaced = tokensArray[slot][player];
        if (!isPlaced){ 
            tokensArray[slot][player] = true;
            env.ui.placeToken(player, slot);    
            return true;
        }
        return false;
    }

    /**
     * Removes a token of a player from a grid slot. Not responsible for returning the token to the player.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    synchronized public boolean removeToken(int player, int slot) {
        boolean isPlaced = tokensArray[slot][player];
        if (isPlaced){
            tokensArray[slot][player] = false;
            env.ui.removeToken(player, slot);
            return true;
        }
        return false;
    }

    /**
     * Flips a token of a player. Not responsible for returning the token to the player.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to flip the token.
     * @return       - True if after the flip the slot has a token. False if it doesn't.
     */
    synchronized public boolean flipToken(int player, int slot) {
        boolean isPlaced = tokensArray[slot][player];
        if (isPlaced){ //removing a token 
            tokensArray[slot][player] = false;
            env.ui.removeToken(player, slot);
            return false;
        }
        else{ //adding a token 
            tokensArray[slot][player] = true;
            env.ui.placeToken(player, slot);
            return true;
        }
    }
}