package com.example.quizapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private CameraDevice mCameraDevice;

    //Step 1: Declaration
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;

    EditText etLogin, etPassword;
    Button bLogin;
    TextView tvRegister;

    FirebaseAuth mAuth;

    // private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Step 2: Recuperation des ids
        etLogin = (EditText) findViewById(R.id.etLogin);
        etPassword = (EditText) findViewById(R.id.etPassword);
        bLogin = (Button) findViewById(R.id.bLogin);
        tvRegister = (TextView) findViewById(R.id.tvRegister);
        mAuth = FirebaseAuth.getInstance();
        //Step 3: Association de listeners
        bLogin.setOnClickListener(view -> {
            loginUser();
        });
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Step 4: Traitement
                startActivity(new Intent(MainActivity.this, Register.class));
                finish();
            }
        });


        // Vérifier que la permission de la caméra est accordée
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Si la permission n'est pas accordée, demander à l'utilisateur de confirmer l'utilisation de la caméra
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("L'application souhaite utiliser la caméra. Voulez-vous continuer ?");
            builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Demander la permission d'utiliser la caméra
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                }
            });
            builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Fermer l'application si l'utilisateur ne souhaite pas utiliser la caméra
                    finish();
                }
            });
            builder.setCancelable(false);
            builder.show();
        } else {
            // La permission a déjà été accordée
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Utilisation de la caméra et GPS");
            builder.setMessage("Cette application va utiliser la caméra pour prendre une photo et GPS pour vous localiser. Êtes-vous sûr de vouloir continuer ?");
            builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    takePicture();
                }
            });
            builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.show();
        }

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
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        // Créer un nom de fichier unique pour l'image
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";

        // Créer un fichier pour l'image
        File imageFile = new File(imagesDir, fileName);

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






    private void loginUser(){
        String email = etLogin.getText().toString();
        String password = etPassword.getText().toString();
        if(TextUtils.isEmpty(email)){
            etLogin.setError("Email cannot be empty");
            etLogin.requestFocus();
        }else if(TextUtils.isEmpty(password)){
            etPassword.setError("password cannot be empty");
            etPassword.requestFocus();
        }else{
            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        Toast.makeText(MainActivity.this, "User logged in successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, QuestionActivity.class));
                    }
                else{
                    Toast.makeText(MainActivity.this, "Log in Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
                }
            });
        }





    }
}
