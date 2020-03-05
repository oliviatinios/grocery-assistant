package com.example.groceryassistant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NavigationTest {

    Navigation nav;

    @Before
    public void setUp() throws Exception {
        nav = new Navigation();
    }

    @After
    public void tearDown() throws Exception {
        nav = null;
    }

    @Test
    public void isNull() {
        assertTrue(nav.isNull());
    }

    @Test
    public void toggleMode() {
        assertFalse(nav.getMode());
        nav.toggleMode();
        assertTrue(nav.getMode());
        nav.toggleMode();
        assertFalse(nav.getMode());
    }

    @Test
    public void setTime() {
        nav.setTime(0);
        assertEquals(0, nav.getTime());

        nav.setTime(1000);
        assertEquals(1000, nav.getTime());

        nav.setTime(1000000);
        assertEquals(1000000, nav.getTime());

    }
}