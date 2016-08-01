package com.grelobites.romgenerator.util.pokeimporter.importers.pok;

import java.util.ArrayList;
import java.util.List;


public class PokTrainer {

    private String name;
    private List<PokValue> pokeValues;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PokValue> getPokeValues() {
        return pokeValues;
    }

    public void setPokeValues(List<PokValue> pokeValues) {
        this.pokeValues = pokeValues;
    }

    public void addPokeValue(PokValue pokeValue) {
        if (pokeValues == null) {
            pokeValues = new ArrayList<>();
        }
        pokeValues.add(pokeValue);
    }

    @Override
    public String toString() {
        return "PokTrainer{" +
                "name='" + name + '\'' +
                ", pokeValues=" + pokeValues +
                '}';
    }
}
