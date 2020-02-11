public class Item {

	// name is the unique id
	private String name;
	private boolean expires; // defaults to false

	// constructor
	public Item(String name, boolean expires) {
		this.name = name;
		this.expires = expires;
	}

	// constructor
	public Item(String name) {
		this(name, false);
	}

	public String getName() {
		return this.name;
	}

	public boolean canExpire()
	{
		return this.expires;
	}

	public String toString() {
		return "[ name: " + this.name
			+ ", expires: " + this.expires
			+ " ]";
	}

	// To perform some quick tests
	public static void main(String [] args)
	{
		Item i1 = new Item("Milk", false);
		System.out.println(i1);

		Item i2 = new Item("Fish", true);
		System.out.println(i2);
	}
}