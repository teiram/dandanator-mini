package com.grelobites.dandanator.util.pokeimporter;

import com.grelobites.dandanator.model.TrainerList;

import java.io.IOException;
import java.io.InputStream;


public interface PokeImporter {

    void importPokes(TrainerList trainerList, InputStream is) throws IOException;
}
