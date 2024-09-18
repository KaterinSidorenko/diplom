package com.example.studentcompare.WorkWithDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.studentcompare.MainActivity;
import com.example.studentcompare.R;
import com.example.studentcompare.Student;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DatabaseGroupActivity extends AppCompatActivity {

    EditText edName, edSecName, edNewGroup;
    Button btnCreate, btnRead, btnAddGroup;
    Spinner spinnerGroup; //  Spinner для выбора группы
    private DatabaseReference mDataBase;
    private String selectedGroup; // для хранения выбранной группы

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_group);
        edNewGroup = findViewById(R.id.newGroup);
        edName = findViewById(R.id.EtUserName);
        edSecName = findViewById(R.id.EtUserSecondName);
        btnCreate = findViewById(R.id.btnSave);
        btnRead = findViewById(R.id.btnRead);
        btnAddGroup = findViewById(R.id.btnAddGroup);
        spinnerGroup = findViewById(R.id.spinnerGroup); // инициализация Spinner

        mDataBase = FirebaseDatabase.getInstance().getReference();
        loadGroupsFromDatabase(); // Загрузка групп из Firebase

        spinnerGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGroup = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делать
            }
        });

    }

    private void loadGroupsFromDatabase() {
        mDataBase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> groups = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String groupName = snapshot.getKey();
                    Log.d("DatabaseGroup", "Group name: " + groupName);
                    groups.add(groupName);
                }
                updateSpinner(groups.toArray(new String[0]));
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DatabaseGroupActivity.this, "Failed to load groups. Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("DatabaseGroup", "Failed to load groups", databaseError.toException());
            }

        });
    }

    private void updateSpinner(String[] groups) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                groups
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroup.setAdapter(adapter);

        // Выберите последний добавленный элемент в спиннере
        spinnerGroup.setSelection(adapter.getCount() - 1);
    }

    public void onClickSave(View view) {
        String name = edName.getText().toString();
        String secname = edSecName.getText().toString();

        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(secname) && !TextUtils.isEmpty(selectedGroup)) {
            Student newStudent = new Student(null, name, secname, selectedGroup);
            mDataBase.child(selectedGroup).push().setValue(newStudent);
            Toast.makeText(DatabaseGroupActivity.this, "Create!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(DatabaseGroupActivity.this, "One of the fields is empty!", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickRead(View view) {
        Intent intent = new Intent(DatabaseGroupActivity.this, ReadActivity.class);
        intent.putExtra("selectedGroup", selectedGroup); // передача выбранной группы в следующую активность
        startActivity(intent);
        finish();
    }

    public void onClickAddGroup(View view) {
        String newGroupName = edNewGroup.getText().toString();

        if (!TextUtils.isEmpty(newGroupName)) {
            // Добавляем новую группу в Firebase Realtime Database
            mDataBase.child(newGroupName).setValue(true);

            // Добавляем новую группу в arrays.xml
            addGroupToArrayXml(newGroupName);

            Toast.makeText(DatabaseGroupActivity.this, "Group added!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(DatabaseGroupActivity.this, "Group name is empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void addGroupToArrayXml(String newGroupName) {
        // Получаем текущий список групп из arrays.xml
        String[] currentGroups = getResources().getStringArray(R.array.groups_array);

        // Создаем новый массив с увеличенным размером
        String[] newGroups = new String[currentGroups.length + 1];

        // Копируем старые значения в новый массив
        System.arraycopy(currentGroups, 0, newGroups, 0, currentGroups.length);

        // Добавляем новую группу в новый массив
        newGroups[currentGroups.length] = newGroupName;

        // Устанавливаем ссылку на старый массив как null
        currentGroups = null;

        // Обновляем arrays.xml
        updateArrayXml(newGroups);
    }

    private void updateArrayXml(String[] newGroups) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                newGroups
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroup.setAdapter(adapter);

        // Выберите последний добавленный элемент в спиннере
        spinnerGroup.setSelection(adapter.getCount() - 1);
    }


    public void onClickBack(View view) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}