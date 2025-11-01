package com.example.comp3330_hkunite;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Calendar;


public class AddFragment extends Fragment {

    //initializing the variables:
    private Button addButtonField;
    private Button uploadImageButtonField;
    private EditText titleField;
    private EditText locationField;
    private EditText dateField;
    private EditText timeField;
    private EditText descriptionField;
    private Switch switchPrivateField;
    private ImageView CoverPicture;

    private ActivityResultLauncher<Intent> pickImageLauncher;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // here i should now create the database i think
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);


        //connecting all the buttons to their UI button, use: findViewById(R.id.btn_add)
        addButtonField = view.findViewById(R.id.addButton);
        uploadImageButtonField = view.findViewById(R.id.add_image);
        titleField = view.findViewById(R.id.editTextTitle);
        locationField = view.findViewById(R.id.editTextLocation);
        dateField= view.findViewById(R.id.editTextDate);
        timeField = view.findViewById(R.id.editTextTime);
        descriptionField = view.findViewById(R.id.editTextDescription);
        switchPrivateField = view.findViewById(R.id.switchPrivate);
        CoverPicture = view.findViewById(R.id.picture);
        registerPictureUpload();

        //making the datepicker and timepicker pop up:
        dateField.setOnClickListener(v -> {openDatePicker(dateField);});
        timeField.setOnClickListener(v -> {openTimePicker(timeField);});
        uploadImageButtonField.setOnClickListener(v -> {uploadImage(CoverPicture);});
        //saving all the input information from the textfields:
        view.setOnClickListener(v -> {
            if (v.getId()==R.id.editTextTitle){
                String title = titleField.getText().toString();}
            if (v.getId()==R.id.editTextLocation){
                String location = locationField.getText().toString();}
            if (v.getId()==R.id.editTextDescription){
                String description = descriptionField.getText().toString();}
            if (v.getId()==R.id.switchPrivate){
                Boolean isPrivate = switchPrivateField.isChecked();} //the defauolt is false -> public}



            //for the buttons
            if (v.getId()==R.id.add_image){
                //this needs to be removed and logic implemented
                String title = titleField.getText().toString();}


            //this button is special because then we send it to the database!!
            if (v.getId()==R.id.addButton){
                //remove the stuff below this is just filler
                String test = titleField.getText().toString();}
        });







        return view;
    }


    //helper methods:
    public void openDatePicker(EditText date){

        //we start a calendar (set on current day month and year to prefill)
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
               getContext(), //so it opens the picker on the current fragment
                (view, y, m, d) -> date.setText(d + "/" + (m + 1) + "/" + y),
                year, month, day
        );
        datePicker.show();
    }

    public void openTimePicker(EditText time){
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(
                getContext(),
                (view, h, m) -> time.setText(h+":"+m),
                hour, minute, true
        );
        timePicker.show();
    }

    private void uploadImage(ImageView CoverPicture){
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        pickImageLauncher.launch(intent);
    }
    private void registerPictureUpload(){
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        try {
                            Uri imageUri = result.getData().getData();
                            CoverPicture.setImageURI(imageUri);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "No Image Selected", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
        );
    }
}