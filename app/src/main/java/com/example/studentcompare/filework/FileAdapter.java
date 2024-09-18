package com.example.studentcompare.filework;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentcompare.R;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<Uri> files;
    private static Context context;

    public FileAdapter(Context context, List<Uri> files) {
        this.context = context;
        this.files = files;

    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        Uri fileUri = files.get(position);
        String fileName = getFileName(fileUri);

        holder.textFileName.setText(fileName);
        holder.imageFileIcon.setImageResource(R.drawable.ic_file_icon);
        holder.itemView.setOnClickListener(v -> openAndDisplayFile(fileUri));
        holder.buttonDelete.setOnClickListener(v -> deleteFile(position));

    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static String getFileName(Uri uri) {
        String fileName = "";
        try {
            String path = uri.getPath();
            if (path != null) {
                fileName = path.substring(path.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }



    private void openWordFile(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Если на устройстве нет приложения для просмотра файла
            Toast.makeText(context, "Приложение для открытия файла не найдено", Toast.LENGTH_SHORT).show();
            Log.e("File Open Error", "ActivityNotFoundException: " + e.getMessage());
        }
    }



    private void deleteFile(int position) {
        // Получаем URI файла, который нужно удалить
        Uri fileUri = files.get(position);

        // Получаем ссылку на файл в Firebase Storage
        StorageReference fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUri.toString());

        // Удаляем файл из Firebase Storage
        fileRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Если удаление файла прошло успешно, удаляем его из списка и обновляем RecyclerView
                files.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "Файл удален", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Если произошла ошибка при удалении файла, выводим сообщение об ошибке
                Toast.makeText(context, "Ошибка при удалении файла", Toast.LENGTH_SHORT).show();
                Log.e("File Delete Error", "Failed to delete file: " + e.getMessage());
            }
        });
    }
    private String convertDocxToHtml(File file) throws IOException {
        StringBuilder htmlContent = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            String text = extractor.getText();
            extractor.close();

            // Заменяем символы, которые могут быть неправильно отображены в HTML
            text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

            // Добавляем текст в HTML-контент, заключая его в теги <pre>
            htmlContent.append("<pre>").append(text).append("</pre>");
        }

        return htmlContent.toString();
    }




    private void openAndDisplayFile(Uri fileUri) {
        // Получаем имя файла из URI
        String fileName = getFileName(fileUri);

        // Создаем временный файл во внешнем хранилище (filesDir)
        File tempFile = new File(context.getFilesDir(), fileName);

        // Получаем ссылку на Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUri.toString());

        // Загружаем файл с Firebase Storage
        storageRef.getFile(tempFile).addOnSuccessListener(taskSnapshot -> {
            try {
                // Если файл загружен успешно, конвертируем его содержимое в HTML и отображаем в WebView
                String htmlContent = convertDocxToHtml(tempFile);
                WebView webView = new WebView(context);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);

                // Создаем диалоговое окно для отображения WebView
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Содержимое файла");
                builder.setView(webView);
                builder.setPositiveButton("Закрыть", (dialog, which) -> dialog.dismiss());
                builder.show();
            } catch (IOException e) {
                // В случае ошибки чтения файла выводим сообщение об ошибке
                Toast.makeText(context, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
                Log.e("File Read Error", "Failed to read file", e);
            }
        }).addOnFailureListener(exception -> {
            // Если возникает ошибка при загрузке файла, выводим сообщение об ошибке
            Toast.makeText(context, "Ошибка загрузки файла", Toast.LENGTH_SHORT).show();
            Log.e("File Download Error", "Failed to download file", exception);
        });
    }




    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView imageFileIcon;
        TextView textFileName;
        ImageButton buttonDelete;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFileIcon = itemView.findViewById(R.id.imageFileIcon);
            textFileName = itemView.findViewById(R.id.textFileName);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
