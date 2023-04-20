package com.example.quizapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionActivity extends AppCompatActivity {
    private CameraDevice mCameraDevice;

    RadioGroup radioGroup;
    TextView Ztext;
    RadioButton optionA;
    RadioButton optionB;
    RadioButton optionC;
    RadioButton optionD;
    Button next;
    String rightAnswer;
    String Answer;
    List<Question> questions = new ArrayList<>();
    int score;
    String imagePaths;
    String imagesDirPath;
    public static ArrayList<Question> list;
    private boolean isTakingPictures = false;

    DatabaseReference databaseReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        next = findViewById(R.id.next);
        Ztext = findViewById(R.id.txt);
        optionA = findViewById(R.id.optA);
        optionB = findViewById(R.id.optB);
        optionC = findViewById(R.id.optC);
        optionD = findViewById(R.id.optD);
        score = 0;
        radioGroup = findViewById(R.id.radioGroup);

        list = new ArrayList<>() ;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Si la permission n'est pas accordée, demander à l'utilisateur de nexter l'utilisation de la caméra

        } else {
            // La permission a déjà été accordée

            startTakingPictures();

        }

        databaseReference = FirebaseDatabase.getInstance().getReference("quiz").child("questions");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot questionSnapshot : snapshot.getChildren()) {
                    Question question = questionSnapshot.getValue(Question.class);
                    questions.add(question);
                }

                // Charger la première question
                loadQuestion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // En cas d'erreur de lecture de la base de données
                Log.e("QuestionActivity", "Error reading questions from database", error.toException());
            }
        });

        //loadQuestion();

    }
    private static final int INTERVALLE_DE_TEMPS = 20000; // 20 secondes

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            takePicture();
            handler.postDelayed(this, INTERVALLE_DE_TEMPS);
        }
    };

    private void startTakingPictures() {
        isTakingPictures = true;
        handler.postDelayed(runnable, INTERVALLE_DE_TEMPS);
    }

    private void stopTakingPictures() {
        isTakingPictures = false;
        handler.removeCallbacks(runnable);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrêter la prise de photos périodiques
        stopTakingPictures();
    }
    private void takePicture() {
        try {
            // Obtenir le service CameraManager
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            // Obtenir l'ID de la caméra frontale (index 1)
            String cameraId = cameraManager.getCameraIdList()[1];

            // Configurer la capture d'image
            ImageReader imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    // Enregistrer l'image dans un fichier
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    saveImage(data);
                    image.close();
                }
            }, new Handler(getMainLooper()));

            // Demander la permission d'utiliser la caméra
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                private CameraDevice cameraDevice;

                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice; // stocker l'objet CameraDevice dans la variable globale
                    try {
                        // Configurer la capture d'image
                        CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        captureRequestBuilder.addTarget(imageReader.getSurface());

                        // Demander la capture d'image
                        cameraDevice.createCaptureSession(Collections.singletonList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                try {
                                    session.capture(captureRequestBuilder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                // Échec de la configuration de la capture
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(CameraDevice cameraDevice) {
                    // Fermer la caméra
                    cameraDevice.close();
                }


                @Override
                public void onError(CameraDevice cameraDevice, int error) {
                    // Erreur lors de l'ouverture de la caméra
                }


            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void saveImage(byte[] data) {
        // Obtenir le chemin du répertoire pour les images
        File imagesDir = new File(getFilesDir(), "images");
        imagesDirPath = imagesDir.getAbsolutePath();
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        // Créer un nom de fichier unique pour l'image
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";

        // Créer un fichier pour l'image
        File imageFile = new File(imagesDir, fileName);
        imagePaths= imageFile.getAbsolutePath();

        try {
            // Enregistrer les données de l'image dans le fichier
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(data);
            fos.close();
            mCameraDevice.close(); // fermer la caméra après chaque capture d'image

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void loadQuestion() {
        if(questions.size() > 0) {
            Question q = questions.remove(0);
            Ztext.setText(q.getQuestion());
            List<String> answers = q.getAnswers();

            optionA.setText(answers.get(0));
            optionB.setText(answers.get(1));
            optionC.setText(answers.get(2));
            optionD.setText(answers.get(3));

            rightAnswer = q.getRightAnswer();
        } else {
            Intent intent = new Intent(QuestionActivity.this, score.class);
            intent.putExtra("score", score);
            startActivity(intent);
            finish();
        }
    }


    public void loadAnswer(View view) {
        int op = radioGroup.getCheckedRadioButtonId();

        switch (op) {
            case R.id.optA:
                Answer = "A";
                break;

            case R.id.optB:
                Answer = "B";
                break;

            case R.id.optC:
                Answer = "C";
                break;

            case R.id.optD:
                Answer = "D";
                break;

            default:
                return;

        }

        radioGroup.clearCheck();

        isRightOrWrong(Answer);
        loadQuestion();

    }

    private void isRightOrWrong(String Answer){

        if(Answer.equals(rightAnswer)) {
            this.score += 1;

        }


    }


}