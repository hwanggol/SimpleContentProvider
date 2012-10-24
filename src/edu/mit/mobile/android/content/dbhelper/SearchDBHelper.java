package edu.mit.mobile.android.content.dbhelper;

import java.util.ArrayList;
import java.util.LinkedList;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.DBHelper;
import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.SQLGenerationException;

/**
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 *
 */
public class SearchDBHelper extends DBHelper {

    LinkedList<RegisteredHelper> mRegisteredHelpers = new LinkedList<SearchDBHelper.RegisteredHelper>();

    public SearchDBHelper() {

    }

    public void registerDBHelper(GenericDBHelper helper, Uri contentUri, String text1Column,
            String text2Column, String... searchColumns) {
        mRegisteredHelpers.add(new RegisteredHelper(helper, contentUri, text1Column, text2Column,
                searchColumns));
    }

    private static class RegisteredHelper {
        public final GenericDBHelper mHelper;
        public final String[] mColumns;
        public final String mText2Column;
        public final String mText1Column;
        public final Uri mContentUri;

        public RegisteredHelper(GenericDBHelper helper, Uri contentUri, String text1Column,
                String text2Column, String... columns) {
            mHelper = helper;
            mColumns = columns;
            mContentUri = contentUri;
            mText1Column = text1Column;
            mText2Column = text2Column;
        }
    }

    @Override
    public Uri insertDir(SQLiteDatabase db, ContentProvider provider, Uri uri, ContentValues values)
            throws SQLException {
        throw new UnsupportedOperationException("insert not supported for this helper");
    }

    @Override
    public int updateItem(SQLiteDatabase db, ContentProvider provider, Uri uri,
            ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("update not supported for this helper");
    }

    @Override
    public int updateDir(SQLiteDatabase db, ContentProvider provider, Uri uri,
            ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("update not supported for this helper");
    }

    @Override
    public int deleteItem(SQLiteDatabase db, ContentProvider provider, Uri uri, String where,
            String[] whereArgs) {
        throw new UnsupportedOperationException("delete not supported for this helper");
    }

    @Override
    public int deleteDir(SQLiteDatabase db, ContentProvider provider, Uri uri, String where,
            String[] whereArgs) {
        throw new UnsupportedOperationException("delete not supported for this helper");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @SuppressWarnings("deprecation")
    @Override
    public Cursor queryItem(SQLiteDatabase db, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        final String searchQuery = uri.getLastPathSegment();

        final StringBuilder multiSelect = new StringBuilder();

        multiSelect.append('(');

        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        boolean addUnion = false;
        for (final RegisteredHelper searchReg : mRegisteredHelpers) {

            // UNION ALL concatenates the inner queries
            if (addUnion) {
                multiSelect.append(" UNION ALL ");
            }
            addUnion = true;

            final StringBuilder extSel = new StringBuilder();

            // build the selection that matches the search string in the given
            int i = 0;
            for (final String column : searchReg.mColumns) {
                if (i > 0) {
                    extSel.append(" OR ");
                }

                extSel.append("\"");
                extSel.append(column);
                extSel.append("\" LIKE ?1");

                i++;
            }

            final ArrayList<String> extProj = new ArrayList<String>();
            final String table = searchReg.mHelper.getTable();
            final String tablePrefix = '"' + table + "\".";

            extProj.add(tablePrefix + ContentItem._ID + " AS " + ContentItem._ID);

            extProj.add(tablePrefix + searchReg.mText1Column + " AS "
                    + SearchManager.SUGGEST_COLUMN_TEXT_1);

            if (searchReg.mText2Column != null) {
                extProj.add(tablePrefix + searchReg.mText2Column + "  AS "
                        + SearchManager.SUGGEST_COLUMN_TEXT_2);
            } else {
                // this is needed as sqlite3 crashes otherwise.
                extProj.add("'' AS " + SearchManager.SUGGEST_COLUMN_TEXT_2);
            }

            if (searchReg.mContentUri != null) {
                extProj.add("'" + searchReg.mContentUri.toString() + "' AS "
                        + SearchManager.SUGGEST_COLUMN_INTENT_DATA);
                extProj.add(tablePrefix + ContentItem._ID + " AS "
                        + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
            }

            qb.setTables(table);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                multiSelect.append(qb.buildQuery(extProj.toArray(new String[extProj.size()]),
                        ProviderUtils.addExtraWhere(selection, extSel.toString()), null, null,
                        sortOrder, null));
            } else {
                multiSelect.append(qb.buildQuery(extProj.toArray(new String[extProj.size()]),
                        ProviderUtils.addExtraWhere(selection, extSel.toString()), null, null,
                        null, sortOrder, null));
            }

        } // inner selects

        multiSelect.append(')');

        return db.query(multiSelect.toString(), null, selection,
                ProviderUtils.addExtraWhereArgs(selectionArgs, "%" + searchQuery + "%"), null,
                null, sortOrder);
    }

    @Override
    public Cursor queryDir(SQLiteDatabase db, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("query dir not supported for this helper");
    }

    @Override
    public String getPath() {
        // unused
        return null;
    }

    @Override
    public void createTables(SQLiteDatabase db) throws SQLGenerationException {
        // unused
    }

    @Override
    public void upgradeTables(SQLiteDatabase db, int oldVersion, int newVersion)
            throws SQLGenerationException {
        // unused
    }

}