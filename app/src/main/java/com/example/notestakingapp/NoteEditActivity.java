package com.example.notestakingapp;

import static com.google.android.material.internal.ViewUtils.dpToPx;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notestakingapp.adapter.NoteDetailsAdapter;
import com.example.notestakingapp.database.DatabaseHandler;
import com.example.notestakingapp.database.NoteTakingDatabaseHelper;
import com.example.notestakingapp.ui.BottomDialog;
import com.example.notestakingapp.utils.ImageUtils;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class NoteEditActivity extends AppCompatActivity {
    private ImageView backImage, voiceImage, imageImage, scribbleImage, cameraImage, shirtImage;
    private LinearLayout layoutBack;
    private TextView textBack;
    private RecyclerView recyclerViewDetails;
    private NoteDetailsAdapter noteDetailsAdapter;
    private List<Item> mItemList;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;
    private static final int REQUEST_CODE_CAMERA = 3;
    private static final String IMAGE_PROP = "image";
    private static final String VOICE_PROP = "voice";

    View activityRootView;


    private int textSegmentId = -1;
    private boolean isTheFirst = true;
    private int noteId = -1;
    private int voiceId = -1;
    private int imageId = -1;
    private LinearLayout toolNavigation;
    private SQLiteDatabase db;
    DatabaseHandler databaseHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_edit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NoteTakingDatabaseHelper noteTakingDatabaseHelper = new NoteTakingDatabaseHelper(getApplicationContext());
        db = noteTakingDatabaseHelper.getReadableDatabase();
        databaseHandler = new DatabaseHandler();

        //check keyboard state changed
//        https://stackoverflow.com/questions/4745988/how-do-i-detect-if-software-keyboard-is-visible-on-android-device-or-not
        View rootView = getWindow().getDecorView().getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Determine if the keyboard is visible
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getHeight();
                int keypadHeight = screenHeight - r.bottom;

                // If the keypad height is greater than a threshold, assume the keyboard is visible
                boolean isVisible = keypadHeight > screenHeight * 0.15;

                if (isVisible) {
                    // Keyboard is visible
                    toolNavigation.setVisibility(View.VISIBLE);
                } else {
                    // Keyboard is not visible
                    toolNavigation.setVisibility(View.GONE);
                }
            }
        });


        // khoi chay ui
        initUi();

        //todo: phai gan lai gia tri cho textSegmentId va noteId

        //xu li recycler view
        noteDetailsAdapter = new NoteDetailsAdapter();
        mItemList = new ArrayList<>();
        //tao Item ui
        mItemList.add(new Item(Item.TYPE_EDIT_TEXT_TITLE, 123));
        mItemList.add(new Item(Item.TYPE_EDIT_TEXT, textSegmentId));
        noteDetailsAdapter.setData(mItemList);
        recyclerViewDetails.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDetails.setAdapter(noteDetailsAdapter);

        //todo: gan noteId please Duy oi, tao note moi
        noteId = (int) databaseHandler.insertNote(this, null, null, null, null);
        Log.d("testInsert", String.valueOf(noteId));
        //        noteId = (int) AppDatabase.getInstance(AddNoteActivity.this).noteDao().insert(new Note(""));

        noteDetailsAdapter.setNoteId(noteId);
        noteDetailsAdapter.setOnEditTextChangedListener(new NoteDetailsAdapter.OnEditTextChangedListener() {
            @Override
            public void onTextChanged(int position, String text) {
                mItemList.get(position).setText(text);
                if (isTheFirst) {
                    //todo: insert 1 text segment vao db theo noteId pls -> xong
                    textSegmentId = (int) databaseHandler.insertTextSegment(NoteEditActivity.this, noteId, text);
                    isTheFirst = false;
                    Log.d("testInsert", "tao textSegment id = " + textSegmentId + "noteId = " + noteId);
                } else if (!isTheFirst) {
                    databaseHandler.updateTextSegment(NoteEditActivity.this, textSegmentId, text);
                }

                //todo: Update textSegment trong db -> xong

            }
        });

        //su kien scroll de an tool
        recyclerViewDetails.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

            }
        });

        //back button
        layoutBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_animation);
//                imageViewAdd.startAnimation(animation);
                Animation animation = AnimationUtils.loadAnimation(NoteEditActivity.this, R.anim.fade_out);
                backImage.startAnimation(animation);
                textBack.setAnimation(animation);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        cameraImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOpenCamera();
            }
        });

        shirtImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showToolDialog();
                BottomDialog.showToolDialog(NoteEditActivity.this);
            }
        });

        imageImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOpenGallery();
            }
        });

        //xu li su kien khi back quay ve activity truoc cua dien thoai
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                deleteNoteIsEmpty(noteId);
                finish();
            }
        });
    }


    private void initUi() {

        shirtImage = findViewById(R.id.image_shirt);
        layoutBack = findViewById(R.id.layout_back);
        backImage = findViewById(R.id.image_back);
        textBack = findViewById(R.id.text_back);
        voiceImage = findViewById(R.id.image_voice);
        imageImage = findViewById(R.id.image_image);
        scribbleImage = findViewById(R.id.image_scribble);
        recyclerViewDetails = findViewById(R.id.recycler_view_details);
        toolNavigation = findViewById(R.id.tool_navigation);
        cameraImage = findViewById(R.id.image_camera);
        activityRootView = findViewById(R.id.main);
    }


    private void deleteNoteIsEmpty(int noteId) {
        if (mItemList.size() == 2 && mItemList.get(0).getText().isEmpty() && mItemList.get(1).getText().isEmpty()) {
            //todo: xoa note neu note do la empty cho nay xu li hoi ngu
            databaseHandler.deleteNote(this, noteId);
            Log.d("testInsert", "deletedNote" + noteId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    //todo: luu anh vao db nho them try catch
                    try {
                        imageId = (int) databaseHandler.insertImage(NoteEditActivity.this, noteId, ImageUtils.uriToBytes(selectedImageUri, null));
                        Log.d("testInsert", "tao Image id = "+imageId+" noteId = "+noteId);
                    } catch (Exception e) {
                        Toast.makeText(this, "insert image into db error!", Toast.LENGTH_SHORT).show();
                    }
                    //ex: AppDatabase.getInstance(AddNoteActivity.this).imageDao().insert(new Image(noteId, ImageUtils.uriToBytes(imageUri, AddNoteActivity.this)));
//                        imageSelected.setImageBitmap(bitmap);
//                        imageSelected.setVisibility(View.VISIBLE);

                    //hien thi hinh anh ra noteEdit
                    mItemList.add(new Item(Item.TYPE_IMAGE_VIEW, selectedImageUri, imageId, IMAGE_PROP));
                    //xu li inputtext is empty se bi xoa di neu anh dc them vao
                    int size = mItemList.size();
                    if (mItemList.get(size - 2).getType() == Item.TYPE_EDIT_TEXT && mItemList.get(size - 2).getText().isEmpty()) {
                       //xoa trong db
                        databaseHandler.deleteNote(NoteEditActivity.this, textSegmentId);
                        Log.d("testInsert", "da xoa textSegment id = "+textSegmentId+" noteId = "+noteId);
                        //xoa o giao dien
                        mItemList.remove(size - 2);
                        Toast.makeText(NoteEditActivity.this, "Remove editText sucess!", Toast.LENGTH_SHORT).show();
                    }
                    noteDetailsAdapter.setData(mItemList);
                    //todo: cap nhat lai textSegmentId va them textsegment vao db
                    textSegmentId = (int) databaseHandler.insertTextSegment(NoteEditActivity.this, noteId, "");
                    Log.d("testInsert", "tao textSegment id = "+textSegmentId+" noteId = "+noteId);

                    //textSegmentId = appdb.insert();
                    mItemList.add(new Item(Item.TYPE_EDIT_TEXT, textSegmentId));
                } catch (Exception exception) {
                    Toast.makeText(this, "selected image error!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void onOpenCamera() {
        ImagePicker.with(this).cameraOnly().crop().compress(1024).maxResultSize(1080, 1080)//User can only capture image using Camera
                .start();
    }

    private void onOpenGallery() {
        ImagePicker.with(this).galleryOnly().crop().compress(1024).maxResultSize(1080, 1080).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}