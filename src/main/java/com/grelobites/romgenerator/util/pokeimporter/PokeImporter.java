package com.grelobites.romgenerator.util.pokeimporter;

import com.grelobites.romgenerator.model.TrainerList;

import java.io.IOException;
import java.io.OutputStream;


public interface PokeImporter {

    void importPokes(TrainerList trainerList, ImportContext ctx) throws IOException;
    void exportPokes(TrainerList trainerList, OutputStream os) throws IOException;
}
