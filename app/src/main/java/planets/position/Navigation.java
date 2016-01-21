/*
 * Copyright (c) 2014. Tim Gaddis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package planets.position;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class Navigation extends Fragment {

    private FragmentListener mCallbacks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Button seButton, skyButton, whatsupButton, leButton, loButton;
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.content_planets_main,
                container, false);

        seButton = (Button) view.findViewById(R.id.buttonSEMain);
        leButton = (Button) view.findViewById(R.id.buttonLEMain);
        loButton = (Button) view.findViewById(R.id.buttonLOMain);
        skyButton = (Button) view.findViewById(R.id.buttonSky);
        whatsupButton = (Button) view.findViewById(R.id.buttonWhatsUp);

        skyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((PlanetsMain) getActivity()).navigate(5, false, true);
            }
        });

        seButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                ((PlanetsMain) getActivity()).navigate(1, false, true);
            }
        });

        leButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                ((PlanetsMain) getActivity()).navigate(3, false, true);
            }
        });

        loButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                ((PlanetsMain) getActivity()).navigate(4, false, true);
            }
        });

        whatsupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                ((PlanetsMain) getActivity()).navigate(6, false, true);
            }
        });

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Planet's Position", -1);
        }

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
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
