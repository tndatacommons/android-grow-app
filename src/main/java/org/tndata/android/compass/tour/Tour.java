package org.tndata.android.compass.tour;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import org.tndata.android.compass.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;


/**
 * Created by isma on 8/30/16.
 */
public class Tour implements OnShowcaseEventListener{
    private static Context sContext;
    private static Tour sTour;


    public static void init(Context context){
        sContext = context.getApplicationContext();
    }

    public static List<Tooltip> getTooltipsFor(Section section){
        List<Tooltip> tooltips = new ArrayList<>();
        for (Tooltip tooltip:section.mTooltips){
            if (!hasBeenSeen(tooltip)){
                tooltips.add(tooltip);
            }
        }
        return tooltips;
    }

    private static boolean hasBeenSeen(Tooltip tooltip){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(sContext);
        return preferences.getBoolean(tooltip.getKey(), false);
    }

    private static void markSeen(Tooltip tooltip){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(sContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(tooltip.getKey(), true);
        editor.apply();
    }

    public static void display(Activity activity, Queue<Tooltip> tooltips){
        display(activity, tooltips, null);
    }

    public static void display(Activity activity, Queue<Tooltip> tooltips, TourListener listener){
        if (tooltips.size() != 0){
            if (sTour == null){
                sTour = new Tour();
            }
            sTour.displayTooltips(activity, tooltips, listener);
        }
    }

    public static void display(Activity activity, Collection<CoachMark> marks, final TourListener listener){

    }

    public static void reset(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(sContext);
        SharedPreferences.Editor editor = preferences.edit();
        for (Tooltip tooltip:Tooltip.values()){
            editor.putBoolean(tooltip.getKey(), false);
        }
        editor.apply();
    }


    private Activity mActivity;
    private Queue<Tooltip> mTooltips;
    private TourListener mListener;


    private void displayTooltips(Activity activity, Queue<Tooltip> tooltips, TourListener listener){
        mActivity = activity;
        mTooltips = tooltips;
        mListener = listener;
        displayNextTooltip();
    }

    private void displayNextTooltip(){
        Log.d("Tour", "displayNextTooltip(), " + mTooltips.size() + " tooltips left.");
        if (!mTooltips.isEmpty()){
            Tooltip nextTooltip = mTooltips.peek();

            ShowcaseView.Builder builder = new ShowcaseView.Builder(mActivity)
                    .setStyle(R.style.CompassShowcaseView)
                    .setContentTitle(nextTooltip.getTitleId())
                    .setContentText(nextTooltip.getDescriptionId())
                    .setShowcaseEventListener(this)
                    .blockAllTouches()
                    .hideOnTouchOutside()
                    .withMaterialShowcase();

            if (nextTooltip.getTarget() != null){
                builder.setTarget(new ViewTarget(nextTooltip.getTarget()));
            }
            builder.build();
        }
    }

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView){
        //First off the tooltip being hidden is removed from the queue for displayNext
        //  to work properly
        Tooltip clickedTooltip = mTooltips.remove();
        displayNextTooltip();
        //The tooltip is marked seen before the listener is notified
        markSeen(clickedTooltip);
        if (mListener != null){
            mListener.onTooltipClick(clickedTooltip);
        }
    }

    @Override
    public void onShowcaseViewDidHide(ShowcaseView showcaseView){

    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView){

    }

    @Override
    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent){

    }


    public enum Section{
        ORGANIZATION(Tooltip.ORG_GENERAL, Tooltip.ORG_SKIP),
        CATEGORY(Tooltip.CAT_GENERAL, Tooltip.CAT_SKIP),
        LIBRARY_PRE(Tooltip.LIB_GENERAL),
        GOAL(Tooltip.GOAL_GENERAL),
        LIBRARY_POST(Tooltip.LIB_GOAL_ADDED),
        FEED(Tooltip.FEED_GENERAL, Tooltip.FEED_UP_NEXT, Tooltip.FEED_PROGRESS, Tooltip.FEED_FAB),
        ACTION(Tooltip.ACTION_GOT_IT);


        private final Tooltip[] mTooltips;


        Section(Tooltip... tooltips){
            mTooltips = tooltips;
        }
    }


    public interface TourListener{
        void onTooltipClick(Tooltip tooltip);
    }


    public enum Tooltip{
        ORG_GENERAL("org_general", R.string.tour_org_general_title, R.string.tour_org_general_description),
        ORG_SKIP("org_skip", R.string.tour_org_skip_title, R.string.tour_org_skip_description),
        CAT_GENERAL("cat_general", R.string.tour_cat_general_title, R.string.tour_cat_general_description),
        CAT_SKIP("cat_skip", R.string.tour_cat_skip_title, R.string.tour_cat_skip_description),
        LIB_GENERAL("lib_general", R.string.tour_lib_general_title, R.string.tour_lib_general_description),
        GOAL_GENERAL("goal_general", R.string.tour_goal_add_title, R.string.tour_goal_add_description),
        LIB_GOAL_ADDED("lib_goal_added", R.string.tour_lib_added_title, R.string.tour_lib_added_description),
        FEED_GENERAL("feed_general", R.string.tour_feed_general_title, R.string.tour_feed_general_description),
        FEED_UP_NEXT("feed_up_next", R.string.tour_feed_up_next_title, R.string.tour_feed_up_next_description),
        FEED_PROGRESS("feed_progress", R.string.tour_feed_progress_title, R.string.tour_feed_progress_description),
        FEED_FAB("feed_fab", R.string.tour_feed_fab_title, R.string.tour_feed_fab_description),
        ACTION_GOT_IT("action_got_it", R.string.tour_action_got_it_title, R.string.tour_action_got_it_description);


        private final String mKey;
        private final int mTitleId;
        private final int mDescriptionId;
        private View mTarget;


        Tooltip(String key, @StringRes int titleId, @StringRes int descriptionId){
            mKey = key;
            mTitleId = titleId;
            mDescriptionId = descriptionId;
            mTarget = null;
        }

        public void setTarget(View target){
            mTarget = target;
        }

        public String getKey(){
            return mKey;
        }

        public int getTitleId(){
            return mTitleId;
        }

        public int getDescriptionId(){
            return mDescriptionId;
        }

        public String getTitle(){
            return sContext.getString(mTitleId);
        }

        public String getDescription(){
            return sContext.getString(mDescriptionId);
        }

        public View getTarget(){
            return mTarget;
        }
    }
}
