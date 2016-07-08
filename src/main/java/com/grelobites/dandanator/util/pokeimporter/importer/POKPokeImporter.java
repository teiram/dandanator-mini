package com.grelobites.dandanator.util.pokeimporter.importer;


import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.Poke;
import com.grelobites.dandanator.model.PokeViewable;
import com.grelobites.dandanator.model.Trainer;
import com.grelobites.dandanator.model.TrainerList;
import com.grelobites.dandanator.model.poke.pok.PokPoke;
import com.grelobites.dandanator.model.poke.pok.PokTrainer;
import com.grelobites.dandanator.model.poke.pok.PokValue;
import com.grelobites.dandanator.util.pokeimporter.PokeImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

public class POKPokeImporter implements PokeImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(POKPokeImporter.class);
    @Override
    public void importPokes(TrainerList trainerList, InputStream is) throws IOException {
        PokPoke poke = PokPoke.fromInputStream(is);
        for (PokTrainer pokTrainer: poke.getTrainers()) {
            if (trainerList.getChildren().size() >= Constants.MAX_TRAINERS_PER_GAME) {
                break;
            }
            trainerList.addTrainerNode(pokTrainer.getName()).map(trainer -> {
                pokTrainer.getPokeValues().stream()
                        .filter(PokValue::isCompatibleSpectrum48K)
                        .filter(pokeValue -> !pokeValue.isInteractive())
                        .filter(pokeValue -> trainer.getChildren().size() < Constants.MAX_POKES_PER_TRAINER)
                        .forEach(pokeValue -> trainer.addPoke(pokeValue.getAddress(),
                                pokeValue.getValue()));
                return true;
            });
        }
    }

    @Override
    public void exportPokes(TrainerList trainerList, OutputStream os) throws IOException {
        try (PrintWriter writer = new PrintWriter(os)) {
            for (PokeViewable trainerViewable : trainerList.getChildren()) {
                Trainer trainer = (Trainer) trainerViewable;
                writer.format("%s%s\r\n", PokPoke.NEXT_TRAINER, trainer.getName());
                List<PokeViewable> pokeViewableList = trainer.getChildren();
                for (int i = 0; i < pokeViewableList.size() - 1; i++) {
                    Poke poke = (Poke) pokeViewableList.get(i);
                    writer.format("%s  8 %d %d 0\r\n", PokPoke.POKE_MARKER, poke.getAddress(), poke.getValue());
                }
                if (pokeViewableList.size() > 0) {
                    Poke poke = (Poke) pokeViewableList.get(pokeViewableList.size() - 1);
                    writer.format("%s  8 %d %d 0\r\n", PokPoke.LAST_POKE_MARKER, poke.getAddress(), poke.getValue());
                }
            }
            writer.print(PokPoke.LAST_LINE_MARKER);
            writer.flush();
        }
    }
}
