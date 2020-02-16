package com.example.groceryassistant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DisplayTest {

    Display gui;

    @Before
    public void setUp() throws Exception {
        gui = new Display();
    }

    @After
    public void tearDown() throws Exception {
        gui = null;
    }

    @Test
    public void setDisplayDensity() {
        gui.setDisplayDensity(0.01f);
        assertEquals(0.01f, gui.getDisplayDensity(), 0.00001f);

        gui.setDisplayDensity(0.1f);
        assertEquals(0.1f, gui.getDisplayDensity(), 0.00001f);

        gui.setDisplayDensity(1.0f);
        assertEquals(1.0f, gui.getDisplayDensity(), 0.00001f);

        gui.setDisplayDensity(10.0f);
        assertEquals(10.0f, gui.getDisplayDensity(), 0.00001f);
    }
}