package org.tndata.android.compass.model;

import android.widget.ImageView;

import org.tndata.android.compass.util.ImageLoader;

import java.io.Serializable;
import java.util.Set;


/**
 * Model class for goals.
 *
 * @author Edited by Ismael Alonso
 * @version 1.0.0
 */
public class Goal extends TDCBase implements Serializable{
    private static final long serialVersionUID = 7109406671934150671L;

    private String outcome = "";
    private Set<Integer> categories;
    private int behaviors_count = 0;


    /*---------*
     * SETTERS *
     *---------*/

    public void setOutcome(String outcome){
        this.outcome = outcome;
    }

    public void setCategories(Set<Integer> categories){
        this.categories = categories;
    }

    public void setBehaviorCount(int behaviorCount){
        this.behaviors_count = behaviorCount;
    }


    /*---------*
     * GETTERS *
     *---------*/

    public String getOutcome(){
        return outcome;
    }

    public Set<Integer> getCategories(){
        return categories;
    }

    public int getBehaviorCount(){
        return behaviors_count;
    }


    /*---------*
     * UTILITY *
     *---------*/

    /**
     * Given a Context and an ImageView, load this Goal's icon (if the user has selected
     * no Behaviors) or load the goal's Progress Icons.
     *
     * @param imageView: an ImageView
     */
    public void loadIconIntoView(ImageView imageView){
        String iconUrl = getIconUrl();
        if (iconUrl != null && !iconUrl.isEmpty()){
            ImageLoader.loadBitmap(imageView, iconUrl);
        }
    }

    @Override
    public String toString(){
        return "Goal #" + getId() + ": " + getTitle();
    }
}
