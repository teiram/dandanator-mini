package com.grelobites.romgenerator.view.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class DirectoryAwareFileChooser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryAwareFileChooser.class);

    private FileChooser delegate = new FileChooser();

    private static Optional<File> getFileDirectory(List<File> files) {
        if (files != null && files.size() > 0) {
            return getFileDirectory(files.get(0));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<File> getFileDirectory(File file) {
        LOGGER.debug("getFileDirectory for " + file);
        if (file != null && file.isFile()) {
            return Optional.ofNullable(file.getParentFile());
        } else {
            return Optional.empty();
        }
    }

    public File showOpenDialog(Window ownerWindow) {
        File result = delegate.showOpenDialog(ownerWindow);

        getFileDirectory(result).ifPresent(f -> delegate.setInitialDirectory(f));
        return result;
    }

    public List<File> showOpenMultipleDialog(Window ownerWindow) {
        List<File> result =  delegate.showOpenMultipleDialog(ownerWindow);

        getFileDirectory(result).ifPresent(f -> delegate.setInitialDirectory(f));
        return result;
    }

    public File showSaveDialog(Window ownerWindow) {
        File result = delegate.showSaveDialog(ownerWindow);

        getFileDirectory(result).ifPresent(f -> delegate.setInitialDirectory(f));
        return result;
    }

    public void setInitialFileName(String name) {
        delegate.setInitialFileName(name);
    }

    public void setTitle(String title) {
        delegate.setTitle(title);
    }
}
