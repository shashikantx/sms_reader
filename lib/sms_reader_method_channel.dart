import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'sms_reader_platform_interface.dart';

/// An implementation of [SmsReaderPlatform] that uses method channels.
class MethodChannelSmsReader extends SmsReaderPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('sms_reader');

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<List<Map<Object?, Object?>>> getInboxSms(
      {int? page, int? pageSize, String? searchQuery}) async {
    final inboxSms = await methodChannel
        .invokeListMethod<Map<Object?, Object?>>('getInboxSms', {
      'pageSize': pageSize ?? 10,
      'page': page ?? 1,
      'searchQuery': searchQuery
    });
    return inboxSms ?? [];
  }
}
