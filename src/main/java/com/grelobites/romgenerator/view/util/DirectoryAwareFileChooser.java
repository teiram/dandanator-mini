package com.grelobites.romgenerator.view.util;

import com.grelobites.romgenerator.Configuration;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class DirectoryAwareFileChooser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryAwareFileChooser.class);

    private FileChooser delegate = new FileChooser();

    private void setInitialDirectory(File file) {
        delegate.setInitialDirectory(file);
        try {
            Configuration.getInstance().setLastUsedDirectory(file.getCanonicalPath());
        } catch (IOException ioe) {
            LOGGER.info("Unable to get canonical path from last used directory {}", file);
        }
    }

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

        getFileDirectory(result).ifPresent(this::setInitialDirectory);
        return result;
    }

    public List<File> showOpenMultipleDialog(Window ownerWindow) {
        List<File> result =  delegate.showOpenMultipleDialog(ownerWindow);

        getFileDirectory(result).ifPresent(this::setInitialDirectory);
        return result;
    }

    public File showSaveDialog(Window ownerWindow) {
        File result = delegate.showSaveDialog(ownerWindow);

        getFileDirectory(result).ifPresent(this::setInitialDirectory);
        return result;
    }

    private static Optional<File> asDirectory(String value) {
        File file = new File(value);
        return file.isDirectory() ? Optional.of(file) : Optional.empty();
    }

    public void setInitialDirectory(String name) {
        asDirectory(name).ifPresent(f -> delegate.setInitialDirectory(f));
    }

    public void setInitialFileName(String name) {
        delegate.setInitialFileName(name);
    }

    public void setTitle(String title) {
        delegate.setTitle(title);
    }
}
