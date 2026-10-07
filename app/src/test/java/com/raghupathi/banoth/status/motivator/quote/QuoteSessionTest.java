package com.raghupathi.banoth.status.motivator.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class QuoteSessionTest {

    private static QuoteRepository.QuoteItem quote(String id, String text) {
        return new QuoteRepository.QuoteItem(id, text, "", "BREAKUP", "en");
    }

    private static List<QuoteRepository.QuoteItem> five() {
        return Arrays.asList(quote("A", "a"), quote("B", "b"), quote("C", "c"), quote("D", "d"), quote("E", "e"));
    }

    @Test
    public void nextWalksEveryQuoteExactlyOnceThenStops() {
        for (int seed = 0; seed < 200; seed++) {
            QuoteSession session = new QuoteSession("BREAKUP", "en");
            session.startCycle(five(), QuoteSession.Source.API, new Random(seed));
            Set<String> seen = new HashSet<>();
            seen.add(session.current().id);
            QuoteRepository.QuoteItem next;
            while ((next = session.advance()) != null) {
                assertTrue("repeated " + next.id, seen.add(next.id));
            }
            assertEquals(5, seen.size());
            assertFalse(session.hasNext());
        }
    }

    @Test
    public void newCycleNeverStartsWithThePreviousQuote() {
        for (int seed = 0; seed < 500; seed++) {
            Random random = new Random(seed);
            QuoteSession session = new QuoteSession("LOVE", "en");
            session.startCycle(five(), QuoteSession.Source.API, random);
            while (session.advance() != null) { }
            String last = session.current().id;
            session.startCycle(session.snapshot(), QuoteSession.Source.API, random);
            assertNotEquals(last, session.current().id);
            assertEquals(0, session.getCurrentIndex());
        }
    }

    @Test
    public void emptySessionHasNoQuote() {
        QuoteSession session = new QuoteSession("NIGHT", "te");
        session.startCycle(new ArrayList<>(), QuoteSession.Source.LOCAL, new Random(1));
        assertNull(session.current());
        assertNull(session.advance());
    }

    @Test
    public void duplicatesDifferingOnlyByCaseWhitespaceOrPunctuationAreRemoved() {
        List<QuoteRepository.QuoteItem> raw = Arrays.asList(
                quote("1", "Never give up."),
                quote("2", "  never   GIVE up "),
                quote("3", "\u201CNever give up!\u201D"),
                quote("4", "Keep going."),
                quote("4", "Different text, same API id"));
        List<QuoteRepository.QuoteItem> unique = QuoteRepository.deduplicate(raw);
        assertEquals(2, unique.size());
        assertEquals("1", unique.get(0).id);
        assertEquals("4", unique.get(1).id);
    }

    @Test
    public void stableIdIsDeterministicAndScopedToCategoryAndLanguage() {
        String a = QuoteRepository.stableQuoteId("LOVE", "en", "Never give up.", "Me");
        assertEquals(a, QuoteRepository.stableQuoteId("LOVE", "en", "  never give UP ", "me"));
        assertNotEquals(a, QuoteRepository.stableQuoteId("BREAKUP", "en", "Never give up.", "Me"));
        assertNotEquals(a, QuoteRepository.stableQuoteId("LOVE", "te", "Never give up.", "Me"));
    }

    @Test
    public void parsesRealApiResponse() throws Exception {
        String json;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("api_love_page0.json")) {
            assertNotNull(in);
            json = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        }
        QuoteApiService.ApiPage page = QuoteApiService.parsePage(json);
        assertEquals(10, page.quotes.size());
        assertEquals(0, page.page);
        assertTrue(page.total >= page.quotes.size());
        for (QuoteApiService.ApiQuote q : page.quotes) {
            assertFalse(q.id.isEmpty());
            assertFalse(q.content.isEmpty());
            assertFalse(q.author.isEmpty());
            assertTrue("not tagged Love: " + q.tags, q.tags.contains("Love"));
        }
    }
}
