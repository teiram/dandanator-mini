package com.grelobites.romgenerator.util.pokeimporter;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportContext {
    private File pokesFile;
    private List<String> importErrors;

    public ImportContext(File pokesFile) {
        this.pokesFile = pokesFile;
    }

    public InputStream getPokeStream() throws FileNotFoundException {
        return new FileInputStream(pokesFile);
    }

    public File getPokesFile() {
        return pokesFile;
    }

    public void setPokesFile(File pokesFile) {
        this.pokesFile = pokesFile;
    }

    public List<String> getImportErrors() {
        return importErrors;
    }

    public void setImportErrors(List<String> importErrors) {
        this.importErrors = importErrors;
    }

    public void addImportError(String importError) {
        if (importErrors == null) {
            importErrors = new ArrayList<>();
        }
        importErrors.add(importError);
    }

    public boolean hasErrors() {
        return importErrors != null && importErrors.size() > 0;
    }
}
