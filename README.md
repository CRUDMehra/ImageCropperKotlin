Android Image Cropper with Kotlin
=======

![Crop](https://github.com/CRUDMehra/ImageCropperKotlin/blob/main/sample/demo_medium.gif?raw=true)


## Features
- Supports image picking from Camera and Gallery
- Crop image with customizable aspect ratios
- Set crop shape (Rectangle, Oval, etc.)
- Control zoom, rotation, and flipping
- Customize cropping UI, including activity title, menu icon colors, and background color
- Compress the cropped image for efficient storage
- High-quality output with options for resolution and format



## Customizations
- Cropping window shape: Rectangular or Oval (cube/circle by fixing aspect ratio).
- Cropping window aspect ratio: Free, 1:1, 4:3, 16:9 or Custom.
- Guidelines appearance: Off / Always On / Show on Toch.
- Cropping window Border line, border corner and guidelines thickness and color.
- Cropping background color.


## Usage
*For a working implementation, please have a look at the Sample Project*


1. Include the library

 ```
 dependencies {
     	        implementation("com.github.CRUDMehra:ImageCropperKotlin:1.0.3")
 }
 ```

Add permissions to manifest

 ```
 
    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="32"
        tools:ignore="ScopedStorage" />

    <queries>
        <intent>
            <action android:name="android.intent.action.PICK" />
            <data android:mimeType="image/jpeg" />
        </intent>
        <intent>
            <action android:name="android.intent.action.CREATE_DOCUMENT" />
            <data android:mimeType="image/jpeg" />
        </intent>
    </queries>

```
Add this line to your Proguard config file
```
-keep class androidx.appcompat.widget.** { *; }
```



2. Add `ImageCropActivity` into your AndroidManifest.xml
 ```xml
  <activity android:name="com.crudmehra.imagecropper.ImageCropActivity"
            android:theme="@style/Base.Theme.AppCompat"/> <!-- optional (needed if default theme has no action bar) -->
 ```

3. Start `CropImageActivity` using builder pattern from your activity
 ```java
 // start picker to get image for cropping and then use the image in cropping activity
 val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/")  // Optional: specify folder
            }
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!

            val intent = CropImage.activity()
                // to set guidelines on crop
                .setGuidelines(ImageCropUI.Guidelines.ON)
                // to change title of activity
                .setActivityTitle("Choose Image")
                // image flip option by default true
                .setAllowFlipping(false)
                // image rotate option by default true
                .setAllowRotation(false)
                // change crop shape
                .setCropShape(ImageCropUI.CropShape.RECTANGLE)
                // set crop button title
                .setCropMenuCropButtonTitle("Done")
                // set icon
//              .setCropMenuCropButtonIcon(R.drawable.ic_launcher_background)
                // to change resolution size of cropped image // this will change image quality // you also can skip this step for high quality image
                //.setRequestedSize(400, 400)
                // picker type like camera or gallery or both
                .setPickerType(ImageCropParams.PickerType.CAMERA_AND_GALLERY)
                // to change crop viewer background
//              .setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                // to change menu icon color
                .setActivityMenuIconColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                // set crop image size like 1:1 , 9:12 or something else
                .setAspectRatio(9, 12)
                // set zoom enable while cropping image by default true
                .setAutoZoomEnabled(false)
                // save image in local storage
                .setOutputUri(imageUri)
                // set output quality by default 100
                .setOutputCompressQuality(100)
                // default JPEG
                .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                // get intent value and set in your launcher
                .getIntent(this)

            launcherRequest.launch(intent)
 ```



4. Override `ActivityResultContracts` method in your activity to get crop result
 ```java
private val launcherRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultURI = getActivityResult(result.data)
        ivCroppedImage?.setImageURI(resultURI?.uri)
        Log.e("MainActivity", "${resultURI?.uri.toString()} ")
    }
 ``` 

If anybody face any issue please contact this email

[hello@crudmehra.com](hello@crudmehra.com)

## License
Originally forked from [ArthurHub/edmodo/cropper](https://github.com/ArthurHub/Android-Image-Cropper.git).

Copyright 2016, Arthur Teplitzki, 2013, Edmodo, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the   License.
You may obtain a copy of the License in the LICENSE file, or at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS   IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
