package com.grelobites.dandanator.poke;

import com.grelobites.dandanator.model.poke.pok.PokPoke;
import com.grelobites.dandanator.model.poke.pok.PokTrainer;
import com.grelobites.dandanator.model.poke.pok.PokValue;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * Created by mteira on 22/6/16.
 */
public class PokFormatLoadingTest {

    private static void checkPokeValue(PokValue pokeValue, int bank, int address,
                                       int value, int originalValue) {
        assertEquals(bank, (long) pokeValue.getBank());
        assertEquals(address, (long) pokeValue.getAddress());
        assertEquals(value, (long) pokeValue.getValue());
        assertEquals(originalValue, (long) pokeValue.getOriginalValue());
    }

    private static void checkTrainer(PokTrainer trainer, String name, int pokeCount) {
        assertEquals(name, trainer.getName());
        assertNotNull(trainer.getPokeValues());
        assertEquals(pokeCount, trainer.getPokeValues().size());
    }

    @Test
    public void test720DegreesPokeLoadFromFile() throws Exception {
        InputStream is = PokFormatLoadingTest.class
                .getClassLoader().getResourceAsStream("pokes/pok/720 Degrees (1986)(US Gold).pok");
        assertNotNull(is);

        PokPoke poke = PokPoke.fromInputStream(is);
        assertEquals(4, poke.getTrainers().size());

        PokTrainer currentTrainer = poke.getTrainers().get(0);
        checkTrainer(currentTrainer, "Infinite lives", 1);
        checkPokeValue(currentTrainer.getPokeValues().get(0), 8, 40774, 0, 61);

        currentTrainer = poke.getTrainers().get(1);
        checkTrainer(currentTrainer, "Infinite money", 1);
        checkPokeValue(currentTrainer.getPokeValues().get(0), 8, 40360, 0, 53);

        currentTrainer = poke.getTrainers().get(2);
        checkTrainer(currentTrainer, "Infinite tickets", 1);
        checkPokeValue(currentTrainer.getPokeValues().get(0), 8, 37357, 0, 203);

        currentTrainer = poke.getTrainers().get(3);
        checkTrainer(currentTrainer, "Infinite time", 1);
        checkPokeValue(currentTrainer.getPokeValues().get(0), 8, 41918, 0, 53);
    }

    @Test
    public void testPyramaniaPokeLoadFromFile() throws Exception {
        InputStream is = PokFormatLoadingTest.class
                .getClassLoader().getResourceAsStream("pokes/pok/Pyramania.pok");
        assertNotNull(is);

        PokPoke poke = PokPoke.fromInputStream(is);
        assertEquals(3, poke.getTrainers().size());

        PokTrainer currentTrainer = poke.getTrainers().get(0);
        checkTrainer(currentTrainer, "Infinite lives", 1);
        checkPokeValue(currentTrainer.getPokeValues().get(0), 8, 30349, 0, 0);

        currentTrainer = poke.getTrainers().get(1);
        checkTrainer(currentTrainer, "Immune to killer landscape", 2);
        checkPokeValue(currentTrainer.getPokeValues().get(0), 8, 29353, 24, 0);
        checkPokeValue(currentTrainer.getPokeValues().get(1), 8, 29354, 1, 0);

        currentTrainer = poke.getTrainers().get(2);
        checkTrainer(currentTrainer, "Immune to nasties", 3);
        checkPokeValue(currentTrainer.getPokeValues().get(0), 8, 30254, 58, 0);
        checkPokeValue(currentTrainer.getPokeValues().get(1), 8, 30266, 58, 0);
        checkPokeValue(currentTrainer.getPokeValues().get(2), 8, 30278, 58, 0);

    }

}
