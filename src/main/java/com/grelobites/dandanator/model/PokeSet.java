package com.grelobites.dandanator.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mteira on 21/6/16.
 */
public class PokeSet {

    private List<Poke> pokeList;

    public List<Poke> getPokeList() {
        return pokeList;
    }

    public void setPokeList(List<Poke> pokeList) {
        this.pokeList = pokeList;
    }

    public void addPoke(Poke poke) {
        if (this.pokeList == null) {
            this.pokeList = new ArrayList<Poke>();
        }
        pokeList.add(poke);
    }
}
