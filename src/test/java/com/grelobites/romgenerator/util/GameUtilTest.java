package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import org.junit.Test;

import static org.junit.Assert.*;


public class GameUtilTest {
    @Test
    public void filterTooLongGameName() throws Exception {
        String gameName = "This is a longer than usual game name";
        assertEquals(gameName.substring(0, DandanatorMiniConstants.GAMENAME_SIZE), GameUtil.filterGameName(gameName));
    }

}