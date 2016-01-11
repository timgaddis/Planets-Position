package planets.position;

import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class PlanetsMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FragmentListener {

    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        setContentView(R.layout.activity_planets_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            getDelegate().setSupportActionBar(toolbar);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        getDelegate().getSupportActionBar().setHomeButtonEnabled(true);
        getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (savedInstanceState == null) {
            selectItem(0, false, false);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.planets_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_solar_ecl) {

        } else if (id == R.id.nav_lunar_ecl) {

        } else if (id == R.id.nav_lunar_occ) {

        } else if (id == R.id.nav_sky_pos) {

        } else if (id == R.id.nav_whats_up) {

        } else if (id == R.id.nav_location) {
            selectItem(7, false, true);
        } else if (id == R.id.nav_settings) {
            selectItem(8, false, true);
        } else if (id == R.id.nav_about) {
            selectItem(9, false, true);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void navigate(int position, boolean edit, boolean back) {
        selectItem(position, edit, back);
    }

    private void selectItem(int position, boolean edit, boolean back) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Bundle args = new Bundle();

        switch (position) {
            case 0: // Main navigaton
                ft.replace(R.id.content_frame, new Navigation());
                if (back)
                    ft.addToBackStack(null);
                ft.commit();
                break;
//            case 1:
//                if (longitude == 0)
//                    loadLocation();
//                title = "Solar Eclipse";
//                ft.replace(R.id.content_frame, new SolarEclipse());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
//            case 3:
//                title = "Lunar Eclipse";
//                ft.replace(R.id.content_frame, new LunarEclipse());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
//            case 4:
//                title = "Lunar Occultation";
//                ft.replace(R.id.content_frame, new LunarOccultation());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
//            case 5:
//                if (longitude == 0)
//                    loadLocation();
//                title = "Sky Position";
//                ft.replace(R.id.content_frame, new SkyPosition());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
//            case 6:
//                if (longitude == 0)
//                    loadLocation();
//                title = "What's Up Now";
//                ft.replace(R.id.content_frame, new WhatsUpNow());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
            case 7: // User Location
                UserLocation userLoc = new UserLocation();
                args.putBoolean("edit", edit);
                userLoc.setArguments(args);
                ft.replace(R.id.content_frame, userLoc);
                if (back)
                    ft.addToBackStack(null);
                ft.commit();
                break;
            case 8: // Settings
                ft.replace(R.id.content_frame, new SettingsFragment());
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 9: // About
                ft.replace(R.id.content_frame, new About());
                ft.addToBackStack(null);
                ft.commit();
                break;
        }
    }

    @Override
    public void onTaskFinished(Location location, int index) {

    }

    @Override
    public void onToolbarTitleChange(CharSequence title, int index) {
        //clear previous selection
        for (int i = 0; i <= 7; i++) {
            navigationView.getMenu().getItem(i).setChecked(false);
        }
        getDelegate().getSupportActionBar().setTitle(title);
        if (index >= 0) {
            navigationView.getMenu().getItem(index).setChecked(true);
        }
    }
}
