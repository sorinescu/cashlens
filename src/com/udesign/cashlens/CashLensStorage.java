/**
 * 
 */
package com.udesign.cashlens;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;

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
	private static final int DATABASE_VERSION = 3;

	private ArrayListWithNotify<Account> mAccounts;
	private ArrayListWithNotify<Currency> mCurrencies;
	private ArrayListWithNotify<Expense> mExpenses;

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
		}

		public static void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion)
		{
			// TODO this is not acceptable; upgrade DB
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
			// TODO this is not acceptable; upgrade DB
			Log.w(AccountsTable.class.getName(),
					"Upgrading database from version " + oldVersion + " to "
							+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + CurrenciesTable.TABLE_NAME);
			onCreate(db);
		}
	}

	/**
	 * Expense data.
	 */
	public static class Expense
	{
		private CashLensStorage mStorage;
		
		int id;
		Date date;
		int accountId;
		int amount;	// fixed point
		int currencyId;
		int thumbnailId;
		String description;
		String imagePath;
		String audioPath;
		
		public Expense(CashLensStorage storage)
		{
			super();
			mStorage = storage;
		}
		
		public String amountToString()
		{
			return Integer.toString(amount / 100) + "." + 
				Integer.toString(amount % 100);
		}
		
		public String currencyName()
		{
			Currency currency = mStorage.getCurrency(currencyId);
			if (currency != null)
				return currency.name;
			
			return "???";
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
		 * The date of the expense (UTC).
		 * <P>
		 * Type: INTEGER
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
		 * The id of the image thumbnail (optional).
		 * <P>
		 * Type: INTEGER FOREIGN KEY
		 * </P>
		 */
		public static final String IMAGE_THUMBNAIL = "image_thumb";

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
					+ ExpensesTable.AMOUNT + " INTEGER," 
					+ ExpensesTable.DATE + " INTEGER NOT NULL," 
					+ ExpensesTable.DESCRIPTION + " TEXT,"
					+ ExpensesTable.IMAGE_FILENAME + " TEXT,"
					+ ExpensesTable.IMAGE_THUMBNAIL + " INTEGER,"
					+ ExpensesTable.AUDIO_FILENAME + " TEXT," 
					+ "FOREIGN KEY ("
						+ ExpensesTable.ACCOUNT + ") REFERENCES "
						+ AccountsTable.TABLE_NAME + "(" + AccountsTable._ID + "),"
					+ "FOREIGN KEY (" 
						+ ExpensesTable.CURRENCY + ") REFERENCES " 
						+ CurrenciesTable.TABLE_NAME + "(" + CurrenciesTable._ID + ")" 
					+ "FOREIGN KEY (" 
						+ ExpensesTable.IMAGE_THUMBNAIL + ") REFERENCES " 
						+ ExpenseThumbnailsTable.TABLE_NAME + "(" + ExpenseThumbnailsTable._ID + ")" 
					+ ");");
		}

		public static void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion)
		{
			// TODO this is not acceptable; upgrade DB
			Log.w(ExpensesTable.class.getName(),
					"Upgrading database from version " + oldVersion + " to "
							+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + ExpensesTable.TABLE_NAME);
			onCreate(db);
		}
	}

	/**
	 * Expense thumbnail DB table structure.
	 */
	private static class ExpenseThumbnailsTable implements BaseColumns
	{
		public static final String TABLE_NAME = "expense_thumbs";

		// This class cannot be instantiated
		private ExpenseThumbnailsTable()
		{
		}

		/**
		 * The compressed (JPEG) thumbnail data.
		 * <P>
		 * Type: BLOB
		 * </P>
		 */
		public static final String DATA = "data";

		public static void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" 
					+ ExpenseThumbnailsTable._ID + " INTEGER PRIMARY KEY," 
					+ ExpenseThumbnailsTable.DATA + " BLOB"
					+ ");");
		}

		public static void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion)
		{
			// TODO this is not acceptable; upgrade DB
			Log.w(ExpenseThumbnailsTable.class.getName(),
					"Upgrading database from version " + oldVersion + " to "
							+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + ExpenseThumbnailsTable.TABLE_NAME);
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
			ExpenseThumbnailsTable.onCreate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			AccountsTable.onUpgrade(db, oldVersion, newVersion);
			CurrenciesTable.onUpgrade(db, oldVersion, newVersion);
			ExpensesTable.onUpgrade(db, oldVersion, newVersion);
			ExpenseThumbnailsTable.onUpgrade(db, oldVersion, newVersion);
		}
	}

	public static CashLensStorage instance(Context currentContext)
			throws IOException
	{
		// use the global application context for the singleton, not the
		// received context, which may become unavailable
		if (mInstance == null)
			mInstance = new CashLensStorage(currentContext
					.getApplicationContext());

		return mInstance;
	}

	public Currency getCurrency(int currencyId)
	{
		for (Currency currency : mCurrencies)
			if (currency.id == currencyId)
				return currency;
		
		return null;
	}

	public Expense getExpense(int expenseId)
	{
		// TODO this function is limited to the expenses we 
		// have read through the last readExpenses call; fix this ?
		for (Expense expense : mExpenses)
			if (expense.id == expenseId)
				return expense;
		
		return null;
	}

	private CashLensStorage(Context context) throws IOException
	{
		mContext = context;

		// Protect app storage directory from indexing by the Android
		// content manager
		File noMedia = new File(storageDirectory(DataType.ROOT), ".nomedia");
		if (!noMedia.exists() && !noMedia.createNewFile())
			throw new IOException(mContext.getString(R.string.cant_create_file)
					+ noMedia.getAbsolutePath());

		// Create/open database
		mHelper = new DBHelper(context);

		// Create data adapters
		mAccounts = new ArrayListWithNotify<Account>();
		readAccounts();

		mCurrencies = new ArrayListWithNotify<Currency>();
		readCurrencies();
		
		mExpenses = new ArrayListWithNotify<Expense>();
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
	
	protected static long dateToUTCInt(Date date)
	{
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.setTime(date);
		return cal.getTime().getTime();
	}

	protected static Date dateFromUTCInt(long utcTime)
	{
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(utcTime);
		return cal.getTime();
	}

	protected enum DataType
	{
		IMAGE, SOUND, ROOT
	}

	public String storageDirectoryPath(DataType dataType)
	{
		String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
		
		dir += "/Android/data/" + mContext.getPackageName() + "/files/";
		Log.i("ExpenseStorage", "App storage dir is '" + dir + "'");

		switch (dataType)
		{
		case IMAGE:
			return dir + "images/";
		case SOUND:
			return dir + "audio/";
		default:
			return dir;
		}
	}
	
	protected File storageDirectory(DataType dataType) throws IOException
	{
		if (dataType == DataType.IMAGE && mImageStorageDir != null)
			return mImageStorageDir;
		else if (dataType == DataType.SOUND && mSoundStorageDir != null)
			return mSoundStorageDir;

		String path = storageDirectoryPath(dataType);
		
		switch (dataType)
		{
		case IMAGE:
			mImageStorageDir = new File(path);
			if (!mImageStorageDir.exists() && !mImageStorageDir.mkdirs())
				throw new IOException(mContext
						.getString(R.string.cant_create_dir)
						+ mImageStorageDir.getAbsolutePath());
			return mImageStorageDir;
		case SOUND:
			mSoundStorageDir = new File(path);
			if (!mSoundStorageDir.exists() && !mSoundStorageDir.mkdirs())
				throw new IOException(mContext
						.getString(R.string.cant_create_dir)
						+ mSoundStorageDir.getAbsolutePath());
			return mSoundStorageDir;
		default:
			File rootDir = new File(path);
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
		Cursor cursor = db().query(CurrenciesTable.TABLE_NAME, null, null,
				null, null, null, CurrenciesTable.NAME);

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
	 * @throws IOException 
	 */
	protected void saveExpenseToDB(Expense expense) throws IOException
	{
		ContentValues values = new ContentValues();
		values.put(ExpensesTable.ACCOUNT, expense.accountId);
		values.put(ExpensesTable.CURRENCY, expense.currencyId);
		values.put(ExpensesTable.AMOUNT, expense.amount);
		values.put(ExpensesTable.DATE, dateToUTCInt(expense.date));
		values.put(ExpensesTable.DESCRIPTION, expense.description);
		values.put(ExpensesTable.IMAGE_FILENAME, expense.imagePath);
		values.put(ExpensesTable.IMAGE_THUMBNAIL, expense.thumbnailId);
		values.put(ExpensesTable.AUDIO_FILENAME, expense.audioPath);

		long id = db().insert(ExpensesTable.TABLE_NAME, null, values);
		if (id < 0)
			throw new IOException("Could not insert expense into database");
		
		expense.id = (int)id;
		
		// TODO notify potential users of expense data that a new one is
		// available
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
		
		// Generate and save thumbnail
		byte[] thumbData = ExpenseThumbnail.createFromJPEG(mContext, jpegData);
		int thumbId = saveExpenseThumbnail(thumbData);
		
		// Save expense to database
		Expense expense = new Expense(this);
		expense.accountId = account.id;
		expense.currencyId = currency.id;
		expense.amount = amount;
		expense.date = date;
		expense.audioPath = null;
		expense.description = null;
		expense.imagePath = imageFile.getAbsolutePath();
		expense.thumbnailId = thumbId;
		
		saveExpenseToDB(expense);
	}

	public ArrayListWithNotify<Expense> readExpenses(Date startDate, Date endDate, int[] accountIds)
	{
		// cache the retrieved expenses locally
		mExpenses = new ArrayListWithNotify<Expense>();
		
		String cond = "";
		
		if (startDate != null)
			cond = ExpensesTable.DATE + ">=" + dateToUTCInt(startDate);
		
		if (endDate != null)
		{
			if (cond.length() > 0)
				cond += " AND ";
			cond += ExpensesTable.DATE + "<" + dateToUTCInt(endDate);
		}
		
		if (accountIds != null)
		{
			if (cond.length() > 0)
				cond += " AND ";
			
			cond += "(";
			for (int i=0; i<accountIds.length; ++i)
			{
				cond += ExpensesTable.ACCOUNT + "=" + Integer.toString(accountIds[i]);
				if (i+1 < accountIds.length)
					cond += " OR ";
			}
			cond += ")";
		}
		
		Cursor cursor = db().query(ExpensesTable.TABLE_NAME, null, cond,
				null, null, null, ExpensesTable.DATE);

		int idIndex = cursor.getColumnIndex(ExpensesTable._ID);
		int accountIdIndex = cursor.getColumnIndex(ExpensesTable.ACCOUNT);
		int currencyIdIndex = cursor.getColumnIndex(ExpensesTable.CURRENCY);
		int amountIndex = cursor.getColumnIndex(ExpensesTable.AMOUNT);
		int dateIndex = cursor.getColumnIndex(ExpensesTable.DATE);;
		int audioPathIndex = cursor.getColumnIndex(ExpensesTable.AUDIO_FILENAME);
		int descriptionIndex = cursor.getColumnIndex(ExpensesTable.DESCRIPTION);
		int imagePathIndex = cursor.getColumnIndex(ExpensesTable.IMAGE_FILENAME);
		int thumbIndex = cursor.getColumnIndex(ExpensesTable.IMAGE_THUMBNAIL);

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			Expense expense = new Expense(this);

			expense.id = cursor.getInt(idIndex);
			expense.accountId = cursor.getInt(accountIdIndex);
			expense.currencyId = cursor.getInt(currencyIdIndex);
			expense.amount = cursor.getInt(amountIndex);
			expense.date = dateFromUTCInt(cursor.getLong(dateIndex));
			expense.audioPath = cursor.getString(audioPathIndex);
			expense.description = cursor.getString(descriptionIndex);
			expense.imagePath = cursor.getString(imagePathIndex);
			expense.thumbnailId = cursor.getInt(thumbIndex);

			mExpenses.add(expense);

			cursor.moveToNext();
		}

		cursor.close();

		return mExpenses;
	}
	
	public void addAccount(Account account) throws IOException
	{
		ContentValues values = new ContentValues();
		values.put(AccountsTable.NAME, account.name);

		long id = (int) db().insert(AccountsTable.TABLE_NAME, null, values);

		if (id < 0)
			throw new IOException(mContext.getString(R.string.cant_insert));

		account.id = (int) id;

		Log.w("addAccount", "New account: " + account.name + ", ID "
				+ Integer.toString(account.id));

		// Also add to accounts list
		mAccounts.add(account);
		mAccounts.notifyDataChanged();
	}

	public void deleteAccount(Account account) throws IOException
	{
		ContentValues values = new ContentValues();
		values.put(AccountsTable.NAME, account.name);

		int affected = db().delete(AccountsTable.TABLE_NAME,
				AccountsTable._ID + "=" + Integer.toString(account.id), null);

		if (affected != 1)
			throw new IOException("Deleted " + Integer.toString(affected)
					+ " items instead of 1 from " + AccountsTable.TABLE_NAME);

		Log.w("addAccount", "Deleted account: " + account.name + ", ID "
				+ Integer.toString(account.id));

		// Also remove from accounts list
		mAccounts.remove(account);
		mAccounts.notifyDataChanged();
	}
	
	protected int saveExpenseThumbnail(byte[] thumbData) throws IOException
	{
		ContentValues values = new ContentValues();
		values.put(ExpenseThumbnailsTable.DATA, thumbData);

		int id = (int) db().insert(ExpenseThumbnailsTable.TABLE_NAME, null, values);

		if (id < 0)
			throw new IOException(mContext.getString(R.string.cant_insert));

		Log.w("saveExpenseThumbnail", "New thumbnail: ID " + Integer.toString(id));
		
		return id;
	}

	public void loadExpenseThumbnail(ExpenseThumbnail thumb) throws IOException
	{
		Cursor cursor = db().query(ExpenseThumbnailsTable.TABLE_NAME,
				new String[] { ExpenseThumbnailsTable.DATA }, 
				ExpenseThumbnailsTable._ID + "=" + Integer.toString(thumb.id),
				null, null, null, null);

		cursor.moveToFirst();
		
		thumb.decodeFromByteArray(cursor.getBlob(0));
		Log.w("loadExpenseThumbnail", "Loaded thumbnail: ID " + Integer.toString(thumb.id));

		cursor.close();
	}
}
