import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.io.*;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.scene.control.cell.*;
import javafx.beans.property.*;

public class FridgeFX extends Application {

	public static final int ID_COL_MIN_WIDTH = 50;
	public static final int ITEM_COL_MIN_WIDTH = 150;
	public static final int QUANTITY_COL_MIN_WIDTH = 50;
	public static final int SECTION_COL_MIN_WIDTH = 100;
	public static final int BOUGHT_COL_MIN_WIDTH = 100;
	public static final int TABLE_VIEW_MIN_HEIGHT = 400;
	
	// used as ChoiceBox value for filter
	public enum FILTER_COLUMNS {
		ITEM,
		SECTION,
		BOUGHT_DAYS_AGO
	};
	
	// the data source controller
	private FridgeDSC fridgeDSC;
	

	public void init() throws Exception {
		// creating an instance of the data source controller to be used
		// in this application
		fridgeDSC = new FridgeDSC();

		try
		{
			fridgeDSC.connect();
		}
		catch(Exception exception)
		{
			System.out.println("ERROR: " + exception);
		}
	}

	public void start(Stage stage) throws Exception {

		build(stage);
		stage.setTitle(getClass().getName());
		stage.show();
		 
		Thread.currentThread().setUncaughtExceptionHandler((thread, exception) ->
		{
			System.out.println("ERROR: " + exception);
		});
	}

	public void build(Stage stage) throws Exception {
		
		VBox root = new VBox();
		//stage.setScene(new Scene(root));

		//{
		//	root.setStyle("-fx-alignment: center");
		//}
		
		// creates table data (an observable list of objects)
		ObservableList<Grocery> tableData = FXCollections.observableArrayList();

		// defines table columns
		TableColumn<Grocery, String> idColumn = new TableColumn<Grocery, String>("Id");
		TableColumn<Grocery, String> itemNameColumn = new TableColumn<Grocery, String>("Item");
		TableColumn<Grocery, Integer> quantityColumn = new TableColumn<Grocery, Integer>("QTY");
		TableColumn<Grocery, String> sectionColumn = new TableColumn<Grocery, String>("Section");
		TableColumn<Grocery, String> daysAgoColumn = new TableColumn<Grocery, String>("Bought");
		
		
		// for each column defined, calls their setCellValueFactory method using an instance of PropertyValueFactory
		idColumn.setCellValueFactory(new PropertyValueFactory<>("Id"));
		itemNameColumn.setCellValueFactory(new PropertyValueFactory<>("ItemName"));
		quantityColumn.setCellValueFactory(new PropertyValueFactory<>("Quantity"));
		sectionColumn.setCellValueFactory(new PropertyValueFactory<>("Section"));
		daysAgoColumn.setCellValueFactory(new PropertyValueFactory<>("DaysAgo"));

		// creates the table view and add table columns to it
		TableView<Grocery> tableView = new TableView<Grocery>();

		// adds table columns to the table view create above
		tableView.getColumns().add(idColumn);
		tableView.getColumns().add(itemNameColumn);
		tableView.getColumns().add(quantityColumn);
		tableView.getColumns().add(sectionColumn);
		tableView.getColumns().add(daysAgoColumn);
		
		//	attachs table data to the table view
		tableView.setItems(tableData);


		
		// set minimum and maximum width to the table view and each columns
		idColumn.setMinWidth(ID_COL_MIN_WIDTH);
		itemNameColumn.setMinWidth(ITEM_COL_MIN_WIDTH);
		quantityColumn.setMinWidth(QUANTITY_COL_MIN_WIDTH);
		sectionColumn.setMinWidth(SECTION_COL_MIN_WIDTH);
		daysAgoColumn.setMinWidth(BOUGHT_COL_MIN_WIDTH);
		tableView.setMinHeight(TABLE_VIEW_MIN_HEIGHT);
				
		tableData.clear();
		tableData.addAll(fridgeDSC.getAllGroceries());
		
		// filter container 
		TextField filterTF = new TextField();
		Label filterLB = new Label("Filter By: ");
		
		ChoiceBox<FILTER_COLUMNS> filterCB = new ChoiceBox<>();
		filterCB.getItems().addAll(FILTER_COLUMNS.values());
		filterCB.setValue(FILTER_COLUMNS.ITEM);
		
		CheckBox cb = new CheckBox("Show Expiry Only");
		cb.setDisable(true);
		
		HBox filterHBox = new HBox(filterTF, filterLB, filterCB, cb);
		filterHBox.setStyle("-fx-spacing: 5");

		// choiceBox filters display by input bought days ago
		filterCB.setOnAction((e) ->
		{
			Enum selected = filterCB.getValue();

			if (selected.equals(FILTER_COLUMNS.BOUGHT_DAYS_AGO))
			{
				cb.setDisable(false);
			}
			else
			{
               cb.setSelected(false);
               cb.setDisable(true);
            }
            filterTF.clear();
            filterTF.requestFocus();
        });

		// checkBox filters display to show groceries with an expiry
		cb.setOnAction((e) ->
		{
            filterTF.clear();
            filterTF.requestFocus();

            try
            {
				tableData.clear();
				if (cb.isSelected())
				{
					List<Grocery> groceries = new ArrayList<Grocery>();
					groceries = fridgeDSC.getAllGroceries();

					for (Grocery g: groceries)
					{
						Item item = g.getItem();
						if (item.canExpire())
						{
							tableData.add(g);
						}
					}
				}
				else
				{
					tableData.addAll(fridgeDSC.getAllGroceries());
				}
            }
            catch (Exception exception)
            {
				System.out.println(exception.getMessage());
            }
        });

		// creates a filtered list
		FilteredList<Grocery> filteredList = new FilteredList<>(tableData, p -> true);

		// creates a sorted list from the filtered list
		SortedList<Grocery> sortedList = new SortedList<>(filteredList);
		
		// binds comparators of sorted list with that of table view
		sortedList.comparatorProperty().bind(tableView.comparatorProperty());
		
		// sets items of table view to be sorted list
		tableView.setItems(sortedList);

		// sets a change listener to text field to set the filter predicate of filtered list
		filterTF.textProperty().addListener((observable, oldValue, newValue) ->
        {
			filteredList.setPredicate(grocery ->
			{
				if (newValue == null || newValue.isEmpty())
				{
					return true;
				}

				Enum selected = filterCB.getValue();
				String filterString = newValue.toUpperCase();

				if (selected.equals(FILTER_COLUMNS.ITEM))
				{
					return grocery.getItemName().toUpperCase().contains(filterString);
				}
				else if (selected.equals(FILTER_COLUMNS.SECTION))
				{
					return grocery.getSection().toString().toUpperCase().contains(filterString);
				}
				else if (selected.equals(FILTER_COLUMNS.BOUGHT_DAYS_AGO) && filterString.matches("[0-9]+") && cb.isSelected() && grocery.getItem().canExpire())
				{
					return grocery.getDaysAgo().toUpperCase().contains(filterString);
				}
				else if (selected.equals(FILTER_COLUMNS.BOUGHT_DAYS_AGO) && filterString.matches("[0-9]+") && !cb.isSelected())
				{
					return grocery.getDaysAgo().toUpperCase().contains(filterString);
				}
				else
				{
					return false;
				}
			});
		});


		// ACTION buttons: ADD, UPDATE ONE, DELETE
		Button addBT = new Button("ADD");
		Button updateBT = new Button("UPDATE ONE");
		Button deleteBT = new Button("DELETE");

		HBox basicHBox = new HBox(addBT, updateBT, deleteBT);

		
		// Item will list item data from the data source controller list all items method
		ComboBox<Item> itemCB = new ComboBox<Item>();
		itemCB.getItems().addAll(fridgeDSC.getAllItems());
		itemCB.setVisibleRowCount(4);
		
		Label itemLB = new Label("Item");
		itemLB.setGraphic(itemCB);
		itemLB.setStyle("-fx-content-display: BOTTOM");

		// Section will list all sections defined in the data source controller SECTION enum
		ChoiceBox<FridgeDSC.SECTION> sectionCB = new ChoiceBox<>();
		sectionCB.getItems().setAll(FridgeDSC.SECTION.values());
		
		Label sectionLB = new Label("Section");
		sectionLB.setGraphic(sectionCB);
		sectionLB.setStyle("-fx-content-display: BOTTOM");

		// Quantity: a texf field, self descriptive
		TextField quantityTF = new TextField();

		Label quantityLB = new Label("Quantity");
		quantityLB.setGraphic(quantityTF);
		quantityLB.setStyle("-fx-content-display: BOTTOM");

		itemCB.setMinWidth(450);
		itemCB.setMaxWidth(450);
		sectionCB.setMinWidth(150);
		sectionCB.setMaxWidth(150);
		quantityTF.setMaxWidth(100);

		HBox addItemHBox = new HBox(itemLB, itemCB, sectionLB, sectionCB, quantityLB, quantityTF);
		addItemHBox.setStyle("-fx-alignment: center; -fx-spacing: 5");

		// CANCEL button: clears all input controls
		// SAVE button
		Button clearBT = new Button("CLEAR");
		Button saveBT = new Button("SAVE");

		HBox clearSaveHBox = new HBox(clearBT, saveBT);
		clearSaveHBox.setStyle("-fx-alignment: center");
	  
	    // ADD button sets the add UI elements to visible;
		addBT.setOnAction(e ->
		{
			try
            {
				root.getChildren().addAll(addItemHBox, clearSaveHBox);
            }
            catch (Exception exception)
            {
				System.out.println(exception.getMessage());
				Alert alert = new Alert(Alert.AlertType.ERROR);
				
				alert.setTitle("Error Dialog");
				alert.setHeaderText("An error occured:");
				alert.setContentText(exception.toString() + "[Error] Fields are already on the stage");
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

				alert.showAndWait();
            }
        });

		// UPDATE ONE button action check to see if a table view row has been selected first before doing their
		// action; uses a Alert confirmation
		updateBT.setOnAction(e ->
        {
			if (tableView.getSelectionModel().getSelectedItem() == null)
            {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				
				alert.setTitle("Error Dialog");
				alert.setHeaderText("An error occured:");
				alert.setContentText("[Error] A grocery needs to be selected for this action to work");
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

				alert.showAndWait();
            }

            Grocery g = tableView.getSelectionModel().getSelectedItem();
            int id = g.getId();

            try
            {
				fridgeDSC.useGrocery(id);
				tableData.clear();
				tableData.addAll(fridgeDSC.getAllGroceries());
            }
            catch (Exception exception)
            {
				System.out.println(exception.getMessage());
				Alert alert = new Alert(Alert.AlertType.ERROR);

				alert.setTitle("Error Dialog");
				alert.setHeaderText("An error occured:");
				alert.setContentText(exception.toString() + "[ERROR] Use DELETE instead.");
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

				alert.showAndWait();
            }
        });
		
		// DELETE button action check to see if a table view row has been selected first before doing their
		// action; uses an Alert confirmation
		deleteBT.setOnAction(e ->
        {
			Grocery g = tableView.getSelectionModel().getSelectedItem();

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			
			alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("Are you sure you want to delete the following Grocery?");
            alert.setContentText("Delete ID: " + g.getId() + ", Name: " + g.getItemName() +"?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK)
            {
				try
				{
					fridgeDSC.removeGrocery(g.getId());
					tableData.clear();
					tableData.addAll(fridgeDSC.getAllGroceries());
				}
				catch (Exception exception)
				{
					System.out.println(exception.getMessage());
				}
            }
        });

		// SAVE button: sends the new grocery information to the data source controller
		saveBT.setOnAction(e ->
        {
			if (itemCB.getValue() == null || quantityTF.getText().isEmpty() || sectionCB.getValue() == null)
            {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				
				alert.setTitle("Error Dialog");
				alert.setHeaderText("An error occured:");
				alert.setContentText("[ERROR] Please complete filling all the fields below to SAVE your grocery.");
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

				alert.showAndWait();
				return;
            }

            Item item = itemCB.getValue();
            String itemName = item.getName();

            String quantityString = quantityTF.getText();

            if (!quantityString.matches("[0-9]+"))
            {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				
				alert.setTitle("Error Dialog");
				alert.setHeaderText("An error occured:");
				alert.setContentText("[ERROR] Quantity field must be a number.");
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

				alert.showAndWait();
				quantityTF.requestFocus();
				quantityTF.clear();
				
				return;
            }

            int quantity = Integer.parseInt(quantityString);

            if (quantity < 1)
            {
				Alert alert = new Alert(Alert.AlertType.ERROR);

				alert.setTitle("Error Dialog");
				alert.setHeaderText("An error occured");
				alert.setContentText("[ERROR] Quantity must have a minimum value of 1.");
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

				alert.showAndWait();
				return;
            }

            FridgeDSC.SECTION section = sectionCB.getValue();
			
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			
			alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("Are you sure you want to add this item?");
            alert.setContentText("");
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK)
            {
				try
				{
					fridgeDSC.addGrocery(itemName, quantity, section);
					tableData.clear();
					tableData.addAll(fridgeDSC.getAllGroceries());
				}
				catch (Exception exception)
				{
					System.out.println(exception.getMessage());
				}

				itemCB.setValue(null);
				sectionCB.setValue(null);
				quantityTF.clear();
				root.getChildren().removeAll(addItemHBox, clearSaveHBox);
            }
        });

		// CANCEL button: clears all input controls
		clearBT.setOnAction(e ->
        {
			itemCB.setValue(null);
            sectionCB.setValue(null);
            quantityTF.clear();
            root.getChildren().removeAll(addItemHBox, clearSaveHBox);
        });

			
		root.getChildren().addAll(filterHBox, tableView, basicHBox);

		root.setStyle(
			"-fx-font-size: 20;" +
			"-fx-alignment: center;"
		);

		Scene scene = new Scene(root);
		stage.setScene(scene);
	}

	public void stop() throws Exception 
	{
		try
        {
           fridgeDSC.disconnect();
        }
        catch (Exception exception)
        {
           System.out.println(exception.getMessage());
        }
	}	
}
