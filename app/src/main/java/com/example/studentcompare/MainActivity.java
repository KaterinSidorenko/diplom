package com.example.studentcompare;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.studentcompare.WorkWithDatabase.DatabaseGroupActivity;
import com.example.studentcompare.WorkWithDatabase.LoginActivity;
import com.example.studentcompare.filework.ComparisonActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    FirebaseAuth auth;
    TextView textView;
    Button btnLogOut;
    Button btnCreateInGroup;
    Button btnFileWork;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();
        btnLogOut = findViewById(R.id.logOut);
        textView = findViewById(R.id.userdetails);
        btnCreateInGroup = findViewById(R.id.btnCreateActivity);
        btnFileWork = findViewById(R.id.filework);
        user = auth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText(user.getEmail());
        }
        btnCreateInGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DatabaseGroupActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        btnFileWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ComparisonActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}