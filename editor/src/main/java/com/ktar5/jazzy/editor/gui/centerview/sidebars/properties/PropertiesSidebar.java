package com.ktar5.jazzy.editor.gui.centerview.sidebars.properties;

import com.ktar5.jazzy.editor.Main;
import com.ktar5.jazzy.editor.properties.ParentProperty;
import com.ktar5.jazzy.editor.properties.Property;
import com.ktar5.jazzy.editor.properties.RootProperty;
import com.ktar5.jazzy.editor.properties.StringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.MapChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class PropertiesSidebar extends Pane {
    private RootProperty root;

    public PropertiesSidebar(RootProperty root) {
        super();
        this.root = root;

        this.setBackground(new Background(new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY)));
        this.setMaxWidth(500);

        TreeItem<Property> rootNode = new TreeItem<>(root);
        setMapListener(rootNode);
        for (Property property : root.getChildren().values()) {
            addNode(rootNode, property);
        }
        rootNode.setExpanded(true);

        //name column
        TreeTableColumn<Property, String> nameColumn = new TreeTableColumn<>("Property");
        nameColumn.setCellFactory(p -> new PropertyCell(root));
        nameColumn.setCellValueFactory(param -> param.getValue().getValue().nameProperty);

        //data column
        TreeTableColumn<Property, String> dataColumn = new TreeTableColumn<>("Value");
        dataColumn.setCellFactory(param -> new PropertyCell(root));
        dataColumn.setCellValueFactory(param -> {
            if (param.getValue().getValue() instanceof ParentProperty)
                return new ReadOnlyStringWrapper("");
            else
                return ((StringProperty) param.getValue().getValue()).valueProperty;
        });

        //Creating a tree table view
        final TreeTableView<Property> treeTableView = new TreeTableView<>(rootNode);
        treeTableView.getColumns().add(nameColumn);
        treeTableView.getColumns().add(dataColumn);
        treeTableView.setShowRoot(false);
        treeTableView.setEditable(true);

        treeTableView.prefWidthProperty().bind(this.widthProperty());
        treeTableView.prefHeightProperty().bind(this.heightProperty());

        this.widthProperty().addListener((observable, oldValue, newValue) -> {
            setPrefWidths(newValue.intValue(), nameColumn, dataColumn, .55);
        });

        treeTableView.setContextMenu(new PropertiesRootRClickMenu(root));

        final String cssUrl1 = getClass().getResource("/tableview.css").toExternalForm();
        Main.root.getScene().getStylesheets().add(cssUrl1);

        this.getChildren().add(treeTableView);
    }

    public void setPrefWidths(int newWidth, TreeTableColumn smaller, TreeTableColumn larger, double smallerRatio) {
        final int smallerWidth = (int) (newWidth * smallerRatio);
        final int largerWidth = newWidth - smallerWidth - 2;
        smaller.setPrefWidth(smallerWidth);
        larger.setPrefWidth(largerWidth);
    }

    public void addNode(TreeItem<Property> parent, Property property) {
        TreeItem<Property> childNode = new TreeItem<>(property);
        parent.getChildren().add(childNode);
        if (property instanceof ParentProperty) {
            setMapListener(childNode);
            for (Property descendant : ((ParentProperty) property).getChildren().values()) {
                addNode(childNode, descendant);
            }
        }
    }

    public void setMapListener(TreeItem<Property> rootNode) {
        if (!(rootNode.getValue() instanceof ParentProperty)) {
            return;
        }
        ParentProperty property = ((ParentProperty) rootNode.getValue());
        property.getChildren().addListener((MapChangeListener<String, Property>) change -> {
            if (change.wasAdded()) {
                addNode(rootNode, change.getValueAdded());
            } else if (change.wasRemoved()) {
                rootNode.getChildren().removeIf(p -> p.getValue() == change.getValueRemoved());
            }
        });
    }
}
