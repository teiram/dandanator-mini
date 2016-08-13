package com.grelobites.romgenerator.util.pokeimporter.importers.pok;


import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.model.Poke;
import com.grelobites.romgenerator.model.PokeViewable;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.model.TrainerList;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.util.pokeimporter.PokeImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

public class PoKPokeImporter implements PokeImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PoKPokeImporter.class);

    private static boolean isCompatibleSpectrum48K(ImportContext ctx, PokValue value) {
        if (!value.isCompatibleSpectrum48K()) {
            ctx.addImportError(LocaleUtil.i18n("non48KCompatiblePokesPresent"));
            return false;
        } else {
            return true;
        }
    }

    private static boolean isInteractive(ImportContext ctx, PokValue value) {
        if (value.isInteractive()) {
            ctx.addImportError(LocaleUtil.i18n("interactivePokesPresent"));
            return false;
        } else {
            return true;
        }
    }

    private static boolean enoughSize(ImportContext ctx, Trainer trainer) {
        if (trainer.getChildren().size() >= DandanatorMiniConstants.MAX_POKES_PER_TRAINER) {
            ctx.addImportError(LocaleUtil.i18n("maximumPokesPerTrainerExhausted"));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void importPokes(TrainerList trainerList, ImportContext ctx) throws IOException {
        PokPoke poke = PokPoke.fromInputStream(ctx.getPokeStream());
        for (PokTrainer pokTrainer: poke.getTrainers()) {
            if (trainerList.getChildren().size() >= DandanatorMiniConstants.MAX_TRAINERS_PER_GAME) {
                ctx.addImportError(LocaleUtil.i18n("maximumTrainersPerGameExhausted"));
                break;
            }
            trainerList.addTrainerNode(Util.substring(pokTrainer.getName(),
                    DandanatorMiniConstants.POKE_EFFECTIVE_NAME_SIZE)).map(trainer -> {
                pokTrainer.getPokeValues().stream()
                        .filter(pokeValue -> isCompatibleSpectrum48K(ctx, pokeValue))
                        .filter(pokeValue -> isInteractive(ctx, pokeValue))
                        .filter(pokeValue -> enoughSize(ctx, trainer))
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
                    writer.format("%s  8 %d %d %d\r\n", PokPoke.LAST_POKE_MARKER, poke.getAddress(),
                            poke.getValue(), poke.getOriginalValue());
                }
            }
            writer.print(PokPoke.LAST_LINE_MARKER);
            writer.flush();
        }
    }
}
