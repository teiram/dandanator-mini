package com.grelobites.romgenerator.zxspectrum;

@FunctionalInterface
public interface Interceptor {
    void intercept(Z80 cpu);
}
