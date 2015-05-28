package fi.bitrite.android.ws.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.HashMap;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.model.NavRow;

abstract class WSBaseActivity extends AppCompatActivity implements android.widget.AdapterView.OnItemClickListener {
    protected Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mLeftDrawerList;
    private NavDrawerListAdapter mNavDrawerListAdapter;
    private int currentActivity;

    public static final String TAG = "WSBaseActivity";
    protected String mActivityName = this.getClass().getSimpleName();
    protected ArrayList<NavRow> mNavRowList = new ArrayList<NavRow>();
    String mActivityFriendly;

    protected boolean mHasBackIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] navMenuOptions = getResources().getStringArray(R.array.nav_menu_options);
        String[] navMenuActivities = getResources().getStringArray(R.array.nav_menu_activities);
        HashMap<String, String> mActivityClassToFriendly = new HashMap<String, String>();

        TypedArray icons = getResources().obtainTypedArray(R.array.nav_menu_icons);
        for (int i=0; i<navMenuOptions.length; i++) {
            mActivityClassToFriendly.put(navMenuActivities[i], navMenuOptions[i]);

            int icon = icons.getResourceId(i, R.drawable.ic_action_email);
            NavRow row = new NavRow(icon, navMenuOptions[i], navMenuActivities[i]);
            mNavRowList.add(row);

            if (navMenuActivities[i].equals(mActivityName)) currentActivity = i;
        }
        mActivityFriendly = mActivityClassToFriendly.get(mActivityName);
    }

    protected void initView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mLeftDrawerList = (ListView) mDrawerLayout.findViewById(R.id.left_drawer);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mNavDrawerListAdapter = new NavDrawerListAdapter(this, mNavRowList);
        mLeftDrawerList.setAdapter(mNavDrawerListAdapter);
        mLeftDrawerList.setOnItemClickListener(this);

        if (mToolbar != null) {
            mToolbar.setTitle(mActivityFriendly);
            setSupportActionBar(mToolbar);
        }
        initDrawer();
    }

    private void initDrawer() {

        ListView leftDrawer = (ListView)findViewById(R.id.left_drawer);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                showDrawerSelection(currentActivity);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Make sure we have an active account, or go to authentication screen
        if (!setupCredentials()) {
            return;
        }

        TextView lblUsername = (TextView) mDrawerLayout.findViewById(R.id.lblUsername);
        TextView lblNotLoggedIn = (TextView) mDrawerLayout.findViewById(R.id.lblNotLoggedIn);

        try {
            String username = AuthenticationHelper.getAccountUsername();
            lblUsername.setText(username);
        } catch (NoAccountException e) {
            lblNotLoggedIn.setVisibility(View.VISIBLE);
            lblUsername.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();

        if (mHasBackIntent) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)){
            mDrawerLayout.closeDrawers();
            return;
        }
        super.onBackPressed();
    }

    /**
     * Handle click from ListView in NavigationDrawer
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String[] activities = getResources().getStringArray(R.array.nav_menu_activities);
        if (mActivityName.equals(activities[position])) return;

        try {
            Class activityClass =  Class.forName(this.getPackageName() + ".activity." + activities[position]);
            Intent i = new Intent(this, activityClass);
            startActivity(i);
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "Class not found: " + activities[position]);
        }

        mDrawerLayout.closeDrawers();
    }


    /**
     * Highlight the text and icon in the selected item on the nav drawer
     *
     * @param position
     */
    public void showDrawerSelection(int position) {
        View rowView = mLeftDrawerList.getChildAt(position);
        int accentColor = getResources().getColor(R.color.primaryColorAccent);

        ((TextView)rowView.findViewById(R.id.menu_text)).setTextColor(accentColor);
        ((ImageView)rowView.findViewById(R.id.icon)).setColorFilter(accentColor);
    }


    private void startAuthenticatorActivity(Intent i) {
        startActivityForResult(i, AuthenticatorActivity.REQUEST_TYPE_AUTHENTICATE);
    }

    /**
     *
     * @return
     *   true if we already have an account set up in the AccountManager
     *   false if we have to wait for the auth screen to process
     */
    public boolean setupCredentials() {
        try {
            AuthenticationHelper.getWarmshowersAccount();
            return true;
        }
        catch (NoAccountException e) {

            if (this.getClass() != AuthenticatorActivity.class) {  // Would be circular, so don't do it.
                startAuthenticatorActivity(new Intent(this, AuthenticatorActivity.class));
            }
            return false;
        }
    }

}