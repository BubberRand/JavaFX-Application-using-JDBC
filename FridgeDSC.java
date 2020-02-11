import java.sql.*;
import java.util.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class FridgeDSC {

	// the date format we will be using across the application
	public static final String DATE_FORMAT = "dd/MM/yyyy";

	/*
		FREEZER, // freezing cold
		MEAT, // MEAT cold
		COOLING, // general fridge area
		CRISPER // veg and fruits section

		note: Enums are implicitly public static final
	*/
	public enum SECTION {
		FREEZER,
		MEAT,
		COOLING,
		CRISPER
	};

	private static Connection connection;
	private static Statement statement;
	private static PreparedStatement preparedStatement;
	
	// connection to database
	public static void connect() throws SQLException {
		try 
		{
			Class.forName("com.mysql.jdbc.Driver");
			//strings below should have correct login information, left as blank for sample  
			String url = "jdbc:mysql://";
			String user = "";
			String password = "";

			connection = DriverManager.getConnection(url, user, password);
			statement = connection.createStatement();
  		} 
		catch(Exception e) 
		{
			System.out.println(e);
			e.printStackTrace();
		}		
	}
	
	// disconnection to database
	public static void disconnect() throws SQLException {
		if(preparedStatement != null) preparedStatement.close();
		if(statement != null) statement.close();
		if(connection != null) connection.close();
	}


	// search for item within database by item name
	public Item searchItem(String name) throws Exception {
		String queryString = "SELECT * FROM item WHERE name = ?";
		
		preparedStatement = connection.prepareStatement(queryString);
		preparedStatement.setString(1, name);
		ResultSet rs = preparedStatement.executeQuery();

		Item item = null;

		//checks if item exists
		if (rs.next()) 
		{

			boolean expires = rs.getBoolean(2);
			item = new Item(name, expires);
		}	

		return item;
	}

	// search for grocery within database by ID
	public Grocery searchGrocery(int id) throws Exception {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_FORMAT);
		String queryString = "SELECT * FROM grocery WHERE id = ?";

		preparedStatement = connection.prepareStatement(queryString);
		String diffId = new Integer(id).toString();
		preparedStatement.setString(1, diffId);
		ResultSet rs = preparedStatement.executeQuery();
		
		Grocery grocery = null;

		//checks if grocery exists
		if (rs.next()) 
		{

			LocalDate date = LocalDate.parse(rs.getString(3), dtf);
			int quantity = rs.getInt(4);
			FridgeDSC.SECTION section = SECTION.valueOf(rs.getString(5).toUpperCase());
			
			if(searchItem(rs.getString(2)) != null)
			{
				Item item = searchItem(rs.getString(2));
				grocery = new Grocery(id, item, date, quantity, section);
			}

		}

		return grocery;
	}

	// pulls all items from database
	public List<Item> getAllItems() throws Exception {
		String queryString = "SELECT * FROM item";
		
		ResultSet rs = statement.executeQuery(queryString);
		List<Item> items = new ArrayList<Item>();

		while(rs.next())
		{
			String name = rs.getString(1);
			boolean expires = rs.getBoolean(2);
			items.add(new Item(name, expires));
		}
		
		return items;
	}

	// pulls all groceries from database
	public List<Grocery> getAllGroceries() throws Exception {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_FORMAT);
		String queryString = "SELECT * FROM grocery";
				
		ResultSet rs = statement.executeQuery(queryString);
		
		List<Grocery> groceries = new ArrayList<Grocery>();

		while(rs.next())
		{
			int id = rs.getInt(1);
			Item item = searchItem(rs.getString(2));
			LocalDate date = LocalDate.parse(rs.getString(3), dtf);
			int quantity = rs.getInt(4);
			FridgeDSC.SECTION section = SECTION.valueOf(rs.getString(5).toUpperCase());
					
			groceries.add(new Grocery(id, item, date, quantity, section));
		}

		return groceries;
	}

	// adds a grocery to the database
	public int addGrocery(String name, int quantity, SECTION section) throws Exception {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_FORMAT);
		LocalDate date = LocalDate.now();
		String dateStr = date.format(dtf);
		
		Item precond = searchItem(name);
		
		boolean pre = (precond != null);
		if(!pre)
		{
			String msg = "Item Name: " + name + " does not exist";
            System.out.println("\nERROR " + msg);
            throw new Exception(msg);
		}
		
		//Altered command string as The id attribute of a grocery is of type int and is auto-generated by the database
		//as can be seen in the SQL script
		String command = "INSERT INTO grocery (ItemName, date, quantity, section) VALUES(?, ?, ?, ?)";
		
		preparedStatement = connection.prepareStatement(command);
		
		preparedStatement.setString(1, name);
		preparedStatement.setString(2, dateStr);
		preparedStatement.setInt(3, quantity);
		preparedStatement.setString(4, section.toString());
		preparedStatement.executeUpdate();
	  
		// retrieving & returning last inserted record id
		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		rs.next();
		int newId = rs.getInt(1);

		return newId;		
	}

	public Grocery useGrocery(int id) throws Exception {

		Grocery precond = searchGrocery(id);
		
		// - check if has quantity is greater one; if not throw exception
		//   with adequate error message
		boolean pre = (precond != null);
		if(!pre)
		{
			String msg = "Grocery id: " + id + " does not exist";
			System.out.println("\nERROR: " + msg);
			throw new Exception(msg);
		}
		
		if(precond.getQuantity() <= 1)
		{
			throw new Exception("Quantity cannot be less than 1");
		}
		
		precond.updateQuantity();
		
		String queryString = 
			"UPDATE grocery " +
			"SET quantity = quantity - 1 " +
			"WHERE quantity > 1 " + 
			"AND id = " + id + ";";
		
		statement.executeUpdate(queryString);
		
		return precond;

	}

	// removes grocery from database
	public int removeGrocery(int id) throws Exception {
		String queryString = "DELETE FROM grocery WHERE id = " + id + ";";

		Grocery precond = searchGrocery(id);
						
		// - if grocery does not exist, throw exception with adequate 
		//   error message
		boolean pre = (precond != null);
		if(!pre)
		{
			String msg = "Grocery id: " + id + " does not exist";
            System.out.println("\nERROR " + msg);
            throw new Exception(msg);
		}
		
		// - if grocery exists, statement execute update on queryString
		//   return the value value of that statement execute update
		return statement.executeUpdate(queryString);	

	}

	// STATIC HELPERS -------------------------------------------------------

	public static long calcDaysAgo(LocalDate date) {
    	return Math.abs(Duration.between(LocalDate.now().atStartOfDay(), date.atStartOfDay()).toDays());
	}

	public static String calcDaysAgoStr(LocalDate date) {
    	String formattedDaysAgo;
    	long diff = calcDaysAgo(date);

    	if (diff == 0)
    		formattedDaysAgo = "today";
    	else if (diff == 1)
    		formattedDaysAgo = "yesterday";
    	else formattedDaysAgo = diff + " days ago";	

    	return formattedDaysAgo;			
	}

	// To perform some quick tests	
	public static void main(String[] args) throws Exception {
		FridgeDSC myFridgeDSC = new FridgeDSC();

		myFridgeDSC.connect();

		System.out.println("\nSYSTEM:\n");

		System.out.println("\n\nshowing all of each:");
		System.out.println(myFridgeDSC.getAllItems());
		System.out.println(myFridgeDSC.getAllGroceries());

		//TESTS
		//int addedId = myFridgeDSC.addGrocery("Milk", 40, SECTION.COOLING);
		//System.out.println("added: " + addedId);
		//System.out.println("deleting " + (addedId - 1) + ": " + (myFridgeDSC.removeGrocery(addedId - 1) > 0 ? "DONE" : "FAILED"));
		//System.out.println("using " + (addedId) + ": " + myFridgeDSC.useGrocery(addedId));
		//System.out.println(myFridgeDSC.searchGrocery(addedId));

		myFridgeDSC.disconnect();
	}
}