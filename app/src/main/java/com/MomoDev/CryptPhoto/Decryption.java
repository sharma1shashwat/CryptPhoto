package com.MomoDev.CryptPhoto;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.security.MessageDigest;

import java.util.Base64;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
public class Decryption extends AppCompatActivity {

    public ImageView img;
    public Button ChooseImage,Extract;
    public EditText ePassword;
    public static final int STORAGE_REQUEST = 101;

    public Bitmap stegoImage,steg_bitmap;
    public Map map;
    String password,message,reqMessage,passMessage;
    String[] storagePermission;
    String AES ="AES";

    private boolean isSISelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decryption);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Decryption");

        ePassword = findViewById(R.id.dPassword);
        ChooseImage = findViewById(R.id.btChooseFile);
        Extract = findViewById(R.id.btExtract);
        img = findViewById(R.id.ivImage);


        storagePermission= new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

        ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {

                img.setImageURI(result);
                isSISelected = true;
            }
        });
        //Handle button click.
        ChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Permission demo project's code

                if(!checkStoragePermission()){
                    requestStoragePermission();
                }
                else{
                    //pickFromGallery();
                    launcher.launch("image/*");

                }
            }
        });

        Extract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isSISelected)
                {
                    //convert stego-image to bitmap.
                    stegoImage =imageToBitmap();
                    if(stegoImage != null) {
                        map = Extracting.extractSecretMessage(stegoImage);
                    }
                    else{
                        Toast.makeText(Decryption.this,"There is no image",Toast.LENGTH_SHORT).show();
                    }

                    if(map!= null){
                        String bits =(String) map.get(Constants.MESSAGE_BITS);
                        byte[] imageBytes = HelperMethods.bitsStreamToByteArray(bits);
                        steg_bitmap = HelperMethods.byteArrayToBitmap(imageBytes);
                       // Toast.makeText(Decryption.this,"STEG bitmap caught",Toast.LENGTH_LONG).show();

                    }else{
                        Toast.makeText(Decryption.this,"Failure during decrypting message",Toast.LENGTH_SHORT).show();
                    }

                    reqMessage = scanQRImage(steg_bitmap);
                    password = ePassword.getText().toString().trim();
                    if (!password.isEmpty() && !reqMessage.isEmpty()) {
                        try {
                            passMessage= displayMessage(reqMessage, password);

                            Intent intent = new Intent(Decryption.this,DisplayDecryptedMessage.class);
                            intent.putExtra("dMessage",passMessage);
                            startActivity(intent);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(Decryption.this, "Provide password.", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(Decryption.this, "Provide image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void requestStoragePermission() {
        requestPermissions(storagePermission,STORAGE_REQUEST);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0){
            boolean storage_accepted= grantResults[0]==(PackageManager.PERMISSION_GRANTED);
            if(!storage_accepted){
                Toast.makeText(this,"Please enable storage permission",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String decrypt(String outputString, String password) throws Exception{
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.DECRYPT_MODE,key);
        byte[] decodedValue = Base64.getDecoder().decode(outputString);
        byte[] decValue = c.doFinal(decodedValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }


    //data encodes in messageDigest
    private SecretKeySpec generateKey(String password) throws Exception{
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = password.getBytes("UTF-8");
        digest.update(bytes,0,bytes.length);
        byte[] key= digest.digest();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;
    }

    //scans the Qr in image format
    public String scanQRImage(Bitmap bMap) {
        String contents = null;

        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();
        try {
            Result result = reader.decode(bitmap);
            contents = result.getText();
            Toast.makeText(Decryption.this, "Message Decrypted", Toast.LENGTH_SHORT).show();

        }
        catch (Exception e) {
            Log.e("QrTest", "Error decoding barcode", e);
        }
        return contents;
    }

    //Decrypt the payload of Qr code
    private String displayMessage(String reqMessage,String password){
        try {
            message = decrypt(reqMessage,password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    private Bitmap imageToBitmap() {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) img.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();
        return bitmap;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

}