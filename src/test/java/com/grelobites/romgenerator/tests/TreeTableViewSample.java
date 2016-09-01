package com.grelobites.romgenerator.tests;

import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;


public class TreeTableViewSample extends Application {

    public static void main(String[] args) {
        Application.launch(TreeTableViewSample.class, args);
    }

    @Override
    public void start(Stage stage) {
        PokeNode pokeNode1 = new PokeNode("First Entry");
        pokeNode1.addAddressValue(15535L, 1L);
        pokeNode1.addAddressValue(20000L, 255L);
        pokeNode1.addAddressValue(21000L, 22L);

        PokeNode pokeNode2 = new PokeNode("Second Entry");
        pokeNode2.addAddressValue(32768L, 44L);

        PokeViewable root = new PokeViewable();
        root.addPokeNode(pokeNode1);
        root.addPokeNode(pokeNode2);

        TreeView<PokeViewable> tree = new TreeView<>(
                new RecursiveTreeItem<PokeViewable>(root, PokeViewable::getChildren));
        tree.setShowRoot(false);
        tree.setEditable(true);
        tree.setCellFactory((a)  -> {
            return new TreeCell<PokeViewable>() {
                private TextField textField;
                @Override
                public void cancelEdit() {
                    super.cancelEdit();
                }
                @Override
                public void startEdit() {
                    super.startEdit();
                    if(textField == null)
                        createTextField();
                        setText(null);
                        setGraphic(textField);
                        textField.selectAll();
                    }
                    private void createTextField() {
                        textField = new TextField(getItem().getValue());
                        textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
                        public void handle(KeyEvent e) {
                            if (e.getCode() == KeyCode.ENTER) {
                                getItem().update(textField.getText());
                                commitEdit(getItem());
                            } else if(e.getCode() == KeyCode.ESCAPE) {
                                cancelEdit();
                            }
                        }
                    });
                }

                @Override
                protected void updateItem(PokeViewable item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (isEditing()) {
                            if (textField != null) {
                                textField.setText(item.getValue());
                            }
                            setText(null);
                            setGraphic(textField);
                            } else {
                                setText(item.getValue());
                            }
                            setGraphic(null);
                            setText(item.getValue());
                            setGraphic(null);
                        }
                    }

            };

        });

        stage.setTitle("Tree Table View Sample");
        final Scene scene = new Scene(new Group(), 400, 400);
        scene.setFill(Color.LIGHTGRAY);
        Group sceneRoot = (Group) scene.getRoot();

        Button button = new Button("Show model");
        button.setOnAction(c -> {
            System.out.println("Showing model");
            System.out.println(root.asString());
        });

        Button addPokeButton = new Button("Add poke");
        addPokeButton.setOnAction(c -> {
            if (tree.getSelectionModel().getSelectedItem() != null) {
                System.out.println("Added new child");
                tree.getSelectionModel().getSelectedItem().getValue()
                        .addNewChild();
            }

        });
        Button addCategoryButton = new Button("Add category");
        addCategoryButton.setOnAction(c -> {
           tree.getRoot().getValue().addNewChild();
        });

        tree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
            if (newValue.getValue() instanceof PokeNode) {
                addCategoryButton.setDisable(true);
                addPokeButton.setDisable(false);
            } else {
                addCategoryButton.setDisable(false);
                addPokeButton.setDisable(true);
            }
                });


        VBox box = new VBox();
        box.getChildren().add(tree);
        box.getChildren().add(button);
        box.getChildren().add(addPokeButton);
        box.getChildren().add(addCategoryButton);

        sceneRoot.getChildren().add(box);
        stage.setScene(scene);
        stage.show();
    }

    public class PokeViewable {
        private ObservableList<PokeViewable> children = FXCollections.observableArrayList();

        public ObservableList<PokeViewable> getChildren() {
            return children;
        }
        public void update(String value) {

        }

        public void addNewChild() {
            children.add(new PokeNode("New Poke"));
        }
        public String getValue() {
            return "";
        }

        public void addPokeNode(PokeNode pokeNode) {
            children.add(pokeNode);
        }

        public String asString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Root node {");
            for (PokeViewable child : children) {
                builder.append(child.asString());
            }
            builder.append("}\n");
            return builder.toString();
        }
    }


    public class PokeNode extends PokeViewable {
        private SimpleStringProperty nameProperty;

        private ObservableList<PokeViewable> addressValueList = FXCollections.observableArrayList();

        public PokeNode(String name) {
            this.nameProperty = new SimpleStringProperty(name);
        }

        public void addNewChild() {
            addressValueList.add(new AddressValueNode(0L, 0L));
        }
        public SimpleStringProperty nameProperty() {
            return nameProperty;
        }
        public String getName() {
            return nameProperty.get();
        }
        public void setName(String name) {
            this.nameProperty.set(name);
        }

        public void update(String value) {
            setName(value);
        }

        public String getValue() {
            return getName();
        }
        public void addAddressValue(Long address, Long value) {
            addressValueList.add(new AddressValueNode(address, value));
        }

        public ObservableList<PokeViewable> getChildren() {
            return addressValueList;
        }

        public String asString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Poke {" + getValue());
            for (PokeViewable child : addressValueList) {
                builder.append(child.asString());
            }
            builder.append("}");
            return builder.toString();
        }


    }

    public class AddressValueNode extends PokeViewable {
        private LongProperty addressProperty;
        private LongProperty valueProperty;
        private ObservableList<PokeViewable> children = FXCollections.emptyObservableList();

        public AddressValueNode(Long address, Long value) {
            this.addressProperty = new SimpleLongProperty(address);
            this.valueProperty = new SimpleLongProperty(value);
        }

        public LongProperty addressProperty() {
            return this.addressProperty;
        }

        public LongProperty valueProperty() {
            return this.valueProperty;
        }

        public Long getAddress() {
            return addressProperty.get();
        }

        public void setAddress(Long address) {
            addressProperty.set(address);
        }

        public void update(String value) {
            String[] pair = value.split(",");
            this.addressProperty.set(Long.parseLong(pair[0]));
            this.valueProperty.set(Long.parseLong(pair[1]));
        }

        public String getValue() {
            return String.format("%d,%d", getAddress(), getPokeValue());
        }
        public Long getPokeValue() {
            return valueProperty.get();
        }

        public void setPokeValue(Long value) {
            this.valueProperty.set(value);
        }

        public ObservableList<PokeViewable> getChildren() {
            return children;
        }

        public String asString() {
            StringBuilder builder = new StringBuilder();
            builder.append("{AddressValue " + getAddress() + "," + getPokeValue() + "}");
            return builder.toString();
        }

    }

    public class RecursiveTreeItem<T> extends TreeItem<T> {

        private Callback<T, ObservableList<T>> childrenFactory;

        public RecursiveTreeItem(Callback<T, ObservableList<T>> func){
            this(null, func);
        }

        public RecursiveTreeItem(final T value, Callback<T, ObservableList<T>> func){
            this(value, (Node) null, func);
        }

        public RecursiveTreeItem(final T value, Node graphic, Callback<T, ObservableList<T>> func){
            super(value, graphic);
            System.out.println("Creating new RecursiveTreeItem for " + value);

            this.childrenFactory = func;

            if(value != null) {
                addChildrenListener(value);
            }

            valueProperty().addListener((obs, oldValue, newValue)->{
                if(newValue != null){
                    addChildrenListener(newValue);
                }
            });
        }

        private void addChildrenListener(T value){
            final ObservableList<T> children = childrenFactory.call(value);

            children.forEach(child ->
                    RecursiveTreeItem.this.getChildren().add(
                            new RecursiveTreeItem<>(child, getGraphic(), childrenFactory)));

            children.addListener((ListChangeListener<T>) change -> {
                while(change.next()){
System.out.println("Detected a change in listener " + change);
                    if(change.wasAdded()){
                        change.getAddedSubList()
                                .forEach(t->
                                        RecursiveTreeItem.this.getChildren().add(
                                                new RecursiveTreeItem<>(t, getGraphic(), childrenFactory)));
                    }

                    if(change.wasRemoved()){
                        change.getRemoved().forEach(t->{
                            final List<TreeItem<T>> itemsToRemove = RecursiveTreeItem.this.getChildren()
                                    .stream()
                                    .filter(treeItem ->
                                            treeItem.getValue().equals(t)).collect(Collectors.toList());

                            RecursiveTreeItem.this.getChildren().removeAll(itemsToRemove);
                        });
                    }

                }
            });
        }
    }

}
