package planets.position;

public interface FragmentListener {

    void onToolbarTitleChange(CharSequence title, int index);

    void onDialogPositiveClick();

    void onDialogNegativeClick();
}
