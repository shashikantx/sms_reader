package co.wisne.sms_reader

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** SmsReaderPlugin */

class SmsReaderPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel

  private lateinit var context: Context

  private final var SMS_PERMISSION_CODE: Int = 101;

  private companion object {
    const val COLUMN_ADDRESS = "address"
    const val COLUMN_BODY = "body"
    const val COLUMN_DATE = "date"
    const val COLUMN_TYPE = "type"
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    context = binding.activity
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sms_reader")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {

    if (call.method == "readInbox") {
      val page = call.argument<Int>("page") ?: 0
      val pageSize = call.argument<Int>("pageSize") ?: 10
      val searchQuery = call.argument<String>("searchQuery")
      var sortOrder = call.argument<String?>("sortOrder")
      val messages = readInbox(page, pageSize, searchQuery, sortOrder)
      result.success(messages)
    } else {
      result.notImplemented()
    }
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
        message[COLUMN_ADDRESS] = cursor.getString(cursor.getColumnIndex(COLUMN_ADDRESS))
        message[COLUMN_BODY] = cursor.getString(cursor.getColumnIndex(COLUMN_BODY))
        message[COLUMN_DATE] = cursor.getString(cursor.getColumnIndex(COLUMN_DATE))
        message[COLUMN_TYPE] = cursor.getString(cursor.getColumnIndex(COLUMN_TYPE))
        messages.add(message)
      }
      messages
    } ?: emptyList()
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onDetachedFromActivityForConfigChanges() {}

  override fun onDetachedFromActivity() {}

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}
}
