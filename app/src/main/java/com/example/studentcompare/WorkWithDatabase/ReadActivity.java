package com.example.studentcompare.WorkWithDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.studentcompare.R;
import com.example.studentcompare.Student;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ReadActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private EditText randomNumberEditTextFrom,randomNumberEditTextBefore;
    private List<String> listData;
    Button btnShair, btnRandNumAgain;
    private DatabaseReference mDataBase;
    private String selectedGroup; // Изменение: используйте выбранную группу

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        // Получение выбранной группы из интента
        Intent intent = getIntent();
        selectedGroup = intent.getStringExtra("selectedGroup");

        btnShair = findViewById(R.id.btnShare);
        btnRandNumAgain = findViewById(R.id.btnRandAgain);
        listView = findViewById(R.id.listView);
        listData = new ArrayList<>();

        // Использование выбранной группы в DatabaseReference
        mDataBase = FirebaseDatabase.getInstance().getReference(selectedGroup);
        adapter = new StudentAdapter(this, listData);
        listView.setAdapter(adapter);
        randomNumberEditTextFrom = findViewById(R.id.randomNumberEditTextFrom);
        randomNumberEditTextBefore = findViewById(R.id.randomNumberEditTextBefore);

        getDataFromDB();
        btnShair = findViewById(R.id.btnShare);
        btnShair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getCount() > 0) {
                    shareData();
                } else {
                    // Выведите сообщение о том, что список пуст
                    Toast.makeText(ReadActivity.this, "Список пуст", Toast.LENGTH_SHORT).show();
                }
            }

        });
        btnRandNumAgain = findViewById(R.id.btnRandAgain); // Исправленная строка
        btnRandNumAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                randomNumberEditTextFrom.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        generateRandomNumbers();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

                randomNumberEditTextBefore.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        generateRandomNumbers();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
                generateRandomNumbers();
            }

        });
    }
    private void shareData() {
        StringBuilder data = new StringBuilder();

        for (int i = 0; i < adapter.getCount(); i++) {
            View itemView = listView.getChildAt(i);
            TextView nameTextView = itemView.findViewById(R.id.nameTextView);
            TextView randomNumberTextView = itemView.findViewById(R.id.randomNumberTextView);

            String fullName = nameTextView.getText().toString();
            String randomNumber = randomNumberTextView.getText().toString();

            data.append(fullName).append(" - ").append(randomNumber).append("\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Рандомные числа");
        intent.putExtra(Intent.EXTRA_TEXT, data.toString());

        startActivity(Intent.createChooser(intent, "Поделиться с помощью"));
    }


    public void generateRandomNumbers() {
        String fromText = randomNumberEditTextFrom.getText().toString();
        String beforeText = randomNumberEditTextBefore.getText().toString();

        if (!fromText.isEmpty() && !beforeText.isEmpty()) {
            try {
                int from = Integer.parseInt(fromText);
                int before = Integer.parseInt(beforeText);

                ((StudentAdapter) adapter).setRandomRange(from, before);
            } catch (NumberFormatException e) {
                // Логируйте ошибку или показывайте сообщение об ошибке
                Log.e("ReadActivity", "Ошибка при конвертации строки в число", e);
                Toast.makeText(this, "Введите корректные числовые значения", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d("ReadActivity", "Одно или оба поля пусты");
        }
    }

    private void getDataFromDB() {
        ValueEventListener vListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (listData.size() > 0) listData.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Student student = ds.getValue(Student.class);
                    assert student != null;
                    String fullName = student.name + " " + student.secname;
                    listData.add(fullName);
                }
                generateRandomNumbers();
                adapter.notifyDataSetChanged(); // Обновляем список после получения данных и установки нового диапазона рандомных чисел
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        mDataBase.addValueEventListener(vListener);
    }

    public void onClickBackGroup(View view) {
        Intent intent = new Intent(getApplicationContext(), DatabaseGroupActivity.class);
        startActivity(intent);
        finish();
    }
}