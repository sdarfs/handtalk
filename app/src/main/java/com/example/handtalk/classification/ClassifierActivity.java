/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.handtalk.classification;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.handtalk.AppSharedPreferences;
import com.example.handtalk.classification.env.BorderedText;
import com.example.handtalk.classification.env.Logger;
import com.example.handtalk.classification.tflite.Classifier;
import com.example.handtalk.classification.tflite.Classifier.Device;
import com.example.handtalk.classification.tflite.Classifier.Model;
import com.example.handtalk.R;

import java.io.IOException;
import java.util.List;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final float TEXT_SIZE_DIP = 10;
  private Bitmap rgbFrameBitmap = null;
  private long lastProcessingTimeMs;
  private Integer sensorOrientation;
  private Classifier classifier;
  private BorderedText borderedText;
  private Button clearText;
  private Button copyText;
  private TextView translatedValueTextView;
  /** Input image size of the model along x axis. */
  private int imageSizeX;
  /** Input image size of the model along y axis. */
  private int imageSizeY;

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_ic_camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    recreateClassifier(getModel(), getDevice(), getNumThreads());
    if (classifier == null) {
      LOGGER.e("No classifier on preview!");
      return;
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

    clearText = findViewById(R.id.clear);
    copyText = findViewById(R.id.copy);
    translatedValueTextView = findViewById(R.id.translated_text);

    clearText.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view)
      {
        translatedValueTextView.setText("");
      }
    });

    copyText.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Получаем текст из TextView
        String textToCopy = translatedValueTextView.getText().toString();

        // Получаем доступ к системному буферу обмена
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", textToCopy);
        clipboard.setPrimaryClip(clip);

        // Уведомляем пользователя о том, что текст скопирован
        Toast.makeText(view.getContext(), "Текст скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  protected void processImage() {
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    final int cropSize = Math.min(previewWidth, previewHeight);

    runInBackground(
            new Runnable() {
              @Override
              public void run() {
                if (classifier != null) {
                  final long startTime = SystemClock.uptimeMillis();
                  final List<Classifier.Recognition> results =
                          classifier.recognizeImage(rgbFrameBitmap, sensorOrientation);
                  lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                  LOGGER.v("Detect: %s", results);

                  runOnUiThread(
                          new Runnable() {
                            @Override
                            public void run() {
                              showResultsInBottomSheet(results);
                            }
                          });
                }
                readyForNextImage();
              }
            });
  }

  @Override
  protected void onInferenceConfigurationChanged() {
    if (rgbFrameBitmap == null) {
      // Defer creation until we're getting camera frames.
      return;
    }
    final Device device = getDevice();
    final Model model = getModel();
    final int numThreads = getNumThreads();
    runInBackground(() -> recreateClassifier(model, device, numThreads));
  }

  private void recreateClassifier(Model model, Device device, int numThreads) {
    if (classifier != null) {
      LOGGER.d("Closing classifier.");
      classifier.close();
      classifier = null;
    }

    try {
      LOGGER.d(
              "Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
      classifier = Classifier.create(this, model, device, numThreads);
    } catch (IOException e) {
      LOGGER.e(e, "Failed to create classifier.");
    }

    // Updates the input image size.
    imageSizeX = classifier.getImageSizeX();
    imageSizeY = classifier.getImageSizeY();
  }
}