package com.mmorpg.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatChannelTest {
    @Test
    void routesPlainTextToTheSelectedChannel() {
        assertEquals("/g Bonjour", ChatChannel.GUILDE.route("Bonjour"));
        assertEquals("/p Bonjour", ChatChannel.GROUPE.route("  Bonjour "));
        assertEquals("Bonjour", ChatChannel.GENERAL.route("Bonjour"));
    }

    @Test
    void commandsAndEmptyInputAreNeverRewritten() {
        assertEquals("/spawn", ChatChannel.GUILDE.route("/spawn"));
        assertEquals("", ChatChannel.GROUPE.route(""));
        assertEquals("   ", ChatChannel.GUILDE.route("   "));
    }

    @Test
    void classifiesReceivedLines() {
        assertEquals(ChatChannel.GUILDE, ChatChannel.classify("[Guilde] ★ Alice « Veilleuse » : salut"));
        assertEquals(ChatChannel.GUILDE, ChatChannel.classify("[Guilde] Bob : salut"));
        assertEquals(ChatChannel.GROUPE, ChatChannel.classify("[Groupe] Bob : on y va"));
        assertEquals(ChatChannel.GENERAL, ChatChannel.classify("[Groupe] Bob a rejoint le groupe."));
        assertEquals(ChatChannel.GENERAL, ChatChannel.classify("[Groupe] Bob vous invite dans son groupe. [Accepter] [Refuser]"));
        assertEquals(ChatChannel.GENERAL, ChatChannel.classify("<Bob> [Guilde] : faux"));
        assertEquals(ChatChannel.GENERAL, ChatChannel.classify(null));
    }

    @Test
    void generalShowsEveryChannelOthersStayFiltered() {
        for (ChatChannel line : ChatChannel.values()) assertTrue(ChatChannel.GENERAL.shows(line));
        assertTrue(ChatChannel.GUILDE.shows(ChatChannel.GUILDE));
        assertFalse(ChatChannel.GUILDE.shows(ChatChannel.GENERAL));
        assertFalse(ChatChannel.GROUPE.shows(ChatChannel.GUILDE));
    }

    @Test
    void cyclesAndRestoresByName() {
        assertEquals(ChatChannel.GUILDE, ChatChannel.GENERAL.next(1));
        assertEquals(ChatChannel.GROUPE, ChatChannel.GENERAL.next(-1));
        assertEquals(ChatChannel.GENERAL, ChatChannel.GROUPE.next(1));
        assertEquals(ChatChannel.GUILDE, ChatChannel.byName("guilde\n"));
        assertEquals(ChatChannel.GENERAL, ChatChannel.byName("???"));
    }
}
