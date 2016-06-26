package com.grelobites.dandanator;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.grelobites.dandanator.view.DandanatorController;
import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainApp.class);

	private Stage primaryStage;
	private VBox rootLayout;

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("Dandanator Mini ROM Generator");
		/*
		this.primaryStage.getIcons()
			.add(new Image("file:resources/images/address_book_32.png"));
		*/
		initRootLayout();
		
	}

    public Menu createDefaultApplicationMenu(String appName) {
        MenuToolkit tk = MenuToolkit.toolkit(Locale.getDefault());
        return new Menu(appName, null,
                tk.createAboutMenuItem(appName),
                new SeparatorMenuItem(),
                new MenuItem("Preferences"),
                new SeparatorMenuItem(),
                tk.createHideMenuItem(appName),
                tk.createHideOthersMenuItem(),
                tk.createUnhideAllMenuItem(),
                new SeparatorMenuItem(),
                tk.createQuitMenuItem(appName));
    }

    private static void populateMenuBar(MenuBar menuBar, Scene scene, DandanatorController controller) {
        Menu fileMenu = new Menu("File");
        MenuItem importRomSet = new MenuItem("Import ROM Set...");
        importRomSet.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+I")
        );

        importRomSet.setOnAction(f -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import ROM Set");
            final File romSetFile = chooser.showOpenDialog(scene.getWindow());
            try {
                controller.importRomSet(romSetFile);
            } catch (Exception e) {
                LOGGER.error("Importing ROM Set from file " +  romSetFile, e);
            }
        });
        fileMenu.getItems().add(importRomSet);
        menuBar.setUseSystemMenuBar(true);
        menuBar.getMenus().add(fileMenu);
    }

	private void initRootLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/dandanator.fxml"));
			rootLayout = loader.load();
            Scene scene = new Scene(rootLayout);

			// Create a new menu bar
            MenuBar bar = new MenuBar();
			MenuToolkit tk = MenuToolkit.toolkit(Locale.getDefault());

            Menu applicationMenu = createDefaultApplicationMenu("Dandanator Mini");

			bar.getMenus().add(applicationMenu);
            populateMenuBar(bar, scene, loader.getController());

			tk.setGlobalMenuBar(bar);

			primaryStage.setScene(scene);
			primaryStage.setResizable(false);
            tk.setMenuBar(primaryStage, bar);

            primaryStage.show();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}



}
