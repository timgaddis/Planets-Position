package planets.position;

import android.location.Location;

public interface FragmentListener {
    void onLocationFound(Location location, int index);

    void onToolbarTitleChange(CharSequence title, int index);

    void onDialogPositiveClick();

    void onDialogNegativeClick();
}
