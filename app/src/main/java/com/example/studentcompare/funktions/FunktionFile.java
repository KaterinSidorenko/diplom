package com.example.studentcompare.funktions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FunktionFile {
    public static String group;
    private static StorageReference selectedMainFolderReference;
    private static StorageReference selectedSubfolderReference;
    private static String selectedSubfolderName;

    private static final int FILE_PICK_REQUEST_CODE = 1001;

    public static void showLocationDialog(Activity activity) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference gsReference = storage.getReferenceFromUrl("gs://student-compare.appspot.com");

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Выберите основную папку для сохранения файла");

        gsReference.listAll().addOnSuccessListener(listResult -> {
            int index = 0;
            String[] folderNames = new String[listResult.getPrefixes().size()];
            for (StorageReference prefix : listResult.getPrefixes()) {
                folderNames[index++] = prefix.getName();
            }

            builder.setItems(folderNames, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    selectedMainFolderReference = gsReference.child(folderNames[which]);
                    showSubfolderDialog(activity);
                    group = folderNames[which];
                }
            });
            builder.create().show();
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            // Handle failure
        });
    }

    private static void showSubfolderDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Выберите подпапку");

        selectedMainFolderReference.listAll().addOnSuccessListener(listResult -> {
            int index = 0;
            String[] subfolderNames = new String[listResult.getPrefixes().size()];
            for (StorageReference prefix : listResult.getPrefixes()) {
                subfolderNames[index++] = prefix.getName();
            }

            builder.setItems(subfolderNames, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    selectedSubfolderReference = selectedMainFolderReference.child("/"+subfolderNames[which]);

                    selectedSubfolderName = subfolderNames[which];

                    openFilePicker(activity);
                }
            });
            builder.create().show();
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            // Handle failure
        });
    }

    // Метод для получения значения group
    public static String getGroup() {
        return group;
    }

    public static void openFilePicker(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(Intent.createChooser(intent, "Выберите файл"), FILE_PICK_REQUEST_CODE);    }

    public static boolean checkFileContainsMain(ContentResolver contentResolver, Uri uri) {
        try {
            InputStream is = contentResolver.openInputStream(uri);
            XWPFDocument doc = new XWPFDocument(is);
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            String text = extractor.getText();

            int mainIndex = text.indexOf("public static void main");
            if (mainIndex != -1) {
                text = text.substring(mainIndex);
            }

            boolean containsMain = mainIndex != -1;
            Log.d("FileContent", text);
            is.close();
            return containsMain;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressLint("Range")
    public static String getFileNameFromUri(Uri uri, Activity activity) {
        String fileName = null;
        String scheme = uri.getScheme();
        if (scheme != null) {
            if (scheme.equals("content")) {
                Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else if (scheme.equals("file")) {
                fileName = new File(uri.getPath()).getName();
            }
        }
        return fileName;
    }

    public static void processSelectedFile(Uri uri, Activity activity, StorageReference storageRef) {
        if (uri != null) {
            String fileName = getFileNameFromUri(uri, activity);

            try {
                boolean containsMain = checkFileContainsMain(activity.getContentResolver(), uri);
                if (containsMain) {
                    uploadModifiedFileToFirebase(uri, fileName, activity, storageRef);
                } else {
                    Toast.makeText(activity, "Файл не содержит метода main", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(activity, "Ошибка обработки файла", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, "Файл не выбран", Toast.LENGTH_SHORT).show();
        }
    }

    private static void uploadModifiedFileToFirebase(Uri uri, String originalFileName, Activity activity, StorageReference storageRef) {
        try {
            InputStream is = activity.getContentResolver().openInputStream(uri);
            XWPFDocument sourceDoc = new XWPFDocument(is);
            XWPFDocument targetDoc = new XWPFDocument();

            int mainIndex = -1;
            for (int i = 0; i < sourceDoc.getParagraphs().size(); i++) {
                XWPFParagraph paragraph = sourceDoc.getParagraphs().get(i);
                String text = paragraph.getText();

                mainIndex = text.indexOf("public static void main");
                if (mainIndex != -1) {
                    text = text.substring(mainIndex);

                    // Копирование параграфов
                    for (int j = i; j < sourceDoc.getParagraphs().size(); j++) {
                        XWPFParagraph srcParagraph = sourceDoc.getParagraphs().get(j);
                        XWPFParagraph tgtParagraph = targetDoc.createParagraph();
                        tgtParagraph.getCTP().set(srcParagraph.getCTP());
                    }
                    break;
                }
            }

            File tempFile = File.createTempFile("temp", ".docx");
            FileOutputStream fos = new FileOutputStream(tempFile);
            targetDoc.write(fos);
            fos.close();

            uploadFileToFirebase(Uri.fromFile(tempFile), originalFileName, activity, storageRef, selectedSubfolderName);

            tempFile.deleteOnExit();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void uploadFileToFirebase(Uri uri, String fileName, Activity activity, StorageReference storageRef, String subfolderName) {
        try {
            InputStream is = activity.getContentResolver().openInputStream(uri);

            StorageReference fileRef = storageRef.child(FunktionFile.getGroup().toString() + "/" + subfolderName + "/" + fileName);
            fileRef.putStream(is)
                    .addOnSuccessListener(taskSnapshot -> {
                        Toast.makeText(activity, "Файл успешно отправлен в Firebase Storage", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity, "Ошибка при отправке файла в Firebase Storage", Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}