/*******************************************************************************
 * Copyright 2012 Sorin Otescu <sorin.otescu@gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.udesign.cashlens;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Random;
import java.util.TimeZone;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

public final class CashLensStorage
{
	private Context mContext;
	private File mImageStorageDir;
	private File mSoundStorageDir;
	private DBHelper mHelper;
	private Random mRandom = new Random();

	private static CashLensStorage mInstance;

	private static final String DATABASE_NAME = "cashlens.db";
	private static final int DATABASE_VERSION = 7;

	private ArrayListWithNotify<Account> mAccounts;
	private ArrayListWithNotify<Currency> mCurrencies;
	private ArrayListWithNotify<Expense> mExpenses;
	
	private ExpenseFilterType mExpenseFilterType;
	private ExpenseFilter mCustomExpenseFilter;
	private ExpenseFilter[] mExpenseFilters;	// current expense filters, computed from filter type and custom filter

	public static enum ExpenseFilterType
	{
		NONE, DAY, WEEK, MONTH, CUSTOM		
	}

	/**
	 * Account data.
	 */
	public static class Account extends ArrayAdapterIDAndName.IDAndName
	{
		private CashLensStorage mStorage;
		
		int currencyId;
		int monthStartDay = 1;

		Account(CashLensStorage storage)
		{
			mStorage = storage;
		}
		
		public Currency getCurrency()
		{
			return mStorage.getCurrency(currencyId);
		}
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

		/**
		 * The id of the account currency (from XML).
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String CURRENCY = "currency_id";

		/**
		 * The day the month starts.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String MONTH_START = "month_start";

		public static void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" 
					+ AccountsTable._ID	+ " INTEGER PRIMARY KEY," 
					+ AccountsTable.NAME + " TEXT,"
					+ AccountsTable.CURRENCY + " INTEGER,"
					+ AccountsTable.MONTH_START + " INTEGER"
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
		implements Comparable<Currency>
	{
		String code;

		public int compareTo(Currency another)
		{
			return name.compareTo(another.name);
		}
		
		public String fullName()
		{
			return name + " (" + code + ")";
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
			return amountToString(amount);
		}
		
		public static String amountToString(int amount)
		{
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			
			return Integer.toString(amount / 100) + symbols.getDecimalSeparator() + 
				Integer.toString((amount / 10) % 10) + Integer.toString(amount % 10);
		}
		
		public String currencyCode()
		{
			Account account = mStorage.getAccount(accountId);
			Currency currency = mStorage.getCurrency(account.currencyId);
			if (currency != null)
				return currency.code;
			
			return "???";
		}

		public int currencyId()
		{
			return mStorage.getAccount(accountId).currencyId;
		}

		public String accountName()
		{
			Account account = mStorage.getAccount(accountId);
			if (account != null)
				return account.name;
			
			return "???";
		}

		public void copyFrom(Expense expense)
		{
			id = expense.id;
			accountId = expense.accountId;
			thumbnailId = expense.thumbnailId;
			amount = expense.amount;
			date = (Date)expense.date.clone();
			if (expense.audioPath != null)
				audioPath = new String(expense.audioPath);
			else
				audioPath = null;
			if (expense.imagePath != null)
				imagePath = new String(expense.imagePath);
			else
				imagePath = null;
			if (expense.description != null)
				description = new String(expense.description);
			else
				description = null;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o)
		{
			Expense expense = (Expense)o;
		
			boolean audioPathSame;
			boolean imagePathSame;
			boolean descriptionSame;
			
			// First get rid of possible null values
			if ((audioPath == null || expense.audioPath == null))
				audioPathSame = (audioPath == expense.audioPath);
			else
				audioPathSame = expense.audioPath.equals(audioPath); 

			if ((imagePath == null || expense.imagePath == null))
				imagePathSame = (imagePath == expense.imagePath);
			else
				imagePathSame = expense.imagePath.equals(imagePath);

			if ((description == null || expense.description == null))
				descriptionSame = (description == expense.description);
			else
				descriptionSame = expense.description.equals(description);
			
			return 
				expense.id == id &&
				expense.accountId == accountId &&
				expense.amount == amount &&
				expense.thumbnailId == thumbnailId &&
				expense.date.equals(date) &&
				audioPathSame &&
				imagePathSame &&
				descriptionSame;
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
		 * The compressed (JPEG) thumbnail data for portrait orientation.
		 * <P>
		 * Type: BLOB
		 * </P>
		 */
		public static final String DATA_PORTRAIT = "data_portrait";

		/**
		 * The compressed (JPEG) thumbnail data for landscape orientation.
		 * Can be null; in this case, the portrait data will be used.
		 * <P>
		 * Type: BLOB
		 * </P>
		 */
		public static final String DATA_LANDSCAPE = "data_landscape";

		public static void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" 
					+ ExpenseThumbnailsTable._ID + " INTEGER PRIMARY KEY," 
					+ ExpenseThumbnailsTable.DATA_PORTRAIT + " BLOB,"
					+ ExpenseThumbnailsTable.DATA_LANDSCAPE + " BLOB"
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
			ExpensesTable.onCreate(db);
			ExpenseThumbnailsTable.onCreate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			AccountsTable.onUpgrade(db, oldVersion, newVersion);
			ExpensesTable.onUpgrade(db, oldVersion, newVersion);
			ExpenseThumbnailsTable.onUpgrade(db, oldVersion, newVersion);
		}
	}

	public static CashLensStorage instance(Context currentContext)
			throws IOException, IllegalAccessException
	{
		// use the global application context for the singleton, not the
		// received context, which may become unavailable
		if (mInstance == null)
			mInstance = new CashLensStorage(currentContext
					.getApplicationContext());

		return mInstance;
	}

	public synchronized Account getAccount(int accountId)
	{
		for (Account account: mAccounts)
			if (account.id == accountId)
				return account;
		
		return null;
	}

	public synchronized Currency getCurrency(int currencyId)
	{
		for (Currency currency : mCurrencies)
			if (currency.id == currencyId)
				return currency;
		
		return null;
	}

	public synchronized Expense getExpense(int expenseId)
	{
		for (Expense expense : mExpenses)
			if (expense.id == expenseId)
				return expense;
		
		return null;
	}

	private CashLensStorage(Context context) throws IOException, IllegalAccessException
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
	}

	public void close()
	{
		if (mHelper != null)
		{
			mHelper.close();
			mHelper = null;
		}
	}

	protected SQLiteDatabase db() throws IllegalAccessException
	{
		if (mHelper == null)
			throw new IllegalAccessException("The storage has been closed. Check for close() calls.");
		
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

	protected void readAccounts() throws IllegalAccessException
	{
		Cursor cursor = db().query(AccountsTable.TABLE_NAME, null, null, null,
				null, null, AccountsTable.NAME);

		mAccounts.clear();

		int idIndex = cursor.getColumnIndex(AccountsTable._ID);
		int nameIndex = cursor.getColumnIndex(AccountsTable.NAME);
		int currencyIdIndex = cursor.getColumnIndex(AccountsTable.CURRENCY);
		int monthStartIndex = cursor.getColumnIndex(AccountsTable.MONTH_START);

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			Account account = new Account(this);

			account.id = cursor.getInt(idIndex);
			account.name = cursor.getString(nameIndex);
			account.currencyId = cursor.getInt(currencyIdIndex);
			account.monthStartDay = cursor.getInt(monthStartIndex);

			mAccounts.add(account);

			cursor.moveToNext();
		}

		cursor.close();

		// auto notification is turned off; manually notify of changed data
		mAccounts.notifyDataChanged();
	}

	public synchronized ArrayAdapterIDAndName<Account> accountsAdapter(Context context)
	{
		return new ArrayAdapterIDAndName<Account>(context, mAccounts);
	}
	
	public synchronized ArrayListWithNotify<Account> getAccounts()
	{
		return mAccounts;
	}

	protected void readCurrencies()
	{
		XmlResourceParser xmlParser = mContext.getResources().getXml(R.xml.dl_iso_table_a1);
		
		mCurrencies.clear();
		
		HashSet<Integer> addedCurrencyIds = new HashSet<Integer>();
		Currency currency = null;

		final int EMPTY=0;
		final int IN_ISO_CURRENCY=1;
		final int IN_CURRENCY=2;
		final int IN_ALPHABETIC_CODE=3;
		final int IN_NUMERIC_CODE=4;
		final int IN_IGNORED_TAG=5;

		int state = EMPTY;
		
		try
		{
			xmlParser.next();
			
			int eventType = xmlParser.getEventType();
			while (eventType != XmlResourceParser.END_DOCUMENT)
			{
		        if (xmlParser.getEventType() == XmlResourceParser.START_TAG) 
		        {
	                String s = xmlParser.getName();
	 
	                if (s.equals("ISO_CURRENCY"))
	                {
	                	currency = new Currency();
	                	state = IN_ISO_CURRENCY;
	                }
	                else if (s.equals("CURRENCY"))
	                	state = IN_CURRENCY;
	                else if (s.equals("ALPHABETIC_CODE"))
	                	state = IN_ALPHABETIC_CODE;
	                else if (s.equals("NUMERIC_CODE"))
	                	state = IN_NUMERIC_CODE;
	                else if (state == IN_ISO_CURRENCY)
	                	state = IN_IGNORED_TAG;
		        } 
			    else if (xmlParser.getEventType() == XmlResourceParser.END_TAG) 
			    {
	                if (state == IN_ISO_CURRENCY)
	                {
	                	Integer id = new Integer(currency.id);
	                	
	                	if (!addedCurrencyIds.contains(id))
	                	{
//		                	Log.d("readCurrencies", "New currency: id " + Integer.toString(currency.id) +
//		                			", name " + currency.name + ", code " + currency.code);
		                	
		                	// Currency has been read; add to list
		                	mCurrencies.add(currency);
		                	addedCurrencyIds.add(id);
	                	}
	                	
	                	currency = null;
	                	state = EMPTY;
	                }
	                else if (state != EMPTY)
	                	state = IN_ISO_CURRENCY;
		        } 
			    else if (xmlParser.getEventType() == XmlResourceParser.TEXT) 
			    {
			    	String txt = xmlParser.getText();
			    	
			    	if (state == IN_CURRENCY)
			    		currency.name = txt;
			    	else if (state == IN_ALPHABETIC_CODE)
			    		currency.code = txt;
			    	else if (state == IN_NUMERIC_CODE)
			    		currency.id = Integer.parseInt(txt);
		        }
		 
		        xmlParser.next();
			}
	 
			xmlParser.close();
		}
		catch (Exception e) 
		{
			currency = null;
		}

		// auto notification is turned off; manually notify of changed data
		Collections.sort(mCurrencies);
		mCurrencies.notifyDataChanged();
	}

	public synchronized ArrayListWithNotify<Currency> getCurencies()
	{
		return mCurrencies;
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
	 * @throws IllegalAccessException 
	 */
	protected void saveExpenseToDB(Expense expense) throws IOException, IllegalAccessException
	{
		ContentValues values = new ContentValues();
		values.put(ExpensesTable.ACCOUNT, expense.accountId);
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
		
		// also add it to loaded expenses, but don't notify listeners that data changed
		// (must be done on UI thread and this function is called on a worker thread)
		if (expenseMatchesFilters(expense))
			mExpenses.add(expense);
	}

	public synchronized void saveExpense(Account account, int amount,
			Date date, byte[] jpegData) throws IOException, IllegalAccessException
	{
		File imageFile = randomFile(storageDirectory(DataType.IMAGE), ".jpeg");
		FileOutputStream image = new FileOutputStream(imageFile);

		// Write image data
		image.write(jpegData);
		image.flush();
		image.close();
		
		// Generate and save thumbnail
		ExpenseThumbnail.Data thumbData = ExpenseThumbnail.createFromJPEG(mContext, jpegData, imageFile.getAbsolutePath());
		
		int thumbId = saveExpenseThumbnail(thumbData);
		
		// Save expense to database
		Expense expense = new Expense(this);
		expense.accountId = account.id;
		expense.amount = amount;
		expense.date = date;
		expense.audioPath = null;
		expense.description = null;
		expense.imagePath = imageFile.getAbsolutePath();
		expense.thumbnailId = thumbId;
		
		saveExpenseToDB(expense);
		
		Log.d("saveExpense", "Saved expense with id " + Integer.toString(expense.id) + ", account " + 
				expense.accountName() + ", amount " + expense.amountToString());
	}

	public synchronized void updateExpense(Expense expense) throws IOException, IllegalAccessException
	{
		ContentValues values = new ContentValues();
		values.put(ExpensesTable.ACCOUNT, expense.accountId);
		values.put(ExpensesTable.AMOUNT, expense.amount);
		values.put(ExpensesTable.DATE, dateToUTCInt(expense.date));
		values.put(ExpensesTable.DESCRIPTION, expense.description);
		values.put(ExpensesTable.IMAGE_FILENAME, expense.imagePath);
		values.put(ExpensesTable.IMAGE_THUMBNAIL, expense.thumbnailId);
		values.put(ExpensesTable.AUDIO_FILENAME, expense.audioPath);

		
		int affected = db().update(ExpensesTable.TABLE_NAME, values, 
				ExpensesTable._ID + "=" + Integer.toString(expense.id), null);
		if (affected != 1)
			throw new IOException("Updated " + Integer.toString(affected)
					+ " items instead of 1 from " + ExpensesTable.TABLE_NAME);

		Log.w("updateExpense", "Updated expense: " + expense.amountToString() + 
				expense.currencyCode() + expense.date.toLocaleString() + ", ID " + 
				Integer.toString(expense.id));

		if (!expenseMatchesFilters(expense))
			mExpenses.remove(expense);	// expense no longer matches the WHERE clause; remove
		
		mExpenses.notifyDataChanged();
	}

	public synchronized void deleteExpense(Expense expense) throws IOException, IllegalAccessException
	{
		int affected = db().delete(ExpensesTable.TABLE_NAME,
				ExpensesTable._ID + "=" + Integer.toString(expense.id), null);

		if (affected != 1)
			throw new IOException("Deleted " + Integer.toString(affected)
					+ " items instead of 1 from " + ExpensesTable.TABLE_NAME);

		Log.w("deleteExpense", "Deleted expense: " + expense.amountToString() + 
				expense.currencyCode() + expense.date.toLocaleString() + ", ID " + 
				Integer.toString(expense.id));

		// Also remove from loaded expenses list
		mExpenses.remove(expense);
		mExpenses.notifyDataChanged();
	}

	/**
	 * @param startDate start date, or beginning if null
	 * @param endDate end date, or now if null
	 * @param accountID account to use, or all if null
	 */
	public static class ExpenseFilter
	{
		Date startDate;
		Date endDate;
		int accountId;

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ExpenseFilter other = (ExpenseFilter) obj;
			if (accountId != other.accountId)
				return false;
			if (endDate == null) {
				if (other.endDate != null)
					return false;
			} else if (!endDate.equals(other.endDate))
				return false;
			if (startDate == null) {
				if (other.startDate != null)
					return false;
			} else if (!startDate.equals(other.startDate))
				return false;
			return true;
		}
	}
	
	private boolean expenseFiltersEqual(ExpenseFilter[] filters)
	{
		if (filters == null || mExpenseFilters == null)
			return filters == mExpenseFilters;	// true if both are null
		else if (filters.length != mExpenseFilters.length)
			return false;
		else
		{
			for (int i=0; i<filters.length; ++i)
				if (!filters[i].equals(mExpenseFilters[i]))
					return false;
		}
		
		return true;
	}
	
	// If filterType is not CUSTOM, customFilter is ignored
	public synchronized void setExpenseFilter(ExpenseFilterType filterType, ExpenseFilter customFilter) throws IllegalAccessException
	{
		mExpenseFilterType = filterType;
		if (filterType == ExpenseFilterType.CUSTOM)
			mCustomExpenseFilter = customFilter;
		
		recomputeExpenseFilters();
	}
	
	private boolean recomputeExpenseFilters() throws IllegalAccessException
	{
		Calendar cal = Calendar.getInstance();
		Date now = cal.getTime();
		
		ExpenseFilter[] filters = null;
		
		Log.d("recomputeExpenseFilters", "computing filters");
		
		switch (mExpenseFilterType)
		{
		case NONE:
			break;	// already null
		case CUSTOM:
			filters = new ExpenseFilter[1];
			filters[0] = mCustomExpenseFilter;
			break;
		case DAY:
			filters = new ExpenseFilter[1];
			filters[0] = new ExpenseFilter();
			filters[0].startDate = CashLensUtils.startOfDay(now);
			filters[0].endDate = CashLensUtils.startOfNextDay(now);
			break;
		case WEEK:
			filters = new ExpenseFilter[1];
			filters[0] = new ExpenseFilter();
			filters[0].startDate = CashLensUtils.startOfThisWeek();
			filters[0].endDate = CashLensUtils.startOfNextWeek();
			break;
		case MONTH:
			filters = new ExpenseFilter[mAccounts.size()];
			for (int i=0; i<filters.length; ++i)
			{
				ExpenseFilter filter = new ExpenseFilter();
				Account account = mAccounts.get(i);
				
				filter.accountId = account.id;
				filter.startDate = CashLensUtils.startOfThisMonth(account.monthStartDay); 
				filter.endDate = CashLensUtils.startOfNextMonth(account.monthStartDay);
				
				filters[i] = filter;
			}
			break;
		}
		
		// Need to reread the expenses if the filters have changed
		if (!expenseFiltersEqual(filters))
		{
			Log.d("recomputeExpenseFilters", "filters have changed !");
			for (int i=0; i<filters.length; ++i)
				Log.d("recomputeExpenseFilters", "filter " + Integer.toString(i) +
						": account=" + Integer.toString(filters[i].accountId) +
						", startDate=" + filters[i].startDate.toLocaleString() +
						", endDate=" + filters[i].endDate.toLocaleString());
			
			mExpenseFilters = filters;
			
			// Also notify listeners that the expenses have changed (autoNotify is off)
			readExpenses(true);
			mExpenses.notifyDataChanged();
			
			return true;	// filters have changed, expenses have been reloaded
		}
		
		return false;	// no change
	}

	public synchronized ArrayListWithNotify<Expense> readExpenses(boolean force) throws IllegalAccessException
	{
		// cache the retrieved expenses locally
		if (mExpenses == null)
		{
			mExpenses = new ArrayListWithNotify<Expense>();
			mExpenses.setAutoNotify(false);	// notify will be called manually
			
			force = true;	// need to read
		}
		
		if (!force)
		{
			Log.d("readExpenses", "return cached expenses");
			return mExpenses;	// return cached copy; should be the same
		}
		
		String cond = "";
		boolean needOr = false;
		
		if (mExpenseFilters != null)
		{
			for (int i=0; i<mExpenseFilters.length; ++i)
			{
				ExpenseFilter filter = mExpenseFilters[i];
				String localCond = "";
			
				if (filter.startDate != null)
					localCond = ExpensesTable.DATE + ">=" + dateToUTCInt(filter.startDate);
				
				if (filter.endDate != null)
				{
					if (localCond.length() > 0)
						localCond += " AND ";
					localCond += ExpensesTable.DATE + "<" + dateToUTCInt(filter.endDate);
				}
				
				if (filter.accountId != 0)
				{
					if (localCond.length() > 0)
						localCond += " AND ";
					localCond += ExpensesTable.ACCOUNT + "=" + Integer.toString(filter.accountId);
				}
	
				if (localCond.length() > 0)
				{
					if (needOr)
						cond += " OR ";
		
					cond += "(" + localCond + ")";
					needOr = true;
				}
			}
		}
		
		mExpenses.clear();
		
		Log.d("readExpenses", "Doing read; filter is: " + cond);
		
		Cursor cursor = db().query(ExpensesTable.TABLE_NAME, null, cond,
				null, null, null, ExpensesTable.DATE);

		int idIndex = cursor.getColumnIndex(ExpensesTable._ID);
		int accountIdIndex = cursor.getColumnIndex(ExpensesTable.ACCOUNT);
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
	
	private boolean expenseMatchesFilters(Expense expense)
	{
		if (mExpenseFilters == null)
			return true;	// no filters; everything matches
		
		for (int i=0; i<mExpenseFilters.length; ++i)
		{
			ExpenseFilter filter = mExpenseFilters[i];
			
			// if filter.accountId == 0, any account matches
			if (filter.accountId != 0 && expense.accountId != filter.accountId)
				continue;
			
			// if filter.startDate == null, any start date matches
			if (filter.startDate != null && expense.date.compareTo(filter.startDate) < 0)
				continue;
			
			// if filter.endDate == null, any end date matches
			if (filter.endDate != null && expense.date.compareTo(filter.endDate) >= 0)
				continue;
			
			// we have a match !
			return true;
		}
		
		Log.d("expenseMatchesFilters", "expense with id " + Integer.toString(expense.id) +
				" doesn't match current filters");
		
		return false;
	}
	
	public synchronized void notifyExpensesChanged()
	{
		mExpenses.notifyDataChanged();
	}
	
	public synchronized void addAccount(Account account) throws IOException, IllegalAccessException
	{
		ContentValues values = new ContentValues();
		values.put(AccountsTable.NAME, account.name);
		values.put(AccountsTable.CURRENCY, account.currencyId);
		values.put(AccountsTable.MONTH_START, account.monthStartDay);
		
		long id = (int) db().insert(AccountsTable.TABLE_NAME, null, values);

		if (id < 0)
			throw new IOException(mContext.getString(R.string.cant_insert));

		account.id = (int) id;

		Log.w("addAccount", "New account: " + account.name + ", ID "
				+ Integer.toString(account.id));

		// Also add to accounts list
		mAccounts.add(account);
		
		// Expense filters depend on the account list; recompute 
		recomputeExpenseFilters();

		mAccounts.notifyDataChanged();
	}

	public synchronized void updateAccount(Account account) throws IOException, IllegalAccessException
	{
		ContentValues values = new ContentValues();
		values.put(AccountsTable.NAME, account.name);
		values.put(AccountsTable.CURRENCY, account.currencyId);
		values.put(AccountsTable.MONTH_START, account.monthStartDay);
		
		int affected = db().update(AccountsTable.TABLE_NAME, values, 
				AccountsTable._ID + "=" + Integer.toString(account.id), null);
		if (affected != 1)
			throw new IOException("Updated " + Integer.toString(affected)
					+ " items instead of 1 from " + AccountsTable.TABLE_NAME);

		Log.w("updateAccount", "Updated account: " + account.name + ", ID " + 
				Integer.toString(account.id));

		// Expense filters depend on the account list; recompute 
		recomputeExpenseFilters();
		
		mAccounts.notifyDataChanged();
	}

	public synchronized void deleteAccount(Account account) throws IOException, IllegalAccessException
	{
		int affected = db().delete(AccountsTable.TABLE_NAME,
				AccountsTable._ID + "=" + Integer.toString(account.id), null);

		if (affected != 1)
			throw new IOException("Deleted " + Integer.toString(affected)
					+ " items instead of 1 from " + AccountsTable.TABLE_NAME);

		affected = db().delete(ExpensesTable.TABLE_NAME, 
				ExpensesTable.ACCOUNT + "=" + Integer.toString(account.id), null);
		
		Log.w("deleteAccount", "Deleted account: " + account.name + ", ID "
				+ Integer.toString(account.id) + " and " + Integer.toString(affected)
				+ " expenses belonging to it");

		// Also remove from accounts list
		mAccounts.remove(account);
		
		// Expense filters depend on the account list; recompute.
		// If the filters have changed, the expenses will be reloaded automatically and 
		// we won't need to reload them manually here
		if (!recomputeExpenseFilters())
		{
			// Don't reload if not already loaded
			if (mExpenses != null)
			{
				mExpenses.clear();
				readExpenses(true);
				mExpenses.notifyDataChanged();
			}
		}
		
		mAccounts.notifyDataChanged();
	}
	
	protected int saveExpenseThumbnail(ExpenseThumbnail.Data thumbData) throws IOException, IllegalAccessException
	{
		ContentValues values = new ContentValues();
		values.put(ExpenseThumbnailsTable.DATA_PORTRAIT, thumbData.portraitData);
		values.put(ExpenseThumbnailsTable.DATA_LANDSCAPE, thumbData.landscapeData);

		int id = (int) db().insert(ExpenseThumbnailsTable.TABLE_NAME, null, values);

		if (id < 0)
			throw new IOException(mContext.getString(R.string.cant_insert));

		Log.w("saveExpenseThumbnail", "New thumbnail: ID " + Integer.toString(id));
		
		return id;
	}

	public synchronized void loadExpenseThumbnail(ExpenseThumbnail thumb) throws IOException, IllegalAccessException
	{
		Cursor cursor = db().query(ExpenseThumbnailsTable.TABLE_NAME,
				new String[] { ExpenseThumbnailsTable.DATA_PORTRAIT, ExpenseThumbnailsTable.DATA_LANDSCAPE }, 
				ExpenseThumbnailsTable._ID + "=" + Integer.toString(thumb.id),
				null, null, null, null);

		cursor.moveToFirst();
		
		thumb.decodeFromByteArray(cursor.getBlob(0), true);
		
		// landscape thumbnail is optional
		if (!cursor.isNull(1))
			thumb.decodeFromByteArray(cursor.getBlob(1), false);
		
		Log.w("loadExpenseThumbnail", "Loaded thumbnail: ID " + Integer.toString(thumb.id));

		cursor.close();
	}
}
