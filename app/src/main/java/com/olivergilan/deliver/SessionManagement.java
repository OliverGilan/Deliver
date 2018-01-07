package com.olivergilan.deliver;

/**
 * Created by olivergilan on 11/24/17.
 */

import java.util.HashMap;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SessionManagement {

    SharedPreferences pref;

    Editor editor;

    Context _context;

    int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "DeliverPref";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";

    // User name (make variable public to access from outside)
    public static final String KEY_NAME = "name";

    // Email address (make variable public to access from outside)
    public static final String KEY_EMAIL = "email";

    //Constructor
    public SessionManagement(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(KEY_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

}
