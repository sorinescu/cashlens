/**
 * 
 */
package com.udesign.cashlens;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * @author sorin
 * 
 */
public final class CashLensStorage
{
	protected Context mContext;
	protected File mImageStorageDir;
	protected File mSoundStorageDir;
	protected DBHelper mHelper;
	private Random mRandom = new Random();

	private static CashLensStorage mInstance;

	private static final String DATABASE_NAME = "cashlens.db";
	private static final int DATABASE_VERSION = 1;

	private ArrayListWithNotify<Account> mAccounts;
	private ArrayListWithNotify<Currency> mCurrencies;

	/**
	 * Account data.
	 */
	public static class Account extends ArrayAdapterIDAndName.IDAndName
	{
	}

	/**
	 * Accounts DB table structure.
	 */
	private static class AccountsTable implements BaseColumns
	{
		public static final String TABLE_NAME = "accounts";

		// This class cannot be instantiated
		private AccountsTable()
		{
		}

		/**
		 * The name of the account.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";

		public static void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + AccountsTable._ID
					+ " INTEGER PRIMARY KEY," + AccountsTable.NAME + " TEXT"
					+ ");");

			// TODO use dynamic data
			ContentValues accountTest1 = new ContentValues();
			accountTest1.put(NAME, "Cashhh");
			db.insert(TABLE_NAME, null, accountTest1);
			ContentValues accountTest2 = new ContentValues();
			accountTest2.put(NAME, "Visa");
			db.insert(TABLE_NAME, null, accountTest2);
		}

		public static void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion)
		{
			Log.w(AccountsTable.class.getName(),
					"Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + AccountsTable.TABLE_NAME);
			onCreate(db);
		}
	}

	/**
	 * Currency data.
	 */
	public static class Currency extends ArrayAdapterIDAndName.IDAndName
	{
	}

	/**
	 * Currencies DB table structure.
	 */
	private static class CurrenciesTable implements BaseColumns
	{
		public static final String TABLE_NAME = "currencies";

		// This class cannot be instantiated
		private CurrenciesTable()
		{
		}

		/**
		 * The name of the currency.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";

		public static void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
					+ CurrenciesTable._ID + " INTEGER PRIMARY KEY,"
					+ CurrenciesTable.NAME + " TEXT" + ");");

			// TODO use dynamic data
			ContentValues currTest1 = new ContentValues();
			currTest1.put(NAME, "USD");
			db.insert(TABLE_NAME, null, currTest1);
			ContentValues currTest2 = new ContentValues();
			currTest2.put(NAME, "EUR");
			db.insert(TABLE_NAME, null, currTest2);
		}

		public static void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion)
		{
			Log.w(AccountsTable.class.getName(),
					"Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + CurrenciesTable.TABLE_NAME);
			onCreate(db);
		}
	}

	/**
	 * Expenses DB table structure.
	 */
	private static class ExpensesTable implements BaseColumns
	{
		public static final String TABLE_NAME = "expenses";

		// This class cannot be instantiated
		private ExpensesTable()
		{
		}

		/**
		 * The date of the expense (GMT).
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String DATE = "date";

		/**
		 * The account to which the expense was debited.
		 * <P>
		 * Type: INTEGER FOREIGN KEY
		 * </P>
		 */
		public static final String ACCOUNT = "account";

		/**
		 * The amount of the expense.
		 * <P>
		 * Type: INTEGER (2 decimal fixed point)
		 * </P>
		 */
		public static final String AMOUNT = "amount";

		/**
		 * The currency of the expense.
		 * <P>
		 * Type: INTEGER FOREIGN KEY
		 * </P>
		 */
		public static final String CURRENCY = "currency";

		/**
		 * The text description of the expense (optional).
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String DESCRIPTION = "description";

		/**
		 * The file name of the image of the expense (optional).
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String IMAGE_FILENAME = "image_fname";

		/**
		 * The file name of the audio of the expense (optional).
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String AUDIO_FILENAME = "audio_fname";

		public static void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + ExpensesTable.TABLE_NAME + " ("
					+ ExpensesTable._ID + " INTEGER PRIMARY KEY,"
					+ ExpensesTable.ACCOUNT + " INTEGER,"
					+ ExpensesTable.CURRENCY + " INTEGER,"
					+ ExpensesTable.AMOUNT + " INTEGER," + ExpensesTable.DATE
					+ " TEXT NOT NULL," + ExpensesTable.DESCRIPTION + " TEXT,"
					+ ExpensesTable.IMAGE_FILENAME + " TEXT,"
					+ ExpensesTable.AUDIO_FILENAME + " TEXT," + "FOREIGN KEY ("
					+ ExpensesTable.ACCOUNT + ") REFERENCES "
					+ AccountsTable.TABLE_NAME + "(" + AccountsTable._ID + "),"
					+ "FOREIGN KEY (" + ExpensesTable.CURRENCY
					+ ") REFERENCES " + CurrenciesTable.TABLE_NAME + "("
					+ CurrenciesTable._ID + ")" + ");");
		}

		public static void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion)
		{
			Log.w(ExpensesTable.class.getName(),
					"Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + ExpensesTable.TABLE_NAME);
			onCreate(db);
		}
	}

	private static class DBHelper extends SQLiteOpenHelper
	{
		DBHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			AccountsTable.onCreate(db);
			CurrenciesTable.onCreate(db);
			ExpensesTable.onCreate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			AccountsTable.onUpgrade(db, oldVersion, newVersion);
			CurrenciesTable.onUpgrade(db, oldVersion, newVersion);
			ExpensesTable.onUpgrade(db, oldVersion, newVersion);
		}
	}

	public static CashLensStorage instance(Context currentContext)
	throws IOException
	{
		// use the global application context for the singleton, not the
		// received context
		// which may become unavailable
		if (mInstance == null)
			mInstance = new CashLensStorage(currentContext
					.getApplicationContext());

		return mInstance;
	}

	private CashLensStorage(Context context) throws IOException
	{
		mContext = context;

		// Protect app storage directory from indexing by the Android
		// content manager
		File noMedia = new File(storageDirectory(DataType.ROOT), ".nomedia");
		if (!noMedia.exists() && !noMedia.createNewFile())
			throw new IOException(mContext
					.getString(R.string.cant_create_file)
					+ noMedia.getAbsolutePath());

		// Create/open database
		mHelper = new DBHelper(context);

		// Create data adapters
		mAccounts = new ArrayListWithNotify<Account>();
		readAccounts();

		mCurrencies = new ArrayListWithNotify<Currency>();
		readCurrencies();
	}

	public void close()
	{
		if (mHelper != null)
			mHelper.close();
	}
	
	protected SQLiteDatabase db()
	{
		return mHelper.getWritableDatabase();
	}

	protected boolean available(boolean read)
	{
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state))
			return true; // read and write
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
			return read; // read-only

		return false; // not mounted
	}

	public boolean availableForRead()
	{
		return available(true);
	}

	public boolean availableForWrite()
	{
		return available(false);
	}

	protected String randomFileName()
	{
		String name = "";

		name += Integer.toHexString(mRandom.nextInt());
		name += Integer.toHexString(mRandom.nextInt());

		return name;
	}

	protected File randomFile(File dir, String extension) throws IOException
	{
		File newFile = null;

		if (!dir.canWrite())
			throw new IOException(mContext.getString(R.string.cant_write_dir)
					+ dir.getAbsolutePath());

		// try random names until we find one that's not taken
		do
		{
			newFile = new File(dir, randomFileName() + extension);
			if (!newFile.createNewFile())
				newFile = null;
		} while (newFile == null);

		return newFile;
	}

	protected enum DataType
	{
		IMAGE, SOUND, ROOT
	}

	protected File storageDirectory(DataType dataType) throws IOException
	{
		if (dataType == DataType.IMAGE && mImageStorageDir != null)
			return mImageStorageDir;
		else if (dataType == DataType.SOUND && mSoundStorageDir != null)
			return mSoundStorageDir;

		String dir = Environment.getExternalStorageDirectory()
		.getAbsolutePath();
		dir += "/Android/data/" + mContext.getPackageName() + "/files/";
		Log.i("ExpenseStorage", "App storage dir is '" + dir + "'");

		switch (dataType)
		{
		case IMAGE:
			mImageStorageDir = new File(dir + "images/");
			if (!mImageStorageDir.exists() && !mImageStorageDir.mkdirs())
				throw new IOException(mContext
						.getString(R.string.cant_create_dir)
						+ mImageStorageDir.getAbsolutePath());
			return mImageStorageDir;
		case SOUND:
			mSoundStorageDir = new File(dir + "audio");
			if (!mSoundStorageDir.exists() && !mSoundStorageDir.mkdirs())
				throw new IOException(mContext
						.getString(R.string.cant_create_dir)
						+ mSoundStorageDir.getAbsolutePath());
			return mSoundStorageDir;
		default:
			File rootDir = new File(dir);
			if (!rootDir.exists() && !rootDir.mkdirs())
				throw new IOException(mContext
						.getString(R.string.cant_create_dir)
						+ rootDir.getAbsolutePath());
			return rootDir;
		}
	}

	protected void readAccounts()
	{
		Cursor cursor = db().query(AccountsTable.TABLE_NAME, null, null, null,
				null, null, AccountsTable.NAME);

		mAccounts.clear();

		int idIndex = cursor.getColumnIndex(AccountsTable._ID);
		int nameIndex = cursor.getColumnIndex(AccountsTable.NAME);

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			Account account = new Account();

			account.id = cursor.getInt(idIndex);
			account.name = cursor.getString(nameIndex);

			mAccounts.add(account);

			cursor.moveToNext();
		}

		cursor.close();

		// auto notification is turned off; manually notify of changed data
		mAccounts.notifyDataChanged();
	}

	public ArrayAdapterIDAndName<Account> accountsAdapter(Context context)
	{
		return new ArrayAdapterIDAndName<Account>(context, mAccounts);
	}

	protected void readCurrencies()
	{
		Cursor cursor = db().query(CurrenciesTable.TABLE_NAME, null, null, null,
				null, null, CurrenciesTable.NAME);

		mCurrencies.clear();

		int idIndex = cursor.getColumnIndex(AccountsTable._ID);
		int nameIndex = cursor.getColumnIndex(AccountsTable.NAME);

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			Currency currency = new Currency();

			currency.id = cursor.getInt(idIndex);
			currency.name = cursor.getString(nameIndex);

			mCurrencies.add(currency);

			cursor.moveToNext();
		}

		cursor.close();

		// auto notification is turned off; manually notify of changed data
		mCurrencies.notifyDataChanged();
	}

	public ArrayAdapterIDAndName<Currency> currenciesAdapter(Context context)
	{
		return new ArrayAdapterIDAndName<Currency>(context, mCurrencies);
	}

	/**
	 * Saves an expense to the database.
	 * 
	 * @param accountID
	 *            Database ID of account.
	 * @param currencyID
	 *            Database ID of currency.
	 * @param amount
	 *            Fixed point (2 decimals) of expense amount.
	 * @param date
	 *            Date of expense.
	 * @param description
	 *            Optional text description of expense.
	 * @param imageFilePath
	 *            Optional JPEG file path for expense.
	 * @param audioFilePath
	 *            Optional audio file path for expense.
	 */
	protected void saveExpenseToDB(Account account, Currency currency,
			int amount, Date date, String description, String imageFilePath,
			String audioFilePath)
	{
		ContentValues values = new ContentValues();
		values.put(ExpensesTable.ACCOUNT, account.id);
		values.put(ExpensesTable.CURRENCY, currency.id);
		values.put(ExpensesTable.AMOUNT, amount);
		values.put(ExpensesTable.DATE, date.toGMTString());
		values.put(ExpensesTable.DESCRIPTION, description);
		values.put(ExpensesTable.IMAGE_FILENAME, imageFilePath);
		values.put(ExpensesTable.AUDIO_FILENAME, audioFilePath);

		db().insert(ExpensesTable.TABLE_NAME, null, values);

		// TODO notify potential users of expense data that a new one is available
	}

	public void saveExpense(Account account, Currency currency, int amount,
			Date date, byte[] jpegData) throws IOException
			{
		File imageFile = randomFile(storageDirectory(DataType.IMAGE), ".jpeg");
		FileOutputStream image = new FileOutputStream(imageFile);

		// Write image data
		image.write(jpegData);
		image.flush();
		image.close();

		// Save expense to database
		saveExpenseToDB(account, currency, amount, date, null, imageFile
				.getAbsolutePath(), null);
			}

	public void addAccount(Account account) throws IOException
	{
		ContentValues values = new ContentValues();
		values.put(AccountsTable.NAME, account.name);

		long id = (int)db().insert(AccountsTable.TABLE_NAME, null, values);

		if (id < 0)
			throw new IOException(mContext.getString(R.string.cant_insert));

		account.id = (int)id;

		Log.w("addAccount", "New account: " + account.name + ", ID " + Integer.toString(account.id));

		// Also add to accounts list
		mAccounts.add(account);
		mAccounts.notifyDataChanged();
	}

	public void deleteAccount(Account account) throws IOException
	{
		ContentValues values = new ContentValues();
		values.put(AccountsTable.NAME, account.name);

		int affected = db().delete(AccountsTable.TABLE_NAME, AccountsTable._ID + "=?", new String[] { Integer.toString(account.id) });

		if (affected != 1)
			throw new IOException("Deleted " + Integer.toString(affected) + " items instead of 1 from " + AccountsTable.TABLE_NAME);

		Log.w("addAccount", "Deleted account: " + account.name + ", ID " + Integer.toString(account.id));

		// Also remove from accounts list
		mAccounts.remove(account);
		mAccounts.notifyDataChanged();
	}
}
