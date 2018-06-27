package com.imitate_genic_wall.Dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle

class OriginalDialog : DialogFragment() {

    var title = "title"
    var message = ""
    var okText = "OK"
    var cancelText = ""
    /** ok押下時の挙動 */
    var onOkClickListener: DialogInterface.OnClickListener? = null
    /** cancel押下時の挙動 デフォルトでは何もしない */
    var onCancelClickListener: DialogInterface.OnClickListener? = DialogInterface.OnClickListener { _, _ -> }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the Builder class for convenient dialog construction
        val builder = AlertDialog.Builder(activity)
        if (cancelText != "" && message != "") {
            //cancelボタンもmassageも使うときの処理
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(okText, onOkClickListener)
                    .setNegativeButton(cancelText, onCancelClickListener)
        } else if(cancelText != "" && message == "") {
            //cancelボタンを使うが、message を使わない時の処理
            builder.setTitle(title)
                    .setPositiveButton(okText, onOkClickListener)
                    .setNegativeButton(cancelText, onCancelClickListener)
        } else if (cancelText == "" && message != ""){
            //cancelボタンを使わないが、message を使う時の処理
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(okText, onOkClickListener)
        } else {
            //cancelボタンもmessage も使わない時の処理
            builder.setTitle(title)
                    .setPositiveButton(okText, onOkClickListener)
        }
        // Create the AlertDialog object and return it
        return builder.create()
    }

    override fun onPause() {
        super.onPause()
        // onPause でダイアログを閉じる場合
        dismiss()
    }
}