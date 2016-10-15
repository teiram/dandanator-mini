package com.grelobites.romgenerator.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PreferencesProvider implements Comparable<PreferencesProvider> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesProvider.class);

    public static final int PRECEDENCE_GLOBAL = 1;
    public static final int PRECEDENCE_HANDLERS = 2;
    public static final int PRECEDENCE_OTHER = 3;

    public static final List<PreferencesProvider> providers = new ArrayList<>();

    private String name;
    private String fXmlLocation;
    private int precedence;


    public PreferencesProvider(String name, String fXmlLocation, int precedence) {
        this.name = name;
        this.fXmlLocation = fXmlLocation;
        this.precedence = precedence;
        LOGGER.debug("Adding preferences provider " + this);
        providers.add(this);
    }

    private static Optional<Tab> getTab(PreferencesProvider provider) {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(PreferencesProvider.class.getResource(provider.fXmlLocation));
        loader.setResources(LocaleUtil.getBundle());
        try {
            Node preferencesPane = loader.load();
            Tab tab = new Tab(provider.name, preferencesPane);
            tab.setClosable(false);
            return Optional.of(tab);
        } catch (Exception e) {
            LOGGER.error("Loading preferences tab from " + provider.fXmlLocation, e);
            return Optional.empty();
        }
    }

    public static List<Tab> preferenceTabs() throws IOException {
        providers.sort(null);
        return providers.stream().map(PreferencesProvider::getTab).filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList());
    }

    @Override
    public int compareTo(PreferencesProvider o) {
        return Integer.valueOf(precedence).compareTo(o.precedence);
    }

    @Override
    public String toString() {
        return "PreferencesProvider{" +
                "name='" + name + '\'' +
                ", fXmlLocation='" + fXmlLocation + '\'' +
                ", precedence=" + precedence +
                '}';
    }

}
