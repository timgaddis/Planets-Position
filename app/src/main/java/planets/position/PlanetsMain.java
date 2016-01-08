package planets.position;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class PlanetsMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planets_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // This gets the top fragment on the back stack and calls its onResume
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        FragmentManager manager = getSupportFragmentManager();
                        Fragment fragment = null;
                        if (manager != null) {
                            int backStackEntryCount = manager
                                    .getBackStackEntryCount();
                            if (backStackEntryCount == 0) {
                                return;
                            }
                            List<Fragment> fragments = manager.getFragments();
                            if (backStackEntryCount < fragments.size())
                                fragment = fragments.get(backStackEntryCount);
                            if (fragment != null)
                                fragment.onResume();
                        }
                    }
                });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        selectItem(0, false, true);
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
    protected void onResumeFragments() {
//        getDelegate().getSupportActionBar().setTitle("Planet's Position");
        super.onResumeFragments();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.planets_main, menu);
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
        CharSequence title = "";
        Bundle args = new Bundle();

        switch (position) {
            case 0:
                title = "Planet\'s Position";
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
            case 7:
                title = "User Location";
                UserLocation userLoc = new UserLocation();
                args.putBoolean("edit", edit);
                userLoc.setArguments(args);
                ft.replace(R.id.content_frame, userLoc);
                if (back)
                    ft.addToBackStack(null);
                ft.commit();
                break;
//            case 8:
//                title = "Settings";
//                Intent i = new Intent(this, SettingsActivity.class);
//                startActivity(i);
//                break;
            case 9:
                title = "About";
                ft.replace(R.id.content_frame, new About());
                ft.addToBackStack(null);
                ft.commit();
                break;
        }
        getDelegate().getSupportActionBar().setTitle(title);
    }
}
