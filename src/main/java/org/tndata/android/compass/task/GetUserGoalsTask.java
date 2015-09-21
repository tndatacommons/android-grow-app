package org.tndata.android.compass.task;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.tndata.android.compass.model.Goal;
import org.tndata.android.compass.util.Constants;
import org.tndata.android.compass.util.NetworkHelper;
import org.tndata.android.compass.util.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GetUserGoalsTask extends AsyncTask<String, Void, List<Goal>>{
    private GetUserGoalsListener mCallback;


    public GetUserGoalsTask(@NonNull GetUserGoalsListener callback){
        mCallback = callback;
    }

    @Override
    protected List<Goal> doInBackground(String... params){
        String url = Constants.BASE_URL + "users/goals/";

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-type", "application/json");
        headers.put("Authorization", "Token " + params[0]);

        InputStream stream = NetworkHelper.httpGetStream(url, headers);
        if (stream == null){
            return null;
        }

        try{
            BufferedReader bReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

            String line, result = "";
            while ((line = bReader.readLine()) != null){
                result += line;
            }
            bReader.close();

            return new Parser().parseGoals(new JSONObject(result).getJSONArray("results"), true);

        }
        catch (IOException|JSONException x){
            x.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<Goal> goals){
        mCallback.goalsLoaded(goals);
    }


    public interface GetUserGoalsListener{
        void goalsLoaded(List<Goal> goals);
    }
}
