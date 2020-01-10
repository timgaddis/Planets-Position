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

package planets.position;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

public abstract class DeferredFragmentTransaction {

    private int contentFrameId;
    private String contentTag;
    private Fragment replacingFragment;
    private DialogFragment dialogFragment;

    public abstract void commit();

    protected String getContentTag() {
        return contentTag;
    }

    public void setContentTag(String contentTag) {
        this.contentTag = contentTag;
    }

    protected DialogFragment getDialogFragment() {
        return dialogFragment;
    }

    public void setDialogFragment(DialogFragment dialogFragment) {
        this.dialogFragment = dialogFragment;
    }

    int getContentFrameId() {
        return contentFrameId;
    }

    public void setContentFrameId(int contentFrameId) {
        this.contentFrameId = contentFrameId;
    }

    Fragment getReplacingFragment() {
        return replacingFragment;
    }

    public void setReplacingFragment(Fragment replacingFragment) {
        this.replacingFragment = replacingFragment;
    }

}
