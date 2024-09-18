package com.example.studentcompare.WorkWithDatabase;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.studentcompare.R;

import java.util.List;
import java.util.Random;

public class StudentAdapter extends ArrayAdapter<String> {
    private List<String> mListData;
    private int randomFrom;
    private int randomBefore;

    public StudentAdapter(Context context, List<String> listData) {
        super(context, R.layout.list_item, R.id.nameTextView, listData);
        this.mListData = listData;
    }

    public void setRandomRange(int from, int before) {
        this.randomFrom = from;
        this.randomBefore = before;
        notifyDataSetChanged(); // Обновляем список
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        TextView nameTextView = view.findViewById(R.id.nameTextView);
        TextView randomNumberTextView = view.findViewById(R.id.randomNumberTextView);

        String fullName = mListData.get(position);
        int randomNumber = generateRandomNumber(randomFrom, randomBefore); // Генерация рандомного числа в заданном диапазоне

        nameTextView.setText(fullName);
        randomNumberTextView.setText(String.valueOf(randomNumber));

        return view;
    }

    private int generateRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }
}
