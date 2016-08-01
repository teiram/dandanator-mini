package com.grelobites.romgenerator;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.PreferencesProvider;
import com.grelobites.romgenerator.view.MainAppController;
import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainApp.class);
    private static final String APP_NAME = "Dandanator Mini ROM Generator";

	private Stage primaryStage;
    private Stage preferencesStage;
    private Stage aboutStage;
    private TabPane preferencesPane;
    private AnchorPane aboutPane;
    private MenuToolkit menuToolkit;

    private void populateMenuBar(MenuBar menuBar, Scene scene, MainAppController controller) {
        Menu fileMenu = new Menu(LocaleUtil.i18n("fileMenuTitle"));

        fileMenu.getItems().addAll(
                importRomSetMenuItem(scene, controller),
                exportPokesMenuItem(scene, controller),
                exportGameMenuItem(scene, controller));

        if (menuToolkit == null) {
            fileMenu.getItems().add(new SeparatorMenuItem());
            fileMenu.getItems().add(preferencesMenuItem());
            fileMenu.getItems().add(new SeparatorMenuItem());
            fileMenu.getItems().add(quitMenuItem());
        }
        menuBar.getMenus().add(fileMenu);
        if (menuToolkit == null) {
            Menu helpMenu = new Menu(LocaleUtil.i18n("helpMenuTitle"));
            helpMenu.getItems().add(aboutMenuItem());
            menuBar.getMenus().add(helpMenu);
        }
    }

    private MenuItem exportPokesMenuItem(Scene scene, MainAppController controller) {
        MenuItem exportPokes = new MenuItem(LocaleUtil.i18n("exportPokesMenuEntry"));
        exportPokes.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+P")
        );

        exportPokes.setOnAction(f -> {
            try {
                controller.exportCurrentGamePokes();
            } catch (Exception e) {
                LOGGER.error("Exporting current game pokes", e);
            }
        });
        return exportPokes;
    }

    private MenuItem exportGameMenuItem(Scene scene, MainAppController controller) {
        MenuItem exportPokes = new MenuItem(LocaleUtil.i18n("exportGameMenuEntry"));
        exportPokes.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+G")
        );

        exportPokes.setOnAction(f -> {
            try {
                controller.exportCurrentGame();
            } catch (Exception e) {
                LOGGER.error("Exporting current game", e);
            }
        });
        return exportPokes;
    }

    private MenuItem importRomSetMenuItem(Scene scene, MainAppController controller) {
        MenuItem importRomSet = new MenuItem(LocaleUtil.i18n("importRomSetMenuEntry"));
        importRomSet.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+I")
        );

        importRomSet.setOnAction(f -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("importRomSetChooser"));
            final File romSetFile = chooser.showOpenDialog(scene.getWindow());
            try {
                if (romSetFile != null) {
                    controller.importRomSet(romSetFile);
                }
            } catch (Exception e) {
                LOGGER.error("Importing ROM Set from file " +  romSetFile, e);
            }
        });
        return importRomSet;
    }

	public static void main(String[] args) {
		launch(args);
	}

    private Configuration getConfiguration() {
        return Configuration.getInstance();
    }

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("Dandanator Mini ROM Generator");

		this.primaryStage.getIcons()
			.add(new Image(MainApp.class.getResourceAsStream("/romgenerator-icon.png")));

		initRootLayout();

	}

    private TabPane getPreferencesPane() throws IOException {
        if (preferencesPane == null) {
            preferencesPane = new TabPane();
            for (Tab tab: PreferencesProvider.preferenceTabs()) {
                preferencesPane.getTabs().add(tab);
            }
        }
        return preferencesPane;
    }

    private Stage getPreferencesStage() throws IOException {
        if (preferencesStage == null) {
            preferencesStage = new Stage();
            preferencesStage.setScene(new Scene(getPreferencesPane()));
            preferencesStage.setTitle(LocaleUtil.i18n("preferencesStageTitle"));
            preferencesStage.initModality(Modality.WINDOW_MODAL);
            preferencesStage.initOwner(primaryStage.getOwner());
            preferencesStage.setResizable(false);
        }
        return preferencesStage;
    }

    private AnchorPane getAboutPane() throws IOException {
        if (aboutPane == null) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/about.fxml"));
            aboutPane = loader.load();
        }
        return aboutPane;
    }

    private Stage getAboutStage() throws IOException {
        if (aboutStage == null) {
            aboutStage = new Stage();
            aboutStage.setScene(new Scene(getAboutPane()));
            aboutStage.setTitle("");
            aboutStage.initModality(Modality.APPLICATION_MODAL);
            aboutStage.initOwner(primaryStage.getOwner());
            aboutStage.setResizable(false);
        }
        return aboutStage;
    }

    private void showPreferencesStage() {
        try {
            getPreferencesStage().show();
        } catch (Exception e) {
            LOGGER.error("Trying to show preferences stage", e);
        }
    }

    private MenuItem preferencesMenuItem() {
        MenuItem preferencesMenuItem = new MenuItem(LocaleUtil.i18n("preferencesMenuEntry"));
        preferencesMenuItem.setAccelerator(KeyCombination.keyCombination("SHORTCUT+,"));
        preferencesMenuItem.setOnAction(event -> showPreferencesStage());

        return preferencesMenuItem;
    }

    private void showAboutStage() {
        try {
            getAboutStage().show();
        } catch (Exception e) {
            LOGGER.error("Trying to show about stage", e);
        }
    }

    private MenuItem aboutMenuItem() {
        MenuItem aboutMenuItem = new MenuItem(LocaleUtil.i18n("aboutMenuEntry"));
        aboutMenuItem.setOnAction(event -> showAboutStage());
        return aboutMenuItem;
    }

    public static MenuItem quitMenuItem() {
        MenuItem menuItem = new MenuItem(LocaleUtil.i18n("quitMenuEntry"));
        menuItem.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+Q"));

        menuItem.setOnAction(e -> Platform.exit());
        return menuItem;
    }

    public Menu createApplicationMenu(String appName) throws IOException {
        if (menuToolkit != null) {
            return new Menu(appName, null,
                    menuToolkit.createAboutMenuItem(appName, getAboutStage()),
                    new SeparatorMenuItem(),
                    preferencesMenuItem(),
                    new SeparatorMenuItem(),
                    menuToolkit.createHideMenuItem(appName),
                    menuToolkit.createHideOthersMenuItem(),
                    menuToolkit.createUnhideAllMenuItem(),
                    new SeparatorMenuItem(),
                    menuToolkit.createQuitMenuItem(appName));
        } else {
            return null;
        }
    }

    private MenuBar initMenuBar() throws IOException {
        MenuBar menuBar = new MenuBar();
        if (menuToolkit != null) {
            Menu applicationMenu = createApplicationMenu(APP_NAME);
            menuBar.getMenus().add(applicationMenu);
        }
        return menuBar;
    }
	
	private void initRootLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/mainapp.fxml"));
            loader.setResources(LocaleUtil.getBundle());
			BorderPane applicationPane = loader.load();
            menuToolkit = MenuToolkit.toolkit(Locale.getDefault());
            MenuBar menuBar = initMenuBar();
            if (menuToolkit == null) {
                applicationPane.setTop(menuBar);
            } else {
                menuBar.setUseSystemMenuBar(true);
                menuToolkit.setGlobalMenuBar(menuBar);
            }
            Scene scene = new Scene(applicationPane);
            populateMenuBar(menuBar, scene, loader.getController());
 			primaryStage.setScene(scene);

            if (menuToolkit != null) {
                menuToolkit.setMenuBar(primaryStage, menuBar);
            }
            primaryStage.setResizable(false);
            primaryStage.show();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
