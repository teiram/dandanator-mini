package com.grelobites.romgenerator;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.PreferencesProvider;
import com.grelobites.romgenerator.view.MainAppController;
import com.grelobites.romgenerator.view.MultiplyUpgradeController;
import com.grelobites.romgenerator.view.util.DirectoryAwareFileChooser;
import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainApp.class);
    private static final String APP_NAME = "ROM Generator";

	private Stage primaryStage;
    private Stage preferencesStage;
    private Stage aboutStage;
    private TabPane preferencesPane;
    private TabPane aboutPane;
    private MenuToolkit menuToolkit;
    private ApplicationContext applicationContext;

    private Stage multiplyUpdaterStage;
    private VBox multiplyUpdaterPane;
    private MultiplyUpgradeController multiplyUpdaterController;

    private void populateMenuBar(MenuBar menuBar, Scene scene, ApplicationContext applicationContext) {
        Menu fileMenu = new Menu(LocaleUtil.i18n("fileMenuTitle"));

        fileMenu.getItems().addAll(
                importRomSetMenuItem(scene, applicationContext),
                mergeRomSetMenuItem(scene, applicationContext),
                exportGameMenuItem(applicationContext));

        if (menuToolkit == null) {
            fileMenu.getItems().add(new SeparatorMenuItem());
            fileMenu.getItems().add(preferencesMenuItem());
            fileMenu.getItems().add(new SeparatorMenuItem());
            fileMenu.getItems().add(quitMenuItem());
        }

        Menu extraMenu = new Menu(LocaleUtil.i18n("extraMenuTitle"));
        extraMenu.getItems().add(multiplyUpdaterMenuItem(applicationContext));
        extraMenu.visibleProperty().bind(Bindings.size(extraMenu.getItems()).greaterThan(0));
        applicationContext.setExtraMenu(extraMenu);

        menuBar.getMenus().addAll(fileMenu, extraMenu);

        if (menuToolkit == null) {
            Menu helpMenu = new Menu(LocaleUtil.i18n("helpMenuTitle"));
            helpMenu.getItems().add(aboutMenuItem());
            menuBar.getMenus().add(helpMenu);
        }
    }


    private MenuItem exportGameMenuItem(ApplicationContext applicationContext) {
        MenuItem exportGame = new MenuItem(LocaleUtil.i18n("exportGameMenuEntry"));
        exportGame.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+G")
        );
        exportGame.disableProperty().bind(applicationContext
                .gameSelectedProperty().not());
        exportGame.textProperty().bind(applicationContext
                .exportGameMenuEntryMessageProperty());

        exportGame.setOnAction(f -> {
            try {
                applicationContext.exportCurrentGame();
            } catch (Exception e) {
                LOGGER.error("Exporting current game", e);
            }
        });
        return exportGame;
    }


    private MenuItem importRomSetMenuItem(Scene scene, ApplicationContext applicationContext) {
        MenuItem importRomSet = new MenuItem(LocaleUtil.i18n("importRomSetMenuEntry"));
        importRomSet.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+I")
        );
        importRomSet.disableProperty().bind(applicationContext
                .backgroundTaskCountProperty().greaterThan(0));
        importRomSet.setOnAction(f -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("importRomSetChooser"));
            final File romSetFile = chooser.showOpenDialog(scene.getWindow());
            try {
                if (romSetFile != null) {
                    applicationContext.importRomSet(romSetFile);
                }
            } catch (Exception e) {
                LOGGER.error("Importing ROM Set from file " +  romSetFile, e);
            }
        });
        return importRomSet;
    }

    private MenuItem mergeRomSetMenuItem(Scene scene, ApplicationContext applicationContext) {
        MenuItem importRomSet = new MenuItem(LocaleUtil.i18n("mergeRomSetMenuEntry"));
        importRomSet.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+M")
        );
        importRomSet.disableProperty().bind(applicationContext
                .backgroundTaskCountProperty().greaterThan(0));
        importRomSet.setOnAction(f -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("mergeRomSetChooser"));
            final File romSetFile = chooser.showOpenDialog(scene.getWindow());
            try {
                if (romSetFile != null) {
                    applicationContext.mergeRomSet(romSetFile);
                }
            } catch (Exception e) {
                LOGGER.error("Merging ROM Set from file " +  romSetFile, e);
            }
        });
        return importRomSet;
    }

    private MenuItem multiplyUpdaterMenuItem(ApplicationContext applicationContext) {
        MenuItem cpldProgrammer = new MenuItem(LocaleUtil.i18n("multiplyUpdaterMenuEntry"));

        cpldProgrammer.disableProperty().bind(applicationContext
                .backgroundTaskCountProperty().greaterThan(0));
        cpldProgrammer.setOnAction(f -> {
            try {
                getMultiplyUpdaterStage().show();
            } catch (Exception e) {
                LOGGER.error("Trying to show Multiply Updater Stage", e);
            }
        });
        return cpldProgrammer;
    }

    public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
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
            Scene preferencesScene = new Scene(getPreferencesPane());
            preferencesScene.getStylesheets().add(Constants.getThemeResourceUrl());
            preferencesStage.setScene(preferencesScene);
            preferencesStage.setTitle(LocaleUtil.i18n("preferencesStageTitle"));
            preferencesStage.initModality(Modality.WINDOW_MODAL);
            preferencesStage.initOwner(primaryStage.getOwner());
            preferencesStage.setResizable(false);
        }
        return preferencesStage;
    }

    private TabPane getAboutPane() throws IOException {
        if (aboutPane == null) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/about.fxml"));
            loader.setResources(LocaleUtil.getBundle());
            aboutPane = loader.load();
        }
        return aboutPane;
    }

    private Stage getAboutStage() throws IOException {
        if (aboutStage == null) {
            aboutStage = new Stage();
            Scene aboutScene = new Scene(getAboutPane());
            aboutScene.getStylesheets().add(Constants.getThemeResourceUrl());
            aboutStage.setScene(aboutScene);
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

    private Pane getApplicationPane() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(MainApp.class.getResource("view/mainapp.fxml"));
        loader.setResources(LocaleUtil.getBundle());
        loader.setController(new MainAppController(applicationContext));
        return loader.load();
    }

    private VBox getMultiplyUpdaterPane() throws IOException {
        if (multiplyUpdaterPane == null) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/multiplyUpdater.fxml"));
            loader.setResources(LocaleUtil.getBundle());
            multiplyUpdaterController = new MultiplyUpgradeController(applicationContext);
            loader.setController(multiplyUpdaterController);
            multiplyUpdaterPane = loader.load();
        }
        return multiplyUpdaterPane;
    }

    private Stage getMultiplyUpdaterStage() throws IOException {
        if (multiplyUpdaterStage == null) {
            multiplyUpdaterStage = new Stage();
            Scene scene = new Scene(getMultiplyUpdaterPane());
            scene.getStylesheets().add(Constants.getThemeResourceUrl());
            multiplyUpdaterStage.setScene(scene);
            multiplyUpdaterStage.setTitle(LocaleUtil.i18n("multiplyUpdaterStageTitle"));
            multiplyUpdaterStage.initModality(Modality.APPLICATION_MODAL);
            multiplyUpdaterStage.initOwner(primaryStage.getOwner());
            multiplyUpdaterStage.setResizable(false);
            multiplyUpdaterStage.setOnHiding((e) -> multiplyUpdaterController.resetView());
        }
        return multiplyUpdaterStage;
    }

    private void initRootLayout() {
		try {
		    applicationContext = new ApplicationContext();
            primaryStage.titleProperty().bind(applicationContext.applicationTitleProperty());
            primaryStage.setOnCloseRequest(e -> Platform.exit());
            BorderPane mainPane = new BorderPane();

            mainPane.autosize();
            Scene scene = new Scene(mainPane);
            scene.getStylesheets().add(Constants.getThemeResourceUrl());
            menuToolkit = MenuToolkit.toolkit(Locale.getDefault());
            MenuBar menuBar = initMenuBar();
            if (menuToolkit == null) {
                mainPane.setTop(menuBar);
            } else {
                menuBar.setUseSystemMenuBar(true);
                menuToolkit.setGlobalMenuBar(menuBar);
            }
            populateMenuBar(menuBar, scene, applicationContext);
            Pane applicationPane = getApplicationPane();
            mainPane.setCenter(applicationPane);

            primaryStage.setScene(scene);
            applicationContext.setApplicationStage(primaryStage);

            //Force preference controllers initialization
            getPreferencesPane();

            if (menuToolkit != null) {
                menuToolkit.setMenuBar(primaryStage, menuBar);
            }
            primaryStage.setResizable(true);
            primaryStage.show();
            primaryStage.setMinHeight(primaryStage.getHeight());
            primaryStage.setMinWidth(primaryStage.getWidth());

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
