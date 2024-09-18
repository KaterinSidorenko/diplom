package com.example.studentcompare.filework;

import static com.example.studentcompare.filework.FileAdapter.getFileName;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.github.javaparser.ast.Node;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentcompare.MainActivity;
import com.example.studentcompare.R;
import com.example.studentcompare.funktions.FunktionFile;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.github.javaparser.ast.comments.Comment;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComparisonActivity extends AppCompatActivity {

    public static final int REQUEST_PICK_DOC_FILE = 1001;

    private RecyclerView recyclerViewFiles;
    private FileAdapter fileAdapter;
    private List<Uri> selectedFiles;
    private String selectedGroup = null;
    private String selectedFolder = null;
    Button btnBack, btnInstruct, btnAdd, buttonOpenFile;
    private StorageReference storageRef; // Объявите здесь переменную storageRef
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kiactivity);
// Получаем ссылку на WebView из разметки
        WebView webView = findViewById(R.id.webView);

        // Инициализация storageRef
        storageRef = FirebaseStorage.getInstance().getReference();
        Log.d("KIActivity", "onCreate() called");
        btnAdd = findViewById(R.id.btnaddFile); // Находим кнопку Add
        btnBack = findViewById(R.id.btnback);
        btnInstruct = findViewById(R.id.btninstruct);
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles);
        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        selectedFiles = new ArrayList<>();
        fileAdapter = new FileAdapter(this, selectedFiles);
        recyclerViewFiles.setAdapter(fileAdapter);
        buttonOpenFile = findViewById(R.id.buttonOpenFile);

        buttonOpenFile.setOnClickListener(v -> FunktionFile.showLocationDialog(this));

// В методе onCreate после инициализации recyclerView и fileAdapter

        Button btnCompare = findViewById(R.id.btnCompare);
        btnCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Проверяем, что у нас есть хотя бы два выбранных файла для сравнения
                if (selectedFiles.size() < 2) {
                    Toast.makeText(ComparisonActivity.this, "Выберите два файла для сравнения", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Показываем диалоговое окно для выбора двух файлов для сравнения
                showComparisonDialog();
            }
        });
        buttonOpenFile.setOnClickListener(v -> FunktionFile.showLocationDialog(ComparisonActivity.this));

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ComparisonActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // showGroupSelectionDialog();


        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // openFilePicker("КИ");
                showGroupSelectionDialog();

            }
        });


        btnInstruct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inflate the custom layout
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.diallog_recommendation, null);

                // Create the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(ComparisonActivity.this);
                builder.setView(dialogView)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    private void showGroupSelectionDialog() {
        // Получение списка групп из каталога "gs://student-compare.appspot.com"
        storageRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                List<String> groupNames = new ArrayList<>();
                // Получение имен групп и добавление их в список
                for (StorageReference prefix : listResult.getPrefixes()) {
                    groupNames.add(prefix.getName());
                }

                // Конвертация списка имен групп в массив строк
                final CharSequence[] items = groupNames.toArray(new CharSequence[groupNames.size()]);

                // Создание диалогового окна выбора группы
                AlertDialog.Builder builder = new AlertDialog.Builder(ComparisonActivity.this);
                builder.setTitle("Выберите группу")
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Сохранение выбранной группы
                                selectedGroup = items[which].toString();
                                // После выбора группы, показываем диалог для выбора лабораторных
                                //showLabSelectionDialog();
                                showFolderSelectionDialog();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void showFolderSelectionDialog() {
        // Получение списка папок внутри "KI/"
        storageRef.child(selectedGroup).listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                List<String> folderNames = new ArrayList<>();
                // Получение имен папок и добавление их в список
                for (StorageReference prefix : listResult.getPrefixes()) {
                    folderNames.add(prefix.getName());
                }

                // Конвертация списка имен папок в массив строк
                final CharSequence[] items = folderNames.toArray(new CharSequence[folderNames.size()]);

                // Создание диалогового окна выбора папки
                AlertDialog.Builder builder = new AlertDialog.Builder(ComparisonActivity.this);
                builder.setTitle("Выберите папку")
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Сохранение выбранной папки
                                selectedFolder = items[which].toString();
                                // Загрузить файлы из выбранной папки
                                loadFilesFromFolder(selectedFolder);
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void loadFilesFromFolder(String folderName) {
        // Получение списка файлов в выбранной папке
        storageRef.child(selectedGroup + "/" + folderName).listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                // Очищаем предыдущий список
                selectedFiles.clear();
                // Добавляем файлы из выбранной папки в список
                for (StorageReference item : listResult.getItems()) {
                    // Получение URL каждого файла и добавление его в список selectedFiles
                    item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Добавление URL файла в список
                            selectedFiles.add(uri);
                            Log.d("Name", String.valueOf(uri));
                            // Обновление RecyclerView
                            fileAdapter.notifyDataSetChanged();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Обработка ошибки при получении URL файла
                            Log.e("KIActivity", "Failed to get download URL for file", e);
                        }
                    });
                }
            }
        });
    }

    //ПРОГРЕСС БАР
    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.progress_dialog, null);
        builder.setView(dialogView);
        progressDialog = builder.create();
        progressDialog.setCancelable(false); // Запретить закрытие диалога при нажатии вне области диалога
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    //СРАВНЕНИЕ ФАЙЛОВ

    private void showComparisonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите файлы для сравнения");

        String[] fileList = new String[selectedFiles.size()];
        boolean[] checkedItems = new boolean[selectedFiles.size()]; // Массив для отслеживания состояния выбора файлов

        // Заполняем список файлов
        for (int i = 0; i < selectedFiles.size(); i++) {
            fileList[i] = getFileName(selectedFiles.get(i));
            checkedItems[i] = false; // По умолчанию ничего не выбрано
        }

        // Передаем пустой список, куда будем добавлять выбранные файлы
        List<Uri> selectedFilesList = new ArrayList<>();

        // Флаг для отслеживания состояния "Выбрать все"
        final boolean[] allSelected = {false};

        // Создаем чекбокс "Выбрать все"
        builder.setNeutralButton("Выбрать все", null); // null, чтобы не закрывалось окно при нажатии

        // Устанавливаем множественный выбор элементов списка
        builder.setMultiChoiceItems(fileList, checkedItems, (dialog, which, isChecked) -> {
            // Обновляем состояние выбранных файлов
            if (isChecked) {
                selectedFilesList.add(selectedFiles.get(which));
            } else {
                selectedFilesList.remove(selectedFiles.get(which));
            }
        });

        builder.setPositiveButton("Сравнить", (dialog, which) -> {
            // Выполняем сравнение только если выбраны хотя бы два файла
            if (selectedFilesList.size() >= 2) {
                downloadFiles(selectedFilesList);
            } else {
                Toast.makeText(this, "Выберите хотя бы два файла для сравнения", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            // Добавляем обработчик для кнопки "Выбрать все"
            Button selectAllButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            selectAllButton.setOnClickListener(view -> {
                // Переключаем состояние "Выбрать все"
                allSelected[0] = !allSelected[0];
                for (int i = 0; i < checkedItems.length; i++) {
                    checkedItems[i] = allSelected[0];
                    if (allSelected[0]) {
                        if (!selectedFilesList.contains(selectedFiles.get(i))) {
                            selectedFilesList.add(selectedFiles.get(i)); // Добавляем файлы в список выбранных, если их там еще нет
                        }
                    } else {
                        selectedFilesList.remove(selectedFiles.get(i)); // Убираем файлы из списка выбранных
                    }
                }
                // Обновляем состояние элементов списка
                ListView listView = ((AlertDialog) dialog).getListView();
                for (int i = 0; i < checkedItems.length; i++) {
                    listView.setItemChecked(i, allSelected[0]);
                }
            });
        });

        dialog.show();
    }


    private void downloadFiles(List<Uri> selectedFilesList) {
        // Создаем временные файлы для сравнения
        List<File> tempFiles = new ArrayList<>();
        for (Uri fileUri : selectedFilesList) {
            String fileName = getFileName(fileUri); // Получаем имя основного файла
            File tempFile = new File(getCacheDir(), fileName); // Используем имя основного файла для временного файла
            tempFiles.add(tempFile);
        }

        // Загружаем выбранные файлы с хранилища Firebase
        for (int i = 0; i < selectedFilesList.size(); i++) {
            Uri fileUri = selectedFilesList.get(i);
            File tempFile = tempFiles.get(i);

            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUri.toString());
            int finalI = i;
            storageRef.getFile(tempFile).addOnSuccessListener(taskSnapshot -> {
                // Если все файлы загружены успешно, выполняем сравнение
                if (finalI == selectedFilesList.size() - 1) {
                    performComparison(selectedFilesList, tempFiles);
                }
            }).addOnFailureListener(exception -> {
                // В случае ошибки загрузки файла, выводим сообщение об ошибке
                Log.e("Download File Error", "Failed to download file", exception);
                Toast.makeText(this, "Ошибка загрузки файла", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void performComparison(List<Uri> selectedFilesList, List<File> tempFiles) {
        // Показываем диалог выбора типа сравнения
        showComparisonTypeDialog(selectedFilesList, tempFiles);
    }

    private void showComparisonTypeDialog(List<Uri> selectedFilesList, List<File> tempFiles) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите тип сравнения");
        builder.setItems(new CharSequence[]{"Без переменных", "По методам", "Полное сходство", "Объединенное сравнение"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    compareWithoutVariables(selectedFilesList, tempFiles);
                    break;
                case 1:
                    compareByMethods(selectedFilesList, tempFiles);
                    break;
                case 2:
                    compareFullSimilarity(selectedFilesList, tempFiles);
                    break;
                case 3:
                    compareFiles(selectedFilesList, tempFiles);
                    break;
            }
        });
        builder.show();
    }

    private void compareFiles(List<Uri> selectedFilesList, List<File> tempFiles) {
        showProgressDialog();

        new Thread(() -> {
            try {
                List<String> fileContents = new ArrayList<>();
                Map<String, List<MethodDeclaration>> fileMethods = new HashMap<>();
                List<ComparisonResult> noVariableComparisonResults = new ArrayList<>();
                Map<String, Map<MethodDeclaration, MethodDeclaration>> similarMethodsMapping = new HashMap<>();

                // Читаем содержимое файлов и создаем AST для методов
                for (File tempFile : tempFiles) {
                    String content = readWordFileContent(Uri.fromFile(tempFile));
                    fileContents.add(content);

                    CompilationUnit cu = StaticJavaParser.parse(content);
                    List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

                    // Удаляем комментарии из методов
                    for (MethodDeclaration method : methods) {
                        method.getAllContainedComments().forEach(Comment::remove);
                    }

                    fileMethods.put(tempFile.getName(), methods);
                }
                boolean allEqual = true;
// Проверка полного сходства
                List<String> identicalFiles = new ArrayList<>();
// Создаем список для хранения различных файлов
                List<String> differentFiles = new ArrayList<>();

                for (int i = 0; i < fileContents.size(); i++) {
                    boolean isUnique = true; // Переменная, чтобы проверить, является ли файл уникальным
                    for (int j = 0; j < fileContents.size(); j++) {
                        if (i != j) { // Не сравниваем файл с самим собой
                            String content1 = delComment(fileContents.get(i));
                            String content2 = delComment(fileContents.get(j));
                            if (isDifferenceInOneLine(content1, content2)) {
                                identicalFiles.add(getFileName(selectedFilesList.get(i)));//+ " и " + getFileName(selectedFilesList.get(j))
                                isUnique = false; // Файл не уникальный
                                break; // Прекращаем проверку для этого файла, так как он уже идентичен другому файлу
                            }
                        }
                    }
                    if (isUnique) {
                        // Если файл уникальный, добавляем его в список различных файлов
                        differentFiles.add(getFileName(selectedFilesList.get(i)));
                        allEqual = false;
                    }
                }


                // Если строки одинаковы, то считаем файлы идентичными
                int identicalFileCount = allEqual ? fileContents.size() : 0;
                double fullComparisonPercentage = (identicalFileCount / (double) tempFiles.size()) * 100;

                // Проверка без переменных
                for (int i = 0; i < fileContents.size(); i++) {
                    for (int j = i + 1; j < fileContents.size(); j++) {
                        ComparisonResult comparisonResult = compareTextIgnoringVariables(fileContents.get(i), fileContents.get(j), tempFiles.get(i), tempFiles.get(j));
                        noVariableComparisonResults.add(comparisonResult);
                    }
                }

                int identicalFilesCountNoVars = (int) noVariableComparisonResults.stream().filter(result -> result.isEqual).count();
                double noVariableComparisonPercentage = (identicalFilesCountNoVars / (double) tempFiles.size()) * 100;

                // Проверка по методам
                Set<MethodDeclaration> similarMethods = new HashSet<>();
                Set<MethodDeclaration> uniqueMethods = new HashSet<>();
                similarMethodsMapping = new HashMap<>();

                for (Map.Entry<String, List<MethodDeclaration>> entry : fileMethods.entrySet()) {
                    String fileName = entry.getKey();
                    List<MethodDeclaration> methods = entry.getValue();

                    for (MethodDeclaration method : methods) {
                        if (!uniqueMethods.add(method)) {
                            similarMethods.add(method);
                            for (Map.Entry<String, List<MethodDeclaration>> innerEntry : fileMethods.entrySet()) {
                                if (!innerEntry.getKey().equals(fileName)) {
                                    for (MethodDeclaration innerMethod : innerEntry.getValue()) {
                                        if (areMethodsSimilar(innerMethod, method)) {
                                            similarMethodsMapping
                                                    .computeIfAbsent(fileName, k -> new HashMap<>())
                                                    .put(method, innerMethod);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Подсчет уникальных и схожих файлов
                List<String> uniqueFilesMethods = new ArrayList<>();
                List<String> similarFilesMethods = new ArrayList<>();
                int similarFilesCount = 0;
                int totalFiles = fileMethods.size();

                for (Map.Entry<String, List<MethodDeclaration>> entry : fileMethods.entrySet()) {
                    String fileName = entry.getKey();
                    boolean isUnique = true;
                    for (MethodDeclaration method : entry.getValue()) {
                        if (similarMethods.contains(method)) {
                            if (!similarFilesMethods.contains(fileName)) {
                                similarFilesMethods.add(fileName);
                            }
                            isUnique = false;
                        }
                    }
                    if (isUnique && !uniqueFilesMethods.contains(fileName)) {
                        uniqueFilesMethods.add(fileName);
                    }
                }

                similarFilesCount = similarFilesMethods.size();
                double methodSimilarityPercentage = (similarFilesCount / (double) totalFiles) * 100;

                // Определяем лучший метод сравнения
                String bestComparisonMethod;
                double highestPercentage = Math.max(fullComparisonPercentage, Math.max(noVariableComparisonPercentage, methodSimilarityPercentage));
                if (highestPercentage == fullComparisonPercentage) {
                    bestComparisonMethod = "Полное сравнение";
                } else if (highestPercentage == noVariableComparisonPercentage) {
                    bestComparisonMethod = "Сравнение без переменных";
                } else {
                    bestComparisonMethod = "Сравнение по методам";
                }

                // Сбор уникальных и схожих файлов для каждого метода сравнения
                List<String> uniqueFilesFull = new ArrayList<>();
                List<String> similarFilesFull = new ArrayList<>();
                if (allEqual) {
                    for (File tempFile : tempFiles) {
                        similarFilesFull.add(tempFile.getName());
                    }
                } else {
                    for (File tempFile : tempFiles) {
                        uniqueFilesFull.add(tempFile.getName());
                    }
                }

                List<String> uniqueFilesNoVars = new ArrayList<>();
                List<String> similarFilesNoVars = new ArrayList<>();

// Добавление всех уникальных файлов в список
                for (ComparisonResult result : noVariableComparisonResults) {
                    if (!similarFilesNoVars.contains(result.file1.getName()) && !similarFilesNoVars.contains(result.file2.getName())) {
                        if (!uniqueFilesNoVars.contains(result.file1.getName())) {
                            uniqueFilesNoVars.add(result.file1.getName());
                        }
                        if (!uniqueFilesNoVars.contains(result.file2.getName())) {
                            uniqueFilesNoVars.add(result.file2.getName());
                        }
                    }
                }

// Вывод уникальных файлов в логи
                System.out.println("Уникальные файлы:");
                for (String uniqueFile : uniqueFilesNoVars) {
                    System.out.println(uniqueFile);
                    Log.e("Unique", uniqueFile);
                }


// Перенос совпадающих файлов в список схожих
                for (ComparisonResult result : noVariableComparisonResults) {
                    if (result.isEqual) {
                        if (!similarFilesNoVars.contains(result.file1.getName())) {
                            similarFilesNoVars.add(result.file1.getName());
                        }
                        if (!similarFilesNoVars.contains(result.file2.getName())) {
                            similarFilesNoVars.add(result.file2.getName());
                        }
                    }
                }

// Вывод схожих файлов в логи
                System.out.println("Схожие файлы:");
                for (String similarFile : similarFilesNoVars) {
                    System.out.println(similarFile);
                }

// Удаление совпадающих файлов из списка уникальных файлов
                uniqueFilesNoVars.removeAll(similarFilesNoVars);


                // Отправка результатов в UI поток
                Map<String, Map<MethodDeclaration, MethodDeclaration>> finalSimilarMethodsMapping = similarMethodsMapping;
                runOnUiThread(() -> {
                    hideProgressDialog();
                    showComparisonResults(
                            fileMethods, similarMethods, noVariableComparisonResults,
                            differentFiles, identicalFiles,
                            uniqueFilesNoVars, similarFilesNoVars,
                            uniqueFilesMethods, similarFilesMethods,
                            finalSimilarMethodsMapping, identicalFileCount,
                            identicalFilesCountNoVars, tempFiles.size(),
                            fullComparisonPercentage, noVariableComparisonPercentage,
                            methodSimilarityPercentage, bestComparisonMethod
                    );
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Ошибка при сравнении файлов", Toast.LENGTH_SHORT).show();
                    Log.e("Compare Files Error", "Exception: " + e.getMessage());
                });
            }
        }).start();
    }


    private void showComparisonResults(
            Map<String, List<MethodDeclaration>> fileMethods, Set<MethodDeclaration> similarMethods,
            List<ComparisonResult> comparisonResults, List<String> uniqueFilesFull, List<String> similarFilesFull,
            List<String> uniqueFilesNoVars, List<String> similarFilesNoVars,
            List<String> uniqueFilesMethods, List<String> similarFilesMethods,
            Map<String, Map<MethodDeclaration, MethodDeclaration>> similarMethodsMapping, int identicalFileCount,
            int identicalFilesCountNoVars, int totalFilesCount, double fullComparisonPercentage,
            double noVariableComparisonPercentage, double methodSimilarityPercentage, String bestComparisonMethod) {

        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);

        StringBuilder resultHtmlBuilder = new StringBuilder();
        resultHtmlBuilder.append("<html><head><style type=\"text/css\">");
        resultHtmlBuilder.append(".similar-method-name { color: red; font-weight: bold; }");
        resultHtmlBuilder.append(".deleted-variable { color: red; font-weight: bold; }");
        resultHtmlBuilder.append(".line-number { color: blue; }");
        resultHtmlBuilder.append(".method-body.with-differences { background-color: yellow; }");
        resultHtmlBuilder.append(".file-divider { border-top: 2px solid black; margin-top: 20px; padding-top: 20px; }");
        resultHtmlBuilder.append(".detail { display: none; }");
        resultHtmlBuilder.append("</style>");
        resultHtmlBuilder.append("<script type=\"text/javascript\">");
        resultHtmlBuilder.append("function toggleDetails(id) {");
        resultHtmlBuilder.append("var detail = document.getElementById(id);");
        resultHtmlBuilder.append("if (detail.style.display === 'none') {");
        resultHtmlBuilder.append("detail.style.display = 'block';");
        resultHtmlBuilder.append("} else {");
        resultHtmlBuilder.append("detail.style.display = 'none';");
        resultHtmlBuilder.append("}");
        resultHtmlBuilder.append("}");
        resultHtmlBuilder.append("</script>");
        resultHtmlBuilder.append("</head><body>");

        resultHtmlBuilder.append("<h1>Результаты сравнения</h1>");
        resultHtmlBuilder.append("<p>Общее количество работ: ").append(totalFilesCount).append("</p>");
        resultHtmlBuilder.append("<p>Процент полного сходства: ").append(fullComparisonPercentage).append("%</p>");
        resultHtmlBuilder.append("<p>Процент сходства без переменных: ").append(noVariableComparisonPercentage).append("%</p>");
        resultHtmlBuilder.append("<p>Процент сходства по методам: ").append(methodSimilarityPercentage).append("%</p>");
        resultHtmlBuilder.append("<p>Наиболее подходящий метод сравнения: <b>").append(bestComparisonMethod).append("</b></p>");

        resultHtmlBuilder.append("<h2>Полное сравнение</h2>");
        resultHtmlBuilder.append("<h3>Уникальные файлы</h3>");
        resultHtmlBuilder.append("<ul>");
        for (String fileName : uniqueFilesFull) {
            resultHtmlBuilder.append("<li>").append(fileName).append("</li>");
        }
        resultHtmlBuilder.append("</ul>");
        resultHtmlBuilder.append("<h3>Схожие файлы</h3>");
        resultHtmlBuilder.append("<ul>");
        for (String fileName : similarFilesFull) {
            resultHtmlBuilder.append("<li>").append(fileName).append("</li>");
        }
        resultHtmlBuilder.append("</ul>");

        resultHtmlBuilder.append("<h2>Сравнение без переменных</h2>");
        resultHtmlBuilder.append("<h3>Уникальные файлы</h3>");
        resultHtmlBuilder.append("<ul>");
        for (String fileName : uniqueFilesNoVars) {
            resultHtmlBuilder.append("<li>").append(fileName).append("</li>");
        }
        resultHtmlBuilder.append("</ul>");
        resultHtmlBuilder.append("<h3>Схожие файлы</h3>");
        resultHtmlBuilder.append("<ul>");
        for (String fileName : similarFilesNoVars) {
            resultHtmlBuilder.append("<li>").append(fileName).append("</li>");
        }
        resultHtmlBuilder.append("</ul>");


        // Раздел для схожих работ
        resultHtmlBuilder.append("<h2>Схожие работы</h2>");
        if (similarFilesNoVars.isEmpty()) {
            resultHtmlBuilder.append("<p>Нет схожих работ</p>");
        } else {
            for (int i = 0; i < comparisonResults.size(); i++) {
                ComparisonResult comparisonResult = comparisonResults.get(i);
                File file1 = comparisonResult.file1;
                File file2 = comparisonResult.file2;
                String file1Name = file1.getName();
                String file2Name = file2.getName();

                if (!comparisonResult.isEqual) {
                    continue;
                }

                resultHtmlBuilder.append("<div>");
                resultHtmlBuilder.append("<p>").append(file1Name).append(" и ").append(file2Name).append("</p>");
                resultHtmlBuilder.append("<button onclick=\"toggleDetails('detail").append(i).append("')\">Подробнее</button>");
                resultHtmlBuilder.append("<div id='detail").append(i).append("' class='detail'>");

                resultHtmlBuilder.append("<h3>").append(file1Name).append(" vs ").append(file2Name).append("</h3>");
                resultHtmlBuilder.append("<pre style=\"padding-left: 20px;\">");
                try {
                    String file1Content = readWordFileContent(Uri.fromFile(file1));
                    String[] lines1 = file1Content.split("\n");
                    for (int j = 0; j < lines1.length; j++) {
                        resultHtmlBuilder.append("<span class=\"line-number\">").append(j + 1).append("</span>");
                        resultHtmlBuilder.append(" ").append(lines1[j]).append("<br>");
                    }
                } catch (IOException e) {
                    resultHtmlBuilder.append("Ошибка чтения файла: ").append(e.getMessage());
                    e.printStackTrace();
                }
                resultHtmlBuilder.append("</pre>");

                resultHtmlBuilder.append("<pre style=\"padding-left: 20px;\">");
                try {
                    String file2Content = readWordFileContent(Uri.fromFile(file2));
                    String[] lines2 = file2Content.split("\n");
                    for (int j = 0; j < lines2.length; j++) {
                        resultHtmlBuilder.append("<span class=\"line-number\">").append(j + 1).append("</span>");
                        resultHtmlBuilder.append(" ").append(lines2[j]).append("<br>");
                    }
                } catch (IOException e) {
                    resultHtmlBuilder.append("Ошибка чтения файла: ").append(e.getMessage());
                    e.printStackTrace();
                }
                resultHtmlBuilder.append("</pre>");

                resultHtmlBuilder.append("</div>"); // Закрываем detail div
                resultHtmlBuilder.append("</div>"); // Закрываем main div
            }
        }

        // Раздел для детального отображения различий по строкам
        for (int i = 0; i < comparisonResults.size(); i++) {
            ComparisonResult comparisonResult = comparisonResults.get(i);
            File file1 = comparisonResult.file1;
            File file2 = comparisonResult.file2;
            String file1Name = file1.getName();
            String file2Name = file2.getName();

            if (!comparisonResult.isEqual) {
                continue;
            }

            resultHtmlBuilder.append("<div>");
            resultHtmlBuilder.append("<p>").append(file1Name).append(" и ").append(file2Name).append("</p>");
            resultHtmlBuilder.append("<button onclick=\"toggleDetails('detail").append(i).append("')\">Подробнее</button>");
            resultHtmlBuilder.append("<div id='detail").append(i).append("' class='detail'>");

            try {
                String file1Content = readWordFileContent(Uri.fromFile(file1));
                String file2Content = readWordFileContent(Uri.fromFile(file2));
                String[] lines1 = file1Content.split("\n");
                String[] lines2 = file2Content.split("\n");

                resultHtmlBuilder.append("<pre style=\"padding-left: 20px;\">");
                for (int j = 0; j < Math.max(lines1.length, lines2.length); j++) {
                    String line1 = (j < lines1.length) ? lines1[j] : "";
                    String line2 = (j < lines2.length) ? lines2[j] : "";
                    resultHtmlBuilder.append("<span class=\"line-number\">").append(j + 1).append("</span> ");
                    if (line1.equals(line2)) {
                        resultHtmlBuilder.append(line1);
                    } else {
                        resultHtmlBuilder.append("<span class=\"deleted-variable\">").append(line1).append("</span>");
                        resultHtmlBuilder.append("<span class=\"deleted-variable\">").append(line2).append("</span>");
                    }
                    resultHtmlBuilder.append("<br>");
                }
                resultHtmlBuilder.append("</pre>");
            } catch (IOException e) {
                resultHtmlBuilder.append("Ошибка чтения файла: ").append(e.getMessage());
                e.printStackTrace();
            }

            resultHtmlBuilder.append("</div>"); // Закрываем detail div
            resultHtmlBuilder.append("</div>"); // Закрываем main div
        }


        resultHtmlBuilder.append("<h2>Сравнение по методам</h2>");
        resultHtmlBuilder.append("<h3>Уникальные файлы</h3>");
        resultHtmlBuilder.append("<ul>");
        for (String fileName : uniqueFilesMethods) {
            resultHtmlBuilder.append("<li>").append(fileName).append("</li>");
        }
        resultHtmlBuilder.append("</ul>");
        resultHtmlBuilder.append("<h3>Схожие файлы</h3>");
        resultHtmlBuilder.append("<ul>");
        for (String fileName : similarFilesMethods) {
            resultHtmlBuilder.append("<li>").append(fileName).append("</li>");
        }
        resultHtmlBuilder.append("</ul>");

        resultHtmlBuilder.append("<h2>Детальные различия методов</h2>");
        for (Map.Entry<String, Map<MethodDeclaration, MethodDeclaration>> entry : similarMethodsMapping.entrySet()) {
            String fileName1 = entry.getKey();
            Map<MethodDeclaration, MethodDeclaration> methodsMap = entry.getValue();

            resultHtmlBuilder.append("<h3>").append("<b>").append(fileName1).append("</b>").append("</h3>");

            for (Map.Entry<MethodDeclaration, MethodDeclaration> methodEntry : methodsMap.entrySet()) {
                MethodDeclaration method1 = methodEntry.getKey();
                MethodDeclaration method2 = methodEntry.getValue();

                String fileName2 = "";

                // Находим имя файла для метода method2, исключая файл с тем же именем, что и fileName1
                for (Map.Entry<String, List<MethodDeclaration>> fileEntry : fileMethods.entrySet()) {
                    String currentFileName = fileEntry.getKey();
                    if (!currentFileName.equals(fileName1) && fileEntry.getValue().contains(method2)) {
                        fileName2 = currentFileName;
                        break;
                    }
                }

                resultHtmlBuilder.append("<p>").append("В работе ").append(fileName1).append(" метод ").append(method1.getName()).append(" похож на метод ").append(method2.getName()).append(" в работе ").append(fileName2).append("</p>");
                resultHtmlBuilder.append("<button onclick=\"toggleDetails('").append(fileName1).append(method1.getName()).append("')\">Показать различия</button>");
                resultHtmlBuilder.append("<div id='").append(fileName1).append(method1.getName()).append("' class='detail'>");

                String[] lines1 = method1.getBody().toString().split("\n");
                String[] lines2 = method2.getBody().toString().split("\n");

                resultHtmlBuilder.append("<pre>");
                int maxLines = Math.max(lines1.length, lines2.length);
                for (int i = 0; i < maxLines; i++) {
                    String line1 = i < lines1.length ? lines1[i] : "";
                    String line2 = i < lines2.length ? lines2[i] : "";

                    if (!line1.equals(line2)) {
                        resultHtmlBuilder.append("<span class='deleted-variable'>").append(line1).append("</span>\n");
                        resultHtmlBuilder.append("<span class='deleted-variable'>").append(line2).append("</span>\n");
                    } else {
                        resultHtmlBuilder.append(line1).append("\n");
                    }
                }
                resultHtmlBuilder.append("</pre>");
                resultHtmlBuilder.append("</div>");
            }

            // Разделитель между файлами
            resultHtmlBuilder.append("<hr class=\"file-divider\">");
        }

        resultHtmlBuilder.append("</body></html>");

        webView.loadDataWithBaseURL(null, resultHtmlBuilder.toString(), "text/html", "UTF-8", null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Результаты сравнения");
        builder.setView(webView);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private String delComment(String text) {
        // Паттерн для удаления комментариев (однострочных и многострочных)
        Pattern commentPattern = Pattern.compile(
                "(?<!:)\\/\\/.*|\\/\\*(?:.|\\R)*?\\*\\/"
        );
        // Удаление комментариев
        Matcher commentMatcher = commentPattern.matcher(text);
        return commentMatcher.replaceAll("");
    }


    //ПОЛНОЕ СРАВНЕНИЕ НАЧАЛО
    private void compareFullSimilarity(List<Uri> selectedFilesList, List<File> tempFiles) {
        // Показываем прогресс-бар перед началом сравнения
        showProgressDialog();

        new Thread(() -> {
            try {
                List<String> fileContents = new ArrayList<>();
                // Читаем содержимое каждого временного файла
                for (File tempFile : tempFiles) {
                    fileContents.add(readWordFileContent(Uri.fromFile(tempFile)));
                }

                // Проверяем, все ли файлы загружены и их содержимое прочитано
                if (fileContents.size() == selectedFilesList.size()) {
                    // Создаем список для хранения идентичных файлов
                    List<String> identicalFiles = new ArrayList<>();
                    // Создаем список для хранения различных файлов
                    List<String> differentFiles = new ArrayList<>();

                    // Проверяем и сравниваем содержимое файлов
                    for (int i = 0; i < fileContents.size(); i++) {
                        for (int j = i + 1; j < fileContents.size(); j++) {
                            String content1 = delComment(fileContents.get(i));
                            String content2 = delComment(fileContents.get(j));
                            if (isDifferenceInOneLine(content1, content2)) {
                                identicalFiles.add(getFileName(selectedFilesList.get(i)) + " и " + getFileName(selectedFilesList.get(j)));
                            } else {
                                differentFiles.add(getFileName(selectedFilesList.get(i)) + " и " + getFileName(selectedFilesList.get(j)));
                            }
                        }
                    }

                    // Скрываем прогресс-бар после завершения сравнения
                    runOnUiThread(this::hideProgressDialog);

                    // Выводим результат сравнения
                    runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Результаты сравнения файлов");

                        // Создаем строку из списка результатов сравнения
                        StringBuilder resultStringBuilder = new StringBuilder();

                        // Добавляем идентичные файлы
                        if (!identicalFiles.isEmpty()) {
                            resultStringBuilder.append("Следующие файлы идентичны:\n");
                            for (String fileName : identicalFiles) {
                                resultStringBuilder.append("- ").append(fileName).append("\n");
                            }
                            resultStringBuilder.append("\n");
                        }

                        // Добавляем различные файлы
                        if (!differentFiles.isEmpty()) {
                            resultStringBuilder.append("Следующие файлы различны:\n");
                            for (String fileName : differentFiles) {
                                resultStringBuilder.append("\n");
                                resultStringBuilder.append("- ").append(fileName).append("\n");
                            }
                        }

                        builder.setMessage(resultStringBuilder.toString());
                        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    });
                } else {
                    // Если не все файлы загружены или содержимое не прочитано, выводим сообщение об ошибке
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(this, "Ошибка чтения файлов", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                // В случае ошибки чтения файла, выводим сообщение об ошибке
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
                    Log.e("Compare Files Error", "IOException: " + e.getMessage());
                });
            }
        }).start();
    }


    private boolean isDifferenceInOneLine(String content1, String content2) {
        String[] lines1 = content1.split("\\r?\\n");
        String[] lines2 = content2.split("\\r?\\n");

        if (lines1.length != lines2.length) {
            return false; // Различие в количестве строк
        }

        int differences = 0;
        for (int i = 0; i < lines1.length; i++) {
            if (!lines1[i].equals(lines2[i])) {
                differences++;
            }
        }

        return differences <= 1;
    }
    //ПОЛНОЕ СРАВНЕНИЕ КОНЕЦ


    //СРАВНЕНИЕ БЕЗ ПЕРЕМЕННЫХ НАЧАЛО
    private void compareWithoutVariables(List<Uri> selectedFilesList, List<File> tempFiles) {
        // Показываем прогресс-бар перед началом сравнения
        showProgressDialog();

        new Thread(() -> {
            try {
                List<String> fileContents = new ArrayList<>();
                List<ComparisonResult> comparisonResults = new ArrayList<>(); // Хранение результатов сравнения

                // Читаем содержимое каждого временного файла и сохраняем его
                for (File tempFile : tempFiles) {
                    fileContents.add(readWordFileContent(Uri.fromFile(tempFile)));
                }

                // Проверяем, все ли файлы загружены и их содержимое прочитано
                if (fileContents.size() == selectedFilesList.size()) {
                    // Проверяем и сравниваем содержимое файлов
                    for (int i = 0; i < fileContents.size(); i++) {
                        for (int j = i + 1; j < fileContents.size(); j++) {
                            // Сравниваем текст, игнорируя переменные, для каждой пары файлов
                            ComparisonResult comparisonResult = compareTextIgnoringVariables(fileContents.get(i), fileContents.get(j), tempFiles.get(i), tempFiles.get(j));
                            comparisonResults.add(comparisonResult); // Добавляем результат сравнения в список
                        }
                    }

                    // Скрываем прогресс-бар после завершения сравнения
                    runOnUiThread(this::hideProgressDialog);

                    // Выводим результаты сравнения
                    // Передаем результаты сравнения в метод отображения результатов
                    runOnUiThread(() -> showComparisonResultsWebView(comparisonResults, tempFiles));
                } else {
                    // Если не все файлы загружены или содержимое не прочитано, выводим сообщение об ошибке
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(this, "Ошибка чтения файлов", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                // В случае ошибки чтения файла, выводим сообщение об ошибке
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
                    Log.e("Compare Files Error", "IOException: " + e.getMessage());
                });
            }
        }).start();
    }


    private ComparisonResult compareTextIgnoringVariables(String text1, String text2, File file1, File file2) {
        // Удалить переменные из текста
        text1 = removeVariables(text1);
        text2 = removeVariables(text2);

        // Сравнить текст без переменных
        List<String> differences = findDifferences(text1, text2);

        // Проверить, есть ли различия
        boolean isEqual = differences.size() < 2;

        // Вернуть результат сравнения и список различий
        return new ComparisonResult(file1, file2, isEqual, differences);
    }


    //СРАВНЕНИЕ БЕЗ ПЕРЕМЕННЫХ КОНЕЦ
    private void showComparisonResultsWebView(List<ComparisonResult> comparisonResults, List<File> tempFiles) {
        Toast.makeText(this, "Подождите немного данные обрабатываются", Toast.LENGTH_SHORT).show();
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);

        // Создание AsyncTask для загрузки содержимого WebView в фоновом потоке
        class LoadWebViewTask extends AsyncTask<Void, Void, String> {
            @Override
            protected String doInBackground(Void... voids) {
                // Создание HTML содержимого здесь
                StringBuilder resultHtmlBuilder = new StringBuilder();
                resultHtmlBuilder.append("<html><head><style type=\"text/css\">");
                resultHtmlBuilder.append(".deleted-variable { color: red; }");
                resultHtmlBuilder.append(".method-body.with-differences { background-color: yellow; }");
                resultHtmlBuilder.append(".line-number { color: blue; }");
                resultHtmlBuilder.append(".detail { display: none; }"); // Скрываем детали по умолчанию
                resultHtmlBuilder.append("</style>");
                resultHtmlBuilder.append("<script type=\"text/javascript\">");
                resultHtmlBuilder.append("function toggleDetails(id) {");
                resultHtmlBuilder.append("var detail = document.getElementById(id);");
                resultHtmlBuilder.append("if (detail.style.display === 'none') {");
                resultHtmlBuilder.append("detail.style.display = 'block';");
                resultHtmlBuilder.append("} else {");
                resultHtmlBuilder.append("detail.style.display = 'none';");
                resultHtmlBuilder.append("}");
                resultHtmlBuilder.append("}");
                resultHtmlBuilder.append("</script>");
                resultHtmlBuilder.append("</head><body>");

                // Создание множеств для уникальных и схожих файлов
                Set<File> uniqueFiles = new HashSet<>(tempFiles);
                Set<File> similarFiles = new HashSet<>();

                // Анализ результатов сравнения
                for (ComparisonResult comparisonResult : comparisonResults) {
                    if (comparisonResult.isEqual) {
                        uniqueFiles.remove(comparisonResult.file1);
                        uniqueFiles.remove(comparisonResult.file2);
                        similarFiles.add(comparisonResult.file1);
                        similarFiles.add(comparisonResult.file2);
                    }
                }
// Расчет процента схожести
                double similarityPercentage = (double) similarFiles.size() / tempFiles.size() * 100;


                // Создание сводки
                resultHtmlBuilder.append("<h2>Сводка</h2>");
                resultHtmlBuilder.append("<p>Процент схожести: <span id=\"progressText\">" + similarityPercentage + "%</span></p>");
                resultHtmlBuilder.append("<p>Количество сравниваемых работ: ").append(tempFiles.size()).append("</p>");
                resultHtmlBuilder.append("<p>Количество уникальных работ: ").append(uniqueFiles.size()).append("</p>");
                resultHtmlBuilder.append("<p>Количество схожих работ: ").append(similarFiles.size()).append("</p>");

                // Раздел для уникальных работ
                resultHtmlBuilder.append("<h2>Уникальные работы</h2>");
                if (uniqueFiles.isEmpty()) {
                    resultHtmlBuilder.append("<p>Нет уникальных работ</p>");
                } else {
                    for (File uniqueFile : uniqueFiles) {
                        resultHtmlBuilder.append("<p>").append(uniqueFile.getName()).append("</p>");
                    }
                }

                // Раздел для схожих работ
                resultHtmlBuilder.append("<h2>Схожие работы</h2>");
                if (similarFiles.isEmpty()) {
                    resultHtmlBuilder.append("<p>Нет схожих работ</p>");
                } else {
                    for (int i = 0; i < comparisonResults.size(); i++) {
                        ComparisonResult comparisonResult = comparisonResults.get(i);
                        File file1 = comparisonResult.file1;
                        File file2 = comparisonResult.file2;
                        String file1Name = file1.getName();
                        String file2Name = file2.getName();

                        if (!comparisonResult.isEqual) {
                            continue;
                        }

                        resultHtmlBuilder.append("<div>");
                        resultHtmlBuilder.append("<p>").append(file1Name).append(" и ").append(file2Name).append("</p>");
                        resultHtmlBuilder.append("<button onclick=\"toggleDetails('detail").append(i).append("')\">Подробнее</button>");
                        resultHtmlBuilder.append("<div id='detail").append(i).append("' class='detail'>");

                        resultHtmlBuilder.append("<h3>").append(file1Name).append(" vs ").append(file2Name).append("</h3>");
                        resultHtmlBuilder.append("<pre style=\"padding-left: 20px;\">");
                        try {
                            String file1Content = readWordFileContent(Uri.fromFile(file1));
                            String[] lines1 = file1Content.split("\n");
                            for (int j = 0; j < lines1.length; j++) {
                                resultHtmlBuilder.append("<span class=\"line-number\">").append(j + 1).append("</span>");
                                resultHtmlBuilder.append(" ").append(lines1[j]).append("<br>");
                            }
                        } catch (IOException e) {
                            resultHtmlBuilder.append("Ошибка чтения файла: ").append(e.getMessage());
                            e.printStackTrace();
                        }
                        resultHtmlBuilder.append("</pre>");

                        resultHtmlBuilder.append("<pre style=\"padding-left: 20px;\">");
                        try {
                            String file2Content = readWordFileContent(Uri.fromFile(file2));
                            String[] lines2 = file2Content.split("\n");
                            for (int j = 0; j < lines2.length; j++) {
                                resultHtmlBuilder.append("<span class=\"line-number\">").append(j + 1).append("</span>");
                                resultHtmlBuilder.append(" ").append(lines2[j]).append("<br>");
                            }
                        } catch (IOException e) {
                            resultHtmlBuilder.append("Ошибка чтения файла: ").append(e.getMessage());
                            e.printStackTrace();
                        }
                        resultHtmlBuilder.append("</pre>");

                        resultHtmlBuilder.append("</div>"); // Закрываем detail div
                        resultHtmlBuilder.append("</div>"); // Закрываем main div
                    }
                }

                // Раздел для детального отображения различий по строкам
                resultHtmlBuilder.append("<h2>Детальные различия</h2>");
                for (int i = 0; i < comparisonResults.size(); i++) {
                    ComparisonResult comparisonResult = comparisonResults.get(i);
                    File file1 = comparisonResult.file1;
                    File file2 = comparisonResult.file2;
                    String file1Name = file1.getName();
                    String file2Name = file2.getName();

                    if (comparisonResult.isEqual) {
                        continue;
                    }

                    resultHtmlBuilder.append("<div>");
                    resultHtmlBuilder.append("<p>").append(file1Name).append(" и ").append(file2Name).append("</p>");
                    resultHtmlBuilder.append("<button onclick=\"toggleDetails('diffDetail").append(i).append("')\">Показать различия</button>");
                    resultHtmlBuilder.append("<div id='diffDetail").append(i).append("' class='detail'>");
                    resultHtmlBuilder.append("<pre style=\"padding-left: 20px;\">");
                    try {
                        String file1Content = readWordFileContent(Uri.fromFile(file1));
                        String[] lines1 = file1Content.split("\n");
                        for (int j = 0; j < lines1.length; j++) {
                            resultHtmlBuilder.append("<span class=\"line-number\">").append(j + 1).append("</span>");
                            resultHtmlBuilder.append(" ").append(lines1[j]).append("<br>");
                        }
                    } catch (IOException e) {
                        resultHtmlBuilder.append("Ошибка чтения файла: ").append(e.getMessage());
                        e.printStackTrace();
                    }
                    resultHtmlBuilder.append("</pre>");

                    resultHtmlBuilder.append("<pre style=\"padding-left: 20px;\">");
                    try {
                        String file2Content = readWordFileContent(Uri.fromFile(file2));
                        String[] lines2 = file2Content.split("\n");
                        for (int j = 0; j < lines2.length; j++) {
                            resultHtmlBuilder.append("<span class=\"line-number\">").append(j + 1).append("</span>");
                            resultHtmlBuilder.append(" ").append(lines2[j]).append("<br>");
                        }
                    } catch (IOException e) {
                        resultHtmlBuilder.append("Ошибка чтения файла: ").append(e.getMessage());
                        e.printStackTrace();
                    }
                    resultHtmlBuilder.append("</pre>");
                    resultHtmlBuilder.append("<pre>");
                    for (String difference : comparisonResult.differences) {
                        resultHtmlBuilder.append("<p>").append("<span class=\"deleted-variable\">").append(difference).append("</span>").append("</p>");
                    }
                    resultHtmlBuilder.append("</pre>");

                    resultHtmlBuilder.append("</div>"); // Закрываем detail div
                    resultHtmlBuilder.append("</div>"); // Закрываем main div
                }

                resultHtmlBuilder.append("</body></html>");
                return resultHtmlBuilder.toString();
            }

            protected void onPostExecute(String htmlContent) {
                super.onPostExecute(htmlContent);
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            }
        }
        new LoadWebViewTask().execute();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Результаты сравнения\n");
        builder.setView(webView);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    // Метод для сравнения текста, игнорируя переменные
    private List<String> findDifferences(String text1, String text2) {
        List<String> differences = new ArrayList<>();

        // Разделим тексты на строки для более удобного сравнения
        String[] lines1 = text1.split("\n");
        String[] lines2 = text2.split("\n");

        // Проверим каждую строку
        for (int i = 0; i < Math.min(lines1.length, lines2.length); i++) {
            String line1 = lines1[i];
            String line2 = lines2[i];

            // Сравним строки
            if (!line1.equals(line2)) {
                // Если строки отличаются, добавим информацию о различии в список
                differences.add("Строка " + (i + 1) + ": " + line1 + " != " + line2);
            }
        }

        // Если количество строк в текстах разное, добавим информацию о дополнительных строках
        if (lines1.length > lines2.length) {
            for (int i = lines2.length; i < lines1.length; i++) {
                differences.add("Текст 1 содержит дополнительную строку: " + lines1[i]);
            }
        } else if (lines1.length < lines2.length) {
            for (int i = lines1.length; i < lines2.length; i++) {
                differences.add("Текст 2 содержит дополнительную строку: " + lines2[i]);
            }
        }

        return differences;
    }

    public class ComparisonResult {
        File file1;
        File file2;
        boolean isEqual;
        List<String> differences;

        public ComparisonResult(File file1, File file2, boolean isEqual, List<String> differences) {
            this.file1 = file1;
            this.file2 = file2;
            this.isEqual = isEqual;
            this.differences = differences;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Сравнение файлов: ").append(file1.getName()).append(" и ").append(file2.getName()).append("\n");
            if (isEqual) {
                result.append("Содержимое файлов идентично\n");
            } else {
                result.append("Различия:\n");
                for (String difference : differences) {
                    result.append(difference).append("\n");
                }
            }
            return result.toString();
        }
    }

    private void compareByMethods(List<Uri> selectedFilesList, List<File> tempFiles) {
        showProgressDialog();

        new Thread(() -> {
            try {
                Map<String, List<MethodDeclaration>> fileMethods = new HashMap<>();
                Map<String, Map<MethodDeclaration, MethodDeclaration>> similarMethodsMapping = new HashMap<>();
                Set<String> mainOnlyFiles = new HashSet<>();

                // Считываем и парсим методы из каждого файла
                for (File tempFile : tempFiles) {
                    String content = readWordFileContent(Uri.fromFile(tempFile));
                    CompilationUnit cu = StaticJavaParser.parse(content);
                    List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

                    // Удаляем комментарии из методов
                    for (MethodDeclaration method : methods) {
                        method.getAllContainedComments().forEach(Comment::remove);
                    }

                    if (methods.size() == 1 && methods.get(0).getNameAsString().equals("main")) {
                        mainOnlyFiles.add(tempFile.getName());
                    }

                    fileMethods.put(tempFile.getName(), methods);
                }

                Set<MethodDeclaration> similarMethods = new HashSet<>();
                Set<MethodDeclaration> uniqueMethods = new HashSet<>(fileMethods.values().stream().flatMap(List::stream).collect(Collectors.toList()));
                Set<String> uniqueFiles = new HashSet<>(fileMethods.keySet());
                Set<String> similarFiles = new HashSet<>();

                // Сравниваем методы и определяем схожие
                for (Map.Entry<String, List<MethodDeclaration>> entry : fileMethods.entrySet()) {
                    String fileName1 = entry.getKey();
                    List<MethodDeclaration> methods1 = entry.getValue();

                    for (Map.Entry<String, List<MethodDeclaration>> innerEntry : fileMethods.entrySet()) {
                        String fileName2 = innerEntry.getKey();
                        List<MethodDeclaration> methods2 = innerEntry.getValue();

                        if (!fileName1.equals(fileName2)) {
                            for (MethodDeclaration method1 : methods1) {
                                Log.e("meth1", String.valueOf(method1));
                                for (MethodDeclaration method2 : methods2) {
                                    Log.e("meth2", String.valueOf(method2));
                                    if (areMethodsSimilar(method1, method2)) {
                                        similarMethods.add(method1);
                                        similarMethods.add(method2);
                                        similarFiles.add(fileName1);
                                        similarFiles.add(fileName2);

                                        similarMethodsMapping.computeIfAbsent(fileName1, k -> new HashMap<>()).put(method1, method2);
                                    }
                                }
                            }
                        }
                    }
                }

                uniqueFiles.removeAll(similarFiles);

                // Вычисление процентов
                int totalFiles = tempFiles.size();
                int totalMethods = uniqueMethods.size();
                Log.e("TotalMeth", String.valueOf(totalMethods));
                int uniqueFilesCount = uniqueFiles.size();
                int similarFilesCount = similarFiles.size();
                int similarMethodsCount = similarMethods.size();
                double similarityPercentage = (similarFilesCount / (double) totalFiles) * 100;


                runOnUiThread(this::hideProgressDialog);

                runOnUiThread(() -> showComparisonResultsDialog(fileMethods, uniqueFiles, similarFiles, similarMethods, similarMethodsMapping, mainOnlyFiles, similarityPercentage));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Ошибка при сравнении методов", Toast.LENGTH_SHORT).show();
                    Log.e("Ошибка при сравнении методов", "Exception: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showComparisonResultsDialog(Map<String, List<MethodDeclaration>> fileMethods, Set<String> uniqueFiles, Set<String> similarFiles, Set<MethodDeclaration> similarMethods, Map<String, Map<MethodDeclaration, MethodDeclaration>> similarMethodsMapping, Set<String> mainOnlyFiles, double similarityPercentage) {
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);

        StringBuilder resultHtmlBuilder = new StringBuilder();
        resultHtmlBuilder.append("<html><head><style type=\"text/css\">");
        resultHtmlBuilder.append(".deleted-variable { color: red; }");
        resultHtmlBuilder.append(".method-body.with-differences { background-color: yellow; }");
        resultHtmlBuilder.append(".line-number { color: blue; }");
        resultHtmlBuilder.append(".similar-method { color: red; }");
        resultHtmlBuilder.append(".file-divider { border-top: 2px solid black; margin-top: 20px; padding-top: 20px; }");
        resultHtmlBuilder.append(".detail { display: none; }");
        resultHtmlBuilder.append("</style>");
        resultHtmlBuilder.append("<script type=\"text/javascript\">");

        // JavaScript функция toggleDetails для показа/скрытия блока с различиями
        resultHtmlBuilder.append("function toggleDetails(id) {");
        resultHtmlBuilder.append("var detail = document.getElementById(id);");
        resultHtmlBuilder.append("if (detail.style.display === 'none') {");
        resultHtmlBuilder.append("detail.style.display = 'block';");
        resultHtmlBuilder.append("} else {");
        resultHtmlBuilder.append("detail.style.display = 'none';");
        resultHtmlBuilder.append("}");
        resultHtmlBuilder.append("}");

        resultHtmlBuilder.append("</script>");
        resultHtmlBuilder.append("</head><body>");

        // Сводка
        resultHtmlBuilder.append("<h2>Сводка</h2>");
        resultHtmlBuilder.append("<p>Количество сравниваемых работ: ").append(fileMethods.size()).append("</p>");
        resultHtmlBuilder.append("<p>Количество уникальных работ: ").append(uniqueFiles.size()).append("</p>");
        resultHtmlBuilder.append("<p>Количество схожих работ: ").append(similarFiles.size()).append("</p>");
        resultHtmlBuilder.append("<p>Процент схожести: ").append((int) similarityPercentage).append("%</p>");

        // Работы, содержащие только метод main
        resultHtmlBuilder.append("<h2>Работы с только методом main</h2>");
        if (mainOnlyFiles.isEmpty()) {
            resultHtmlBuilder.append("<p>Нет работ, содержащих только метод main</p>");
        } else {
            for (String fileName : mainOnlyFiles) {
                resultHtmlBuilder.append("<p>").append(fileName).append("</p>");
            }
        }

        // Уникальные работы
        resultHtmlBuilder.append("<h2>Уникальные работы</h2>");
        if (uniqueFiles.isEmpty()) {
            resultHtmlBuilder.append("<p>Нет уникальных работ</p>");
        } else {
            for (String uniqueFile : uniqueFiles) {
                resultHtmlBuilder.append("<p>").append(uniqueFile).append("</p>");
            }
        }

        // Схожие работы
        resultHtmlBuilder.append("<h2>Схожие работы</h2>");
        if (similarFiles.isEmpty()) {
            resultHtmlBuilder.append("<p>Нет схожих работ</p>");
        } else {
            for (String similarFile : similarFiles) {
                resultHtmlBuilder.append("<p>").append(similarFile).append("</p>");
            }
        }

        // Таблица со схожими методами
        resultHtmlBuilder.append("<h2>Схожие методы</h2>");
        resultHtmlBuilder.append("<table border=\"1\">");
        resultHtmlBuilder.append("<tr><th>Метод</th>");

        for (String fileName : fileMethods.keySet()) {
            resultHtmlBuilder.append("<th>").append(fileName).append("</th>");
        }
        resultHtmlBuilder.append("</tr>");

        for (MethodDeclaration method : similarMethods) {
            resultHtmlBuilder.append("<tr><td>").append(method.getName()).append("</td>");

            for (Map.Entry<String, List<MethodDeclaration>> entry : fileMethods.entrySet()) {
                List<MethodDeclaration> methods = entry.getValue();
                boolean found = methods.stream().anyMatch(m -> areMethodsSimilar(m, method));
                resultHtmlBuilder.append("<td>").append(found ? "Да" : "").append("</td>");
            }
            resultHtmlBuilder.append("</tr>");
        }

        resultHtmlBuilder.append("</table>");

        // Раздел с различиями по строкам
        resultHtmlBuilder.append("<h2>Детальные сходства </h2>");

        for (Map.Entry<String, Map<MethodDeclaration, MethodDeclaration>> entry : similarMethodsMapping.entrySet()) {
            String fileName1 = entry.getKey();
            Map<MethodDeclaration, MethodDeclaration> methodsMap = entry.getValue();

            resultHtmlBuilder.append("<h3>").append("<b>").append(fileName1).append("</b>").append("</h3>");

            for (Map.Entry<MethodDeclaration, MethodDeclaration> methodEntry : methodsMap.entrySet()) {
                MethodDeclaration method1 = methodEntry.getKey();
                MethodDeclaration method2 = methodEntry.getValue();

                String fileName2 = "";

                // Находим имя файла для метода method2, исключая файл с тем же именем, что и fileName1
                for (Map.Entry<String, List<MethodDeclaration>> fileEntry : fileMethods.entrySet()) {
                    String currentFileName = fileEntry.getKey();
                    if (!currentFileName.equals(fileName1) && fileEntry.getValue().contains(method2)) {
                        fileName2 = currentFileName;
                        break;
                    }
                }

                resultHtmlBuilder.append("<p>").append("В работе ").append(fileName1).append(" метод ").append(method1.getName()).append(" похож на метод ").append(method2.getName()).append(" в работе ").append(fileName2).append("</p>");
                resultHtmlBuilder.append("<button onclick=\"toggleDetails('").append(fileName1).append(method1.getName()).append("')\">Показать сходства</button>");
                resultHtmlBuilder.append("<h4>Код метода ").append(method1.getName()).append("</h4>");
                resultHtmlBuilder.append("<div id='").append(fileName1).append(method1.getName()).append("' class='detail'>");

                String[] lines1 = method1.getBody().toString().split("\n");
                String[] lines2 = method2.getBody().toString().split("\n");

                resultHtmlBuilder.append("<pre>");
                int maxLines = Math.max(lines1.length, lines2.length);
                for (int i = 0; i < maxLines; i++) {
                    String line1 = i < lines1.length ? lines1[i] : "";
                    String line2 = i < lines2.length ? lines2[i] : "";

                    if (!line1.equals(line2)) {
                        resultHtmlBuilder.append("<span class='deleted-variable'>").append(line1).append("</span>\n");
                        resultHtmlBuilder.append("<span class='deleted-variable'>").append(line2).append("</span>\n");
                    } else {
                        resultHtmlBuilder.append(line1).append("\n");
                    }
                }

                resultHtmlBuilder.append("</pre>");
                resultHtmlBuilder.append("</div>");
            }

            // Добавляем разделитель после завершения сравнения методов в каждом файле
            resultHtmlBuilder.append("<hr>");
        }

        resultHtmlBuilder.append("</body></html>");

        webView.loadDataWithBaseURL(null, resultHtmlBuilder.toString(), "text/html", "UTF-8", null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Результаты сравнения");
        builder.setView(webView);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private boolean areMethodsSimilar(MethodDeclaration method1, MethodDeclaration method2) {
        String[] lines1 = method1.getBody().toString().split("\n");
        String[] lines2 = method2.getBody().toString().split("\n");

        int differences = 0;
        int maxLines = Math.max(lines1.length, lines2.length);
        int threshold = 2;  // Пороговое значение различий

        for (int i = 0; i < maxLines; i++) {
            String line1 = i < lines1.length ? lines1[i].trim() : "";
            String line2 = i < lines2.length ? lines2[i].trim() : "";

            if (!line1.equals(line2)) {
                differences++;
                if (differences >= threshold) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void printAST(Node node) {
        printAST(node, 0);
    }

    private static void printAST(Node node, int depth) {
        // Выводим отступы в зависимости от глубины уровня в дереве
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }
        // Выводим имя класса узла
        System.out.println(node.getClass().getSimpleName());

        // Рекурсивно обрабатываем дочерние узлы
        for (Node child : node.getChildNodes()) {
            printAST(child, depth + 1);
        }
    }


    // Метод для удаления переменных и имен массивов из текста
    private String removeVariables(String text) {
        // Паттерн для удаления комментариев (однострочных и многострочных)
        Pattern commentPattern = Pattern.compile(
                "(?<!:)\\/\\/.*|\\/\\*(?:.|\\R)*?\\*\\/"
        );

        // Удаление комментариев
        Matcher commentMatcher = commentPattern.matcher(text);
        text = commentMatcher.replaceAll("");
        // Паттерн для поиска переменных в строках System.out.println() и их объявлений
        Pattern pattern = Pattern.compile(
                "\\b(?:int|float|double|boolean|String|char)\\s*\\[\\s*\\]\\s*(\\w+)\\s*" +
                        "|\\b(?:int|float|double|boolean|String|char)\\s+(\\w+)\\s*\\[\\s*\\]\\s*" +
                        "|(?<=System\\.out\\.(?:println|print)\\()\\s*(\\w+)(?=\\s*\\)|;)" +
                        "|\\b(\\w+)\\s*(?=[\\+\\-]{2})|\\b(\\w+)\\s*\\[\\s*(.*?)\\s*\\]\\s*" +
                        "|\\breturn\\s+(\\w+);|\\b(?:int|float|double|boolean|String|char)\\s+(\\w+)\\b" +
                        "|(?<=System\\.out\\.(?:println|print)\\()\\s*(\\w+)(?=\\s*\\)|;)" +
                        "|\\b(\\w+)\\s*(?=[\\+\\-]{2})" +
                        "|(?<=System\\.out\\.(?:println|print)\\()\\s*(\\d+ \\+ \\w+)(?=\\s*\\)|;)" + // Добавляем новое выражение
                        "(?<=System\\.out\\.(?:println|print)\\()\\s*([^;]+)(?=;)" +
                        "\\b(?:int|float|double|boolean|String|char)\\s+\\w+\\s*|" +           // Поиск объявлений переменных
                        "(System\\.out\\.print(?:ln)?\\(.*?\\))"                                // Захват содержимого внутри System.out.println
        );

        // Создаем матчер для текста
        Matcher matcher = pattern.matcher(text);

        // Создаем новый текст без переменных
        StringBuilder newTextBuilder = new StringBuilder();
        int lastMatchEnd = 0;
        while (matcher.find()) {
            // Добавляем текст между найденными паттернами
            newTextBuilder.append(text, lastMatchEnd, matcher.start());

            // Проверяем найденный паттерн
            String match = matcher.group();
            if (match.startsWith("System.out.")) {
                // Удаляем переменные и арифметические выражения внутри System.out.println()
                String cleanedMatch = match.replaceAll("\\d+\\s*\\+\\s*\\w+", "")
                        .replaceAll("\\w+", "");
                newTextBuilder.append(cleanedMatch);
            }
            lastMatchEnd = matcher.end();
        }
        // Добавляем оставшуюся часть текста после последнего совпадения
        newTextBuilder.append(text.substring(lastMatchEnd));

        Log.e("textWithoutVar", String.valueOf(newTextBuilder));
        // Возвращаем текст без переменных
        return newTextBuilder.toString();
    }


    private String HilightVariables(String text) {
        // Создаем паттерн для поиска всех вхождений переменных и их использований в выводах System.out.println()
        Pattern pattern = Pattern.compile("\\b(?:int|float|double|boolean|String|char)\\s*\\[\\s*\\]\\s*(\\w+)\\s*" +
                "|\\b(?:int|float|double|boolean|String|char)\\s+(\\w+)\\s*\\[\\s*\\]\\s*" +
                "|(?<=System\\.out\\.(?:println|print)\\()\\s*(\\w+)(?=\\s*\\)|;)" +
                "|\\b(\\w+)\\s*(?=[\\+\\-]{2})" +
                "|\\b(\\w+)\\s*\\[\\s*(.*?)\\s*\\]\\s*" +
                "|\\breturn\\s+(\\w+);" +
                "|\\b(?:int|float|double|boolean|String|char)\\s+(\\w+)\\b" +
                "|(?<=System\\.out\\.(?:println|print)\\()\\s*(\\w+)(?=\\s*\\)|;)" +
                "|\\b(\\w+)\\s*(?=[\\+\\-]{2})" +
                "|\\b(?:\\+\\+|--)(\\w+)\\b");

        // Создаем матчер для текста
        Matcher matcher = pattern.matcher(text);

        // Создаем новый текст с помеченными переменными
        StringBuilder newTextBuilder = new StringBuilder();
        int lastMatchEnd = 0;
        while (matcher.find()) {
            // Добавляем непомеченный текст между вхождениями переменных
            newTextBuilder.append(text, lastMatchEnd, matcher.start());
            lastMatchEnd = matcher.end();

            // Добавляем переменную с пометкой
            String variable = matcher.group();
            Log.e("HilightVar", variable);
            newTextBuilder.append("<span class=\"deleted-variable\">").append(variable).append("</span>");
        }
        // Добавляем оставшуюся часть текста после последнего вхождения переменной
        newTextBuilder.append(text.substring(lastMatchEnd));

        // Возвращаем текст с помеченными переменными
        return newTextBuilder.toString();
    }


    private String readWordFileContent(Uri uri) throws IOException {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                XWPFDocument document = new XWPFDocument(inputStream);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                content.append(extractor.getText());
                extractor.close();
            } else {
                throw new IOException("InputStream is null");
            }
        }

        return content.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("KIActivity", "onActivityResult() called");
        if (requestCode == REQUEST_PICK_DOC_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                FunktionFile.processSelectedFile(uri, this, storageRef);
            } else {
                Toast.makeText(this, "Файл не найден(uri отсутствует)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClickBackKI(View view) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}