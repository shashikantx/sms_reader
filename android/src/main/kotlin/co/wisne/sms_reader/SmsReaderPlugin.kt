package co.wisne.sms_reader

import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog
import android.content.Intent


/** SmsReaderPlugin */

class SmsReaderPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, RequestPermissionsResultListener, ActivityResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel

  private lateinit var context: Context
  private var activityBinding: ActivityPluginBinding? = null
  private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null

  private final var SMS_READ_PERMISSION_CODE: Int = 101;

  private var onPermissionGranted: (() -> Unit)? = null
  private var onPermissionDenied: (() -> Unit)? = null

  private companion object {
    const val COLUMN_ID = "_id"
    const val COLUMN_THREAD_ID = "thread_id"
    const val COLUMN_ADDRESS = "address"
    const val COLUMN_PERSON = "person"
    const val COLUMN_BODY = "body"
    const val COLUMN_DATE = "date"
    const val COLUMN_TYPE = "type"
    const val COLUMN_READ = "read"
    const val COLUMN_STATUS = "status"
    const val COLUMN_SERVICE_CENTER = "service_center"
    const val COLUMN_PROTOCOL = "protocol"
    const val COLUMN_SUBJECT = "subject"
  }


  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    this.context = flutterPluginBinding.getApplicationContext();
    pluginBinding = flutterPluginBinding
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sms_reader")
    channel.setMethodCallHandler(this)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    Log.d("SMS_READER","onAttachedActivity");
    activityBinding = binding
    activityBinding?.addRequestPermissionsResultListener(this)
    activityBinding?.addActivityResultListener(this)
  }


  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "readInbox") {

     onPermissionGranted = {
       val page = call.argument<Int>("page") ?: 0
       val pageSize = call.argument<Int>("pageSize") ?: 10
       val searchQuery = call.argument<String>("searchQuery")
       val sortOrder = call.argument<String?>("sortOrder")
       val messages = readInbox(page, pageSize, searchQuery, sortOrder)
       result.success(messages)
     }

      onPermissionDenied = {
        result.error("PERMISSION_DENIED", "Permission denied", null)
      }

      handlePermission(activityBinding!!.getActivity(),
        android.Manifest.permission.READ_SMS,
        SMS_READ_PERMISSION_CODE,
        onPermissionGranted,
        onPermissionDenied)
    } else if (call.method == "canReadSms") {
      val canItReadSms = canReadSms()
      result.success(canItReadSms)
    }
    else {
      result.notImplemented()
    }
  }
  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding);
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    pluginBinding = null
    channel.setMethodCallHandler(null)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  override fun onDetachedFromActivity() {
    activityBinding?.removeRequestPermissionsResultListener(this)
    activityBinding?.removeActivityResultListener(this)
    activityBinding = null
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    return false;
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ): Boolean {
    Log.d("SMS_READER","onRequestPermissionsResult");
    when (requestCode) {
      SMS_READ_PERMISSION_CODE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && onPermissionGranted != null ) {
          Log.d("SMS_READER","onRequestPermissionsResult Permission Granted")
          // Permission granted, execute the provided function
          onPermissionGranted?.invoke()
          return true;
        } else if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED && onPermissionDenied != null) {
          Log.d("SMS_READER","onRequestPermissionsResult Permission denied")
          // Permission denied, execute the provided function
          onPermissionDenied?.invoke()
          return true;
        }
      }
      else -> {
        // Handle other permission requests if needed
      }
    }
    return false;
  }

  fun canReadSms() : Boolean {
    //check if permission is granted
    if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
      //if permission is granted, check if it can read sms
      val contentResolver: ContentResolver = context.contentResolver
      val cursorAll = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null)
      val total = cursorAll?.count ?: 0
      cursorAll?.close()
      return total > 0;
    }
    return false;
  }


  // content://sms/inbox
  // paginated query
  fun readInbox(page: Int, pageSize: Int, searchQuery: String? = null, sortOrder: String? = null): List<Map<String, Any?>> {
    val contentResolver: ContentResolver = context.contentResolver
    val cursorAll = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null)
    val total = cursorAll?.count ?: 0
    cursorAll?.close()

    if (total == 0) {
      return emptyList()
    }

    // Build the selection based on the searchQuery
    val selection: String? = if (!searchQuery.isNullOrBlank()) {
      "$COLUMN_ADDRESS LIKE ? OR $COLUMN_BODY LIKE ?"
    } else {
      null
    }

    // Arguments for the selection if searchQuery is provided
    val selectionArgs: Array<String>? = if (!searchQuery.isNullOrBlank()) {
      arrayOf("%$searchQuery%", "%$searchQuery%")
    } else {
      null
    }

    val sortOrderString = if (sortOrder.isNullOrBlank()) {
      "$COLUMN_DATE DESC"
    } else {
      sortOrder
    }

    val limit = "$pageSize"
    val offset = "${page * pageSize}"

    val cursor = contentResolver.query(
      Uri.parse("content://sms/inbox"),
      null,
      selection,
      selectionArgs,
      "$sortOrderString LIMIT $limit OFFSET $offset"
    )

    return cursor?.use { cursor ->
      val messages = mutableListOf<Map<String, Any?>>()
      while (cursor.moveToNext()) {
        val message = mutableMapOf<String, Any?>()
        message[COLUMN_ID] = cursor.getString(cursor.getColumnIndex(COLUMN_ID))
        message[COLUMN_THREAD_ID] = cursor.getString(cursor.getColumnIndex(COLUMN_THREAD_ID))
        message[COLUMN_ADDRESS] = cursor.getString(cursor.getColumnIndex(COLUMN_ADDRESS))
        message[COLUMN_BODY] = cursor.getString(cursor.getColumnIndex(COLUMN_BODY))
        message[COLUMN_DATE] = cursor.getString(cursor.getColumnIndex(COLUMN_DATE))
        message[COLUMN_TYPE] = cursor.getString(cursor.getColumnIndex(COLUMN_TYPE))
        message[COLUMN_READ] = cursor.getString(cursor.getColumnIndex(COLUMN_READ))
        message[COLUMN_STATUS] = cursor.getString(cursor.getColumnIndex(COLUMN_STATUS))
        message[COLUMN_PERSON] = cursor.getString(cursor.getColumnIndex(COLUMN_PERSON))
        message[COLUMN_SERVICE_CENTER] = cursor.getString(cursor.getColumnIndex(COLUMN_SERVICE_CENTER))
        message[COLUMN_PROTOCOL] = cursor.getString(cursor.getColumnIndex(COLUMN_PROTOCOL))
        message[COLUMN_SUBJECT] = cursor.getString(cursor.getColumnIndex(COLUMN_SUBJECT))

        messages.add(message)
      }
      messages
    } ?: emptyList()
  }

  fun askPermission() {
    Log.i("SMS_READER","Asking Permission");
    if (ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_SMS
      ) != PackageManager.PERMISSION_GRANTED
    ) {

      Log.w("SMS_READER","READ_SMS Permission not granted");
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          activityBinding!!.getActivity(),
          android.Manifest.permission.READ_SMS
        )
      ) {
        // Show an explanation to the user *asynchronously*
        Log.w("SMS_READER","Show Rationale");
        showPermissionExplainationDialog();
      } else {
        // No explanation needed, we can request the permission.
        Log.w("SMS_READER","Rationale not required requesting permission");
        ActivityCompat.requestPermissions(
          activityBinding!!.getActivity(),
          arrayOf(android.Manifest.permission.READ_SMS),
          SMS_READ_PERMISSION_CODE
        )
        Log.w("SMS_READER","Permission Requested");
      }
    }
  }

  fun showPermissionExplainationDialog() {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("Permission Required")
    builder.setMessage("We need permission to read SMS.")

    // Add "OK" button to dismiss the dialog
    builder.setPositiveButton("OK") { _, _ ->
      // Request the permission after the user acknowledges the explanation
      ActivityCompat.requestPermissions(
        context as android.app.Activity,
        arrayOf(android.Manifest.permission.READ_SMS),
        SMS_READ_PERMISSION_CODE
      )
    }

    // Add "Cancel" button to dismiss the dialog without requesting the permission
    builder.setNegativeButton("Cancel") { _, _ ->
      // Handle cancellation if needed
      // You might want to inform the user that the app functionality will be limited without the permission
    }

    val dialog = builder.create()
    dialog.show()
  }


  fun handlePermission(
    activity: android.app.Activity,
    permission: String,
    requestCode: Int,
    onPermissionGranted: (() -> Unit)?,
    onPermissionDenied: (() -> Unit)?
  ) {
    if (ContextCompat.checkSelfPermission(activity, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
      // Permission already granted, execute the provided function
      onPermissionGranted?.invoke()
    } else {
      Log.w("SMS_READER","READ_SMS Permission not granted");
      // Permission not granted, request the permission
      activity.requestPermissions(arrayOf(permission), requestCode)
      // You can handle the result of the request in onRequestPermissionsResult callback
      // If the user grants the permission, onPermissionGranted will be executed
      // If the user denies the permission, you can handle it in onPermissionDenied
    }
  }

}
