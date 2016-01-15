package org.tndata.android.compass.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;
import org.tndata.android.compass.adapter.PlacesAdapter;
import org.tndata.android.compass.database.CompassDbHelper;
import org.tndata.android.compass.model.Place;
import org.tndata.android.compass.model.UserPlace;
import org.tndata.android.compass.util.API;
import org.tndata.android.compass.util.NetworkRequest;
import org.tndata.android.compass.parser.PlaceParser;

import java.util.ArrayList;
import java.util.List;


/**
 * This activity lists the places defined by the user plus all of the primary places, either
 * set or not by the user. It allows place edition and addition.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public class PlacesActivity
        extends AppCompatActivity
        implements
                AdapterView.OnItemClickListener,
                NetworkRequest.RequestCallback,
                DialogInterface.OnClickListener,
                DialogInterface.OnShowListener,
                View.OnClickListener{

    //Request codes
    private static final int PLACE_PICKER_REQUEST_CODE = 65485;


    private CompassApplication mApplication;

    //Activity UI components
    private ProgressBar mProgress;
    private ListView mList;
    private MenuItem mAdd;

    //Dialog components
    private EditText mName;
    private AlertDialog mNameDialog;
    private boolean mEdition;

    //Adapter and selected item
    private PlacesAdapter mAdapter;
    private UserPlace mCurrentPlace;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        mApplication = (CompassApplication)getApplication();

        //Get and set the toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.places_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mProgress = (ProgressBar)findViewById(R.id.places_progress);
        mList = (ListView)findViewById(R.id.places_list);

        //Create the name dialog. This needs to be done only once
        mName = new EditText(this);
        mName.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mNameDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.places_name_dialog_title)
                .setView(mName)
                .setNegativeButton(R.string.places_name_dialog_cancel, null)
                .setPositiveButton(R.string.places_name_dialog_accept, null)
                .create();
        mNameDialog.setOnShowListener(this);

        //Load the primary places
        NetworkRequest.get(this, this, API.getPrimaryPlacesUrl(), "");
    }

    @Override
    public void onRequestComplete(int requestCode, String result){
        //Parse the result
        List<Place> primaryPlaces = PlaceParser.parsePrimaryPlaces2(result);

        //First off, the progress bar is hidden
        mProgress.setVisibility(View.GONE);

        //If the result of the parser is null, the string was bad, otherwise, proceed
        if (primaryPlaces != null){
            //Two lists are used to sort the list
            List<UserPlace> places = new ArrayList<>();
            List<UserPlace> userPlaces = new ArrayList<>();
            List<UserPlace> currentPlaces = mApplication.getUserData().getPlaces();

            //The user places are added to the list in the appropriate order
            for (UserPlace userPlace:currentPlaces){
                userPlace.getPlace().setSet(true);

                int primaryIndex = -1;
                for (int i = 0; i < primaryPlaces.size(); i++){
                    if (primaryPlaces.get(i).getName().equals(userPlace.getName())){
                        primaryIndex = i;
                        break;
                    }
                }

                //If the place is primary it is added at the head, otherwise it is added at the tail
                if (primaryIndex != -1){
                    userPlace.getPlace().setPrimary(true);
                    places.add(userPlace);
                    //The primary place is removed from the list to keep track of which ones have
                    //  been added already
                    primaryPlaces.remove(primaryIndex);
                }
                else{
                    userPlace.getPlace().setPrimary(false);
                    userPlaces.add(userPlace);
                }
            }
            //The reminder of primary places need to be added to the list as well
            for (Place place:primaryPlaces){
                place.setSet(false);
                places.add(new UserPlace(place, -1, 0, 0));
            }
            //The list of user places are added to the final list
            places.addAll(userPlaces);

            //Finally, set the adapter and enable the add button.
            mAdapter = new PlacesAdapter(this, places);
            mList.setAdapter(mAdapter);
            mList.setOnItemClickListener(this);
            mList.setVisibility(View.VISIBLE);
            mAdd.setEnabled(true);
        }
        else{
            Toast.makeText(this, "Couldn't load the list of places", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestFailed(int requestCode, String message){
        //If the data couldn't be retrieved the user is notified
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_places, menu);
        mAdd = menu.findItem(R.id.places_add);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == R.id.places_add){
            mCurrentPlace = null;
            mEdition = false;
            mName.setText("");
            mNameDialog.show();
            return true;
        }
        return false;
    }

    @Override
    public void onShow(DialogInterface dialog){
        //We need to override the default dismissal effect when a dialog button is clicked.
        //  The solution is to set an onClick listener to the button itself to deprive the
        //  dialog from having the opportunity to dismiss the dialog.
        mNameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onClick(View view){
        String name = mName.getText().toString().trim();

        //If the user didn't input a name, don't let him continue
        if (name.equals("")){
            Toast.makeText(this, R.string.places_name_dialog_empty, Toast.LENGTH_SHORT).show();
        }
        else{
            //Check if the name is already set
            boolean duplicate = false;
            for (UserPlace userPlace:mAdapter.getPlaces()){
                if (userPlace.getName().equals(name)){
                    duplicate = true;
                    break;
                }
            }
            //If the name exists, don't let the user continue
            if (duplicate){
                Toast.makeText(this, R.string.places_name_dialog_chosen, Toast.LENGTH_SHORT).show();
            }
            else{
                //If everything checks out, dismiss the dialog
                mNameDialog.dismiss();
                //If we are editing the name, then save it
                if (mEdition){
                    mCurrentPlace.getPlace().setName(mName.getText().toString().trim());
                    mAdapter.notifyDataSetChanged();
                    NetworkRequest.put(this, null, API.getPostPutPlaceUrl(mCurrentPlace),
                            mApplication.getToken(), API.getPostPutPlaceBody(mCurrentPlace));

                    //Update the place in the database
                    CompassDbHelper dbHelper = new CompassDbHelper(this);
                    dbHelper.updatePlace(mCurrentPlace);
                    dbHelper.close();

                    addPlaceToLocalList(mCurrentPlace);
                }
                //Otherwise this is a new place request, fire the place picker
                else{
                    Place newPlace = new Place(name);
                    newPlace.setName(name);
                    Intent add = new Intent(this, PlacePickerActivity.class);
                    add.putExtra(PlacePickerActivity.PLACE_KEY, newPlace);
                    startActivityForResult(add, PLACE_PICKER_REQUEST_CODE);
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        //Record the selected item
        mCurrentPlace = mAdapter.getItem(position);
        //If the place is primary, the name cannot be edited, so go straight to the picker
        if (mCurrentPlace.isPrimary()){
            Intent add = new Intent(this, PlacePickerActivity.class);
            add.putExtra(PlacePickerActivity.PLACE_KEY, mCurrentPlace);
            startActivityForResult(add, PLACE_PICKER_REQUEST_CODE);
        }
        //Otherwise prompt an option dialog (change name or change location)
        else{
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.places_edit_dialog_title)
                    .setItems(R.array.places_edit_dialog_options, this)
                    .create();
            dialog.show();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which){
        //This onClick responds to events from the option dialog
        //Edit the name
        if (which == 0){
            mEdition = true;
            mName.setText(mCurrentPlace.getName());
            mNameDialog.show();
        }
        //Edit the location
        else if (which == 1){
            Intent add = new Intent(this, PlacePickerActivity.class);
            add.putExtra(PlacePickerActivity.PLACE_KEY, mCurrentPlace);
            startActivityForResult(add, PLACE_PICKER_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK){
            if (requestCode == PLACE_PICKER_REQUEST_CODE){
                UserPlace place = (UserPlace)data.getSerializableExtra(PlacePickerActivity.PLACE_RESULT_KEY);
                Log.d("PlacesActivity", place.toString());
                //If a place wasn't selected this is a new place, so save it.
                if (mCurrentPlace == null){
                    place.getPlace().setName(mName.getText().toString().trim());

                    //Write the place to the database
                    CompassDbHelper dbHelper = new CompassDbHelper(this);
                    dbHelper.savePlace(place);
                    dbHelper.close();

                    mAdapter.addPlace(place);
                }
                //Otherwise, this place was edited, so update both, the place and the adapter
                else{
                    mCurrentPlace.setLatitude(place.getLocation().latitude);
                    mCurrentPlace.setLongitude(place.getLocation().longitude);

                    CompassDbHelper dbHelper = new CompassDbHelper(this);
                    //If the place is primary and not set it won't be in the database, so save
                    if (mCurrentPlace.isPrimary() && !mCurrentPlace.isSet()){
                        dbHelper.savePlace(mCurrentPlace);
                    }
                    //Otherwise it will, so update
                    else{
                        dbHelper.updatePlace(mCurrentPlace);
                    }
                    dbHelper.close();
                    
                    mCurrentPlace.getPlace().setSet(true);
                    mAdapter.notifyDataSetChanged();
                }
                addPlaceToLocalList(place);
            }
        }
    }

    public void addPlaceToLocalList(UserPlace place){
        List<UserPlace> userPlaces = mApplication.getUserData().getPlaces();
        if (!userPlaces.contains(place)){
            userPlaces.add(place);
        }
        else{
            int index = userPlaces.indexOf(place);
            userPlaces.remove(index);
            userPlaces.add(index, place);
        }
    }
}
