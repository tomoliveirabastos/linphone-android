/*
ContactPickerActivity.java
Copyright (C) 2010  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.linphone;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.provider.Contacts.Photos;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

@SuppressWarnings("deprecation")
public class ContactPickerActivityOld extends Activity {
    static final int PICK_CONTACT_REQUEST = 0;
    static final int PICK_PHONE_NUMBER_REQUEST = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);    

    }

    @Override
	protected void onResume() {
		super.onResume();
        startActivityForResult(new Intent(Intent.ACTION_PICK, Contacts.Phones.CONTENT_URI)
		,
		PICK_CONTACT_REQUEST);

	}

    private Uri getPhotoUri(Uri photoUriToTest) {
		return retrievePhotoUri(getContentResolver(), photoUriToTest);
	}

	private static Uri retrievePhotoUri(ContentResolver resolver, Uri photoUriToTest) {
    	Cursor cursor = resolver.query(photoUriToTest, new String[]{Photos.DATA}, null, null, null);
    	try {
    		if (cursor == null || !cursor.moveToNext()) {
    			return null;
    		}   
    		byte[] data = cursor.getBlob(0);
    		if (data == null) {
    			// TODO: simplify all this stuff
				// which is here only to check that the
				// photoUri really points to some data.
				// Not retrieving the data now would be better.
    			return null;
    		}   
    		return photoUriToTest;
    	} finally {
    		if (cursor != null) cursor.close();
    	}   
    }   

	protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
            	String lColumns[] = new String[] { People._ID, People.NAME, People.NUMBER };
 
                Cursor lCur = managedQuery(data.getData(), lColumns, // Which columns to return
                        null, // WHERE clause; which rows to return(all rows)
                        null, // WHERE clause selection arguments (none)
                        null // Order-by clause (ascending by name)

                );
                if (lCur.moveToFirst()) {
                    String lName = lCur.getString(lCur.getColumnIndex(People.NAME));
                    String lPhoneNo = lCur.getString(lCur.getColumnIndex(People.NUMBER));
                    long id = lCur.getLong(lCur.getColumnIndex(People._ID));
                    Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, id);
                    Uri potentialPictureUri = Uri.withAppendedPath(personUri, Contacts.Photos.CONTENT_DIRECTORY);
                    Uri pictureUri = getPhotoUri(potentialPictureUri);
                    // FIXME surprisingly all this picture stuff doesn't seem to work
                    DialerActivity.instance().setContactAddress(lPhoneNo, lName, pictureUri);
                }
            }
            	
            	LinphoneActivity.instance().getTabHost().setCurrentTabByTag(LinphoneActivity.DIALER_TAB);	
            }
        }

	private static Uri retrievePhotoUriAndCloseC(ContentResolver resolver, Cursor c, String column) {
		if (c == null) return null;
		while (c.moveToNext()) {
			long id = c.getLong(c.getColumnIndex(column));
            Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, id);
			Uri potentialPictureUri = Uri.withAppendedPath(personUri, Contacts.Photos.CONTENT_DIRECTORY);
			Uri pictureUri = retrievePhotoUri(resolver, potentialPictureUri);
			if (pictureUri != null) {
				c.close();
				return personUri; // FIXME, see LinphoneUtils
			}
		}
		c.close();
		return null;
	}

	public static Uri findUriPictureOfContact(ContentResolver resolver, String username, String domain) {
		String normalizedNumber = PhoneNumberUtils.getStrippedReversed(username);
		if (TextUtils.isEmpty(normalizedNumber)) {
			// non phone username
			return null;
		}
		String[] projection = {Contacts.Phones.PERSON_ID};
		String selection = Contacts.Phones.NUMBER_KEY + "=" + normalizedNumber;
		Cursor c = resolver.query(Contacts.Phones.CONTENT_URI, projection, selection, null, null);
		
		return retrievePhotoUriAndCloseC(resolver, c, Contacts.Phones.PERSON_ID);
	}
}
