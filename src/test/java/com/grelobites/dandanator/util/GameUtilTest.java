package com.grelobites.dandanator.util;

import com.grelobites.dandanator.Constants;
import org.junit.Test;

import static org.junit.Assert.*;


public class GameUtilTest {
    @Test
    public void filterTooLongGameName() throws Exception {
        String gameName = "This is a longer than usual game name";
        assertEquals(gameName.substring(0, Constants.GAMENAME_SIZE), GameUtil.filterGameName(gameName));
    }

}