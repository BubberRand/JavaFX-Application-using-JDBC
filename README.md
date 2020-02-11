# JavaFX-Application-using-JDBC
This is an assignment I completed for university with the goal to create an application to maintain a collection of groceries stored in 
your fridge

The groceries and a list of items are stored in a MySQL database.
The system consists of the following main classes:

- **Grocery** represents a grocery item bought and stored in your fridge
- **Item** has a one to one mapping with the Grocery class where Grocery class has one (and only one) instance of Item
- **FridgeDSC** is the data source controller
- **FridgeFX** is the graphical user interface for the users to interact with the system

As well an sql script to create the tables for the mySQL database.

The application displays the groceries in the form of a list, with functionality to add, delete and update the groceries. In addition to
the feature to filter the displayed groceries by different attributes and show only items with expiry.

