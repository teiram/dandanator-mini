package com.grelobites.romgenerator;

/**
 * This class just dependency checks on the bootstrap process, removing dependencies from javafx
 * what is not detected properly on startup
 */
public class MainShade {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
