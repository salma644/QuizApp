package com.example.quizapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Bundle;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class score extends AppCompatActivity {
    List<Boolean> results = new ArrayList<>();

    Button bLogout, bTry , bmaps;
    ProgressBar progressBar;
    TextView tvScore;
    int score;
    private Interpreter tflite;
    private ArrayList<String> imagePaths;
    private String imagesDirPath;
    private static final int INPUT_SIZE = 224; // Taille de l'image en entrée attendue par le modèle
    private static final int NUM_CLASSES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        tvScore = (TextView) findViewById(R.id.tvScore);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        bLogout = (Button) findViewById(R.id.bLogout);
        bTry = (Button) findViewById(R.id.bTry);
        bmaps = (Button) findViewById(R.id.bmaps);
        Intent intent = getIntent();
        score = intent.getIntExtra("score", 0);
        progressBar.setProgress(100 * score / 7);
        tvScore.setText(100 * score / 7 + " %");
        //Toast.makeText(getApplicationContext(),score+"",Toast.LENGTH_SHORT).show();
        bLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Merci de votre Participation !", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        bTry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(score.this, QuestionActivity.class));
                finish();
            }
        });
        bmaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(score.this, MapsActivity.class));
            }
        });

        //CETTE SECTION DE CODE PERMET LE CHARGEMENT DU MODEL ,ALORS QU'IL CONTIENT DES ERREURS QUE JE FALLAIS LES REGLER
        /*Intent intent1 = getIntent();

        try {
            tflite = new Interpreter(loadModelFile(), new Interpreter.Options());
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        // Récupérer la liste d'images de l'utilisateur depuis l'activité QuestionsActivity
        /*Intent intent1 = getIntent();
        File imagesDir = new File(getFilesDir(), "images");
        String imagesDirPath = imagesDir.getAbsolutePath();
        imagePaths = intent1.getStringArrayListExtra(imagesDirPath);*/



        /*
        imagePaths = intent.getStringArrayListExtra("imagePaths");
        imagesDirPath = intent.getStringExtra("imagesDirPath");
        //String imagesDirPath = intent.getStringExtra("imagesDirPath");
        //ArrayList<String> imagePaths = intent.getStringArrayListExtra("imagePaths");

        // Traiter les réponses de l'utilisateur
        // Obtenir le répertoire contenant les images
        File imagesDir = new File(getFilesDir(), "images");
        if (!imagesDir.exists()) {
            // Le répertoire n'existe pas, il n'y a pas d'images à traiter
            return;
        }

// Récupérer la liste des fichiers dans le répertoire
        File[] imageFiles = imagesDir.listFiles();

// Prétraiter et prédire chaque image
        for (File imageFile : imageFiles) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
            inputBuffer.order(ByteOrder.nativeOrder());
            inputBuffer.rewind();
            for (int i = 0; i < INPUT_SIZE; ++i) {
                for (int j = 0; j < INPUT_SIZE; ++j) {
                    int pixelValue = bitmap.getPixel(j, i);
                    inputBuffer.putFloat((pixelValue >> 16) & 0xFF);
                    inputBuffer.putFloat((pixelValue >> 8) & 0xFF);
                    inputBuffer.putFloat(pixelValue & 0xFF);
                }
            }
            float[][] output = new float[1][NUM_CLASSES];
            tflite.run(inputBuffer, output);
            boolean isFraud = output[0][0] > output[0][1];
            results.add(isFraud);
        }

// Afficher les résultats
        for (int i = 0; i < results.size(); i++) {
            boolean isFraud = results.get(i);
            String imagePath = imageFiles[i].getAbsolutePath();
            Log.d("Fraud Detection", "Image " + imagePath + " is " + (isFraud ? "fraudulent" : "legitimate"));
        }*/
    }

    /*
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
*/
}