package com.grelobites.romgenerator.exomizer;


public class PreCalc {
    private MatchNode single;
    private Match cache;

    public PreCalc(MatchNode single) {
        this.single = single;
    }

    public MatchNode getSingle() {
        return single;
    }

    public void setCache(Match cache) {
        this.cache = cache;
    }

    public Match getCache() {
        return cache;
    }
}
