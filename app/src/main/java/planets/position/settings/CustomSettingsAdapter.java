/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2020 Tim Gaddis
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

package planets.position.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import planets.position.R;

public class CustomSettingsAdapter extends BaseAdapter {
    private ArrayList<Setting> settingsList;
    private final LayoutInflater mInflater;

    CustomSettingsAdapter(Context c, ArrayList<Setting> list) {
        settingsList = list;
        mInflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return settingsList.size();
    }

    @Override
    public Object getItem(int position) {
        return settingsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.settings_row, null);
            holder = new ViewHolder();
            holder.txtTitle = convertView.findViewById(R.id.txtTitle);
            holder.txtFormat = convertView.findViewById(R.id.txtFormat);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.txtTitle.setText(settingsList.get(position).getTitle());
        holder.txtFormat.setText(settingsList.get(position).getFormat());
        notifyDataSetChanged();

        return convertView;
    }

    static class ViewHolder {
        TextView txtTitle;
        TextView txtFormat;
    }

    public void updateResults(ArrayList<Setting> results) {
        settingsList = results;
        notifyDataSetChanged();
    }

}
