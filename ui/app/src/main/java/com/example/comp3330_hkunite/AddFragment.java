package com.example.comp3330_hkunite;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;


public class AddFragment extends Fragment {

    //initializing the variables:
    private Button addButton;
    private Button uploadImageButton;
    private EditText title;
    private EditText location;
    private EditText date;
    private EditText time;
    private EditText description;
    private Switch switchPrivate;




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
        addButton = view.findViewById(R.id.addButton);
        uploadImageButton = view.findViewById(R.id.add_image);
        title = view.findViewById(R.id.editTextTitle);
        location = view.findViewById(R.id.editTextLocation);
        date= view.findViewById(R.id.editTextDate);
        time = view.findViewById(R.id.editTextTime);
        description = view.findViewById(R.id.editTextDescription);
        switchPrivate = view.findViewById(R.id.switchPrivate);



        return inflater.inflate(R.layout.fragment_add, container, false);
    }
}