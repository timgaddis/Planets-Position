package planets.position;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import planets.position.Util.PositionFormat;

public class UserLocation extends Fragment {

    private FragmentListener mCallbacks;
    private TextView latitudeText, longitudeText, elevationText, gmtOffsetText,
            editLocText;
    private PositionFormat pf;

    public UserLocation() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_user_location, container, false);
        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("User Location", 5);
        }

        latitudeText = (TextView) v.findViewById(R.id.newLatLabel);
        longitudeText = (TextView) v.findViewById(R.id.newLongText);
        elevationText = (TextView) v.findViewById(R.id.newElevationText);
        gmtOffsetText = (TextView) v.findViewById(R.id.newGMTOffsetText);
        editLocText = (TextView) v.findViewById(R.id.locEditText);

        pf = new PositionFormat(getActivity().getApplicationContext());

        loadLocation();

        return v;
    }

    private void loadLocation() {

        double ra, dec, az, alt;
        ra = 5.458967510328336;
        dec = 23.2260666095222;
        az = 298.3351453874998;
        alt = 33.81055373204086;

        latitudeText.setText(pf.formatRA(ra));
        longitudeText.setText(pf.formatDec(dec));
        elevationText.setText(pf.formatAZ(az));
        gmtOffsetText.setText(pf.formatALT(alt));

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getTargetFragment() == null) {
            // attach to PlanetsMain
            if (!(context instanceof FragmentListener)) {
                throw new IllegalStateException(
                        "Activity must implement the FragmentListener interface.");
            }
            mCallbacks = (FragmentListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

}
