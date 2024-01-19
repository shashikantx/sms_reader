import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'sms_reader_platform_interface.dart';

/// An implementation of [SmsReaderPlatform] that uses method channels.
class MethodChannelSmsReader extends SmsReaderPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('sms_reader');

  @override
  Future<List<Map<Object?, Object?>>> readInbox(
      {int? page,
      int? pageSize,
      String? searchQuery,
      String? sortOrder}) async {
    final inboxSms = await methodChannel
        .invokeListMethod<Map<Object?, Object?>>('readInbox', {
      'pageSize': pageSize ?? 10,
      'page': page ?? 1,
      'searchQuery': searchQuery,
      'sortOrder': sortOrder ?? 'date ASC'
    });
    return inboxSms ?? [];
  }
}
