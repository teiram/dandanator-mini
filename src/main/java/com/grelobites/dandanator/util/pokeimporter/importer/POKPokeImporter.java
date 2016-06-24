package com.grelobites.dandanator.util.pokeimporter.importer;


import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.TrainerList;
import com.grelobites.dandanator.model.poke.pok.PokPoke;
import com.grelobites.dandanator.model.poke.pok.PokTrainer;
import com.grelobites.dandanator.model.poke.pok.PokValue;
import com.grelobites.dandanator.util.pokeimporter.PokeImporter;

import java.io.IOException;
import java.io.InputStream;

public class POKPokeImporter implements PokeImporter {
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
}
