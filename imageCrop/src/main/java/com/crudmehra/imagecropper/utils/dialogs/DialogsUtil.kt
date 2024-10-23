package com.crudmehra.imagecropper.utils.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog

fun showImagePickerDialog(
    context: Context,
    titleDialog: String = "Select Image Source",
    negativeButtonTitle: String = "Cancel",
    onItemSelected: (String) -> Unit   // Single higher-order function for clicked item
) {
    // Options for camera and gallery
    val options = arrayOf("Camera", "Gallery")

    // Create an AlertDialog to show the options
    val builder = AlertDialog.Builder(context)
    builder.setTitle(titleDialog)
    builder.setItems(options) { _, which ->
        val selectedOption = options[which]
        onItemSelected(selectedOption)  // Pass the selected option to the callback
    }

    builder.setNegativeButton(negativeButtonTitle) { dialog, _ ->
        onItemSelected("Cancel")
        dialog.dismiss()    // Cancel the dialog if pressed
    }

    val dialog = builder.create()
   /* dialog.setOnDismissListener {
        onItemSelected("Cancel")
    }*/
    dialog.show()
}