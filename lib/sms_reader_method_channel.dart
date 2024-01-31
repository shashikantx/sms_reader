import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'sms_reader_platform_interface.dart';

/// An implementation of [SmsReaderPlatform] that uses method channels.
class MethodChannelSmsReader extends SmsReaderPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('sms_reader');

  ///
  /// [page] is the pagination page number, default is 1
  /// [pageSize] is the number of sms to be returned per page, default is 10
  /// [searchQuery] is the search query to be used to filter sms, default is null
  /// [sortOrder] is the order to be used to sort sms, default is null e.g. 'date DESC'
  /// ```dart
  /// final inboxSms = await SmsReader.readInbox(
  ///   page: 1,
  ///   pageSize: 10,
  ///   searchQuery: 'Flutter',
  ///   sortOrder: 'date DESC'
  /// );
  /// ```
  ///
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
      'sortOrder': sortOrder
    });
    return inboxSms ?? [];
  }

  @override
  Future<bool> canReadSms() async {
    final canReadSms = await methodChannel.invokeMethod<bool>('canReadSms');
    return canReadSms ?? false;
  }
}
