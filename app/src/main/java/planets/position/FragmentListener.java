package planets.position;

import android.location.Location;

public interface FragmentListener {
    void onTaskFinished(Location location, int index);

    void onToolbarTitleChange(CharSequence title, int index);
}
